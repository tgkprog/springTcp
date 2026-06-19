package com.tcp.tgk.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class TcpClientService {

    private static final Logger log = LoggerFactory.getLogger(TcpClientService.class);

    @Autowired
    private AbstractClientConnectionFactory connectionFactory;
    
    @Autowired
    private ConnectionStateMachine stateMachine;

    @Value("${tcp.client.connection.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${tcp.client.connection.retry.max-attempts:9999}")
    private int maxRetryAttempts;

    @Value("${tcp.client.connection.retry.wait-between-retries:150}")
    private long waitBetweenRetries;

    @Value("${tcp.client.connection.read-timeout:5000}")
    private int readTimeout;

    @Value("${tcp.client.connection.persistent:true}")
    private boolean persistentConnection;

    @Value("${tcp.client.connection.timeout:550}")
    private int connectionTimeout;

    @Value("${tcp.client.monitoring.enabled:false}")
    private boolean monitoringEnabled;

    @Value("${tcp.client.monitoring.tick-ms:2000}")
    private long monitoringTickMs;

    private TcpConnection activeConnection;
    private final SynchronousQueue<String> responseQueue = new SynchronousQueue<>();
    private final AtomicBoolean isConnecting = new AtomicBoolean(false);
    private volatile boolean isShutdown = false;

    @PostConstruct
    public void init() {
        connectionFactory.registerListener(message -> {
            try {
                Object payload = message.getPayload();
                String response;
                if (payload instanceof byte[]) {
                    response = new String((byte[]) payload, StandardCharsets.UTF_8).trim();
                } else {
                    response = payload.toString().trim();
                }
                log.info("TcpListener received response: [{}]", response);
                responseQueue.offer(response);
            } catch (Exception e) {
                log.error("Error in TcpListener: {}", e.getMessage());
            }
            return false;
        });
        log.info("TCP Client Service initialized");
        log.info("Connection Monitoring: {} (tick: {}ms)", monitoringEnabled ? "ENABLED" : "DISABLED", monitoringTickMs);
        log.info("Using ConnectionStateMachine for state tracking");
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void testScheduling() {
        log.info("TEST: Scheduled task is working! Time: {} Thread: {}", System.currentTimeMillis(), Thread.currentThread().getName());
    }

    public ConnectionStateMachine.ConnectionState getConnectionStatus() {
        return stateMachine.getCurrentState();
    }

    @Scheduled(fixedDelayString = "${tcp.client.monitoring.tick-ms:2000}", 
               initialDelayString = "${tcp.client.monitoring.tick-ms:2000}")
    public void monitorConnection() {
        if (!monitoringEnabled) {
            return;
        }

        // Only monitor if we are currently CONNECTED. Skip during transient or error states.
        if (stateMachine.getCurrentState() != ConnectionStateMachine.ConnectionState.CONNECTED) {
            log.debug("MONITOR: Skipping health check because state is not CONNECTED (current: {})", stateMachine.getCurrentState());
            return;
        }

        // Skip monitoring if a user command is in progress
        synchronized (this) {
            try {
                log.info("Running connection health check (shallow probe)");
                String response = sendShallowProbe();
                log.info("Probe response: [{}]", response == null ? "NULL" : response);
                
                if (response != null && response.contains("OK")) {
                    log.info("Probe SUCCESS - response contains OK");
                } else {
                    log.warn("Probe FAILED - response does not contain OK");
                    handleConnectionError("Invalid probe response: " + response);
                }
            } catch (Exception e) {
                log.error("Probe EXCEPTION: {}", e.getMessage(), e);
                handleConnectionError(e.getMessage());
            }
        }
    }

    private void handleConnectionError(String errorMessage) {
        ConnectionStateMachine.ConnectionState previousStatus = stateMachine.getCurrentState();
        log.warn("Connection health check failed: {}", errorMessage);
        
        // Set to ERROR (transient)
        stateMachine.transitionTo(ConnectionStateMachine.ConnectionState.ERROR);
        log.info("Connection status changed: {} -> ERROR", previousStatus);
        
        closePersistentConnection();
        triggerReconnection("health check failed");
    }

    public void triggerReconnection(String reason) {
        if (isShutdown) {
            log.info("Shutdown in progress, ignoring reconnection request.");
            return;
        }
        
        // Trigger background reconnection only if not already connecting
        if (!isConnecting.get()) {
            log.info("Triggering immediate reconnection...");
            new Thread(() -> {
                try {
                    connectToServer();
                } catch (Exception e) {
                    log.error("Background reconnection failed: {}", e.getMessage());
                }
            }, "reconnection-thread").start();
        } else {
            log.info("Reconnection already in progress, skipping background thread spawning.");
        }
    }

    private String sendShallowProbe() throws Exception {
        if (!stateMachine.isConnected() || activeConnection == null || !activeConnection.isOpen()) {
            throw new Exception("Not connected");
        }

        // Already synchronized by caller
        activeConnection.send(MessageBuilder.withPayload("probe:shallow".getBytes(StandardCharsets.UTF_8)).build());

        String response = responseQueue.poll(1000, TimeUnit.MILLISECONDS);
        if (response == null) {
            throw new Exception("Probe timeout");
        }
        
        return response;
    }

    public void connectToServer() {
        if (stateMachine.isConnected()) {
            return;
        }
        if (!isConnecting.compareAndSet(false, true)) {
            log.info("Connection attempt already in progress, skipping concurrent connectToServer() request.");
            return;
        }
        
        try {
            stateMachine.transitionTo(ConnectionStateMachine.ConnectionState.CONNECTING);
            log.info("Connection status: CONNECTING");
            
            int attempt = 0;
            long currentWait = waitBetweenRetries;
            while (!stateMachine.isConnected() && (attempt < maxRetryAttempts || maxRetryAttempts == 9999)) {
                attempt++;
                try {
                    log.info("Attempting to connect to {}:{} (attempt {})", 
                        connectionFactory.getHost(), connectionFactory.getPort(), attempt);
                    connectPersistent();
                    
                    // Fallback manual transition if event listener didn't run synchronously (should run synchronously)
                    if (!stateMachine.isConnected()) {
                        stateMachine.transitionTo(ConnectionStateMachine.ConnectionState.CONNECTED);
                    }
                    
                    log.info("Successfully connected to server");
                    log.info("Connection status: CONNECTED");
                    return;
                } catch (Exception e) {
                    log.warn("Connection attempt {} failed: {}", attempt, e.getMessage());
                    if (retryEnabled) {
                        try {
                            log.info("Waiting {}ms before next connection attempt...", currentWait);
                            Thread.sleep(currentWait);
                            currentWait = Math.min(currentWait * 2, 5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }
            if (!stateMachine.isConnected()) {
                stateMachine.transitionTo(ConnectionStateMachine.ConnectionState.ERROR);
                log.error("Failed to connect to server after {} attempts", attempt);
                log.info("Connection status: ERROR");
            }
        } finally {
            isConnecting.set(false);
        }
    }

    public String sendCommand(String command) throws Exception {
        if (!persistentConnection) {
            return sendViaNewConnection(command);
        }

        int attempts = retryEnabled ? maxRetryAttempts : 1;
        Exception lastException = null;
        
        for (int i = 0; i < attempts; i++) {
            try {
                ensureConnected();
                return sendViaPersistentConnection(command);
            } catch (Exception e) {
                lastException = e;
                log.warn("Command failed (attempt {}): {}", i + 1, e.getMessage());
                closePersistentConnection();
                
                if (i < attempts - 1 && retryEnabled) {
                    log.info("Reconnecting in {}ms...", waitBetweenRetries);
                    Thread.sleep(waitBetweenRetries);
                    connectToServer();
                }
            }
        }
        
        throw new Exception("Failed after " + attempts + " attempts: " + lastException.getMessage(), lastException);
    }

    private void ensureConnected() throws Exception {
        if (stateMachine.getCurrentState() == ConnectionStateMachine.ConnectionState.INIT_IDLE) {
            throw new Exception("Not connected. Please connect first using the Reconnect button or press Enter in the Address field.");
        }
        if (!stateMachine.isConnected() || activeConnection == null || !activeConnection.isOpen()) {
            log.info("Connection lost, reconnecting...");
            connectToServer();
            
            // Wait for connection to be established (up to 10 seconds)
            int waitTime = 0;
            while (isConnecting.get() && !stateMachine.isConnected() && waitTime < 10000) {
                Thread.sleep(100);
                waitTime += 100;
            }
            
            if (!stateMachine.isConnected() || activeConnection == null || !activeConnection.isOpen()) {
                throw new Exception("Not connected");
            }
        }
    }

    private String sendViaPersistentConnection(String command) throws Exception {
        synchronized (this) {
            responseQueue.clear();
            activeConnection.send(MessageBuilder.withPayload(command.getBytes(StandardCharsets.UTF_8)).build());
            
            String response = responseQueue.poll(readTimeout, TimeUnit.MILLISECONDS);
            if (response == null) {
                throw new Exception("Response timeout");
            }
            
            return response;
        }
    }

    private void connectPersistent() throws Exception {
        if (!connectionFactory.isRunning()) {
            log.info("Starting connectionFactory manually...");
            connectionFactory.start();
        }
        activeConnection = connectionFactory.getConnection();
    }

    private void closePersistentConnection() {
        try {
            if (activeConnection != null) {
                activeConnection.close();
            }
        } catch (Exception e) {
            log.warn("Error closing connection: {}", e.getMessage());
        } finally {
            activeConnection = null;
        }
    }

    private String sendViaNewConnection(String command) throws Exception {
        if (!connectionFactory.isRunning()) {
            log.info("Starting connectionFactory manually...");
            connectionFactory.start();
        }
        TcpConnection connection = connectionFactory.getConnection();
        try {
            connection.send(MessageBuilder.withPayload(command.getBytes(StandardCharsets.UTF_8)).build());
            String response = responseQueue.poll(readTimeout, TimeUnit.MILLISECONDS);
            if (response == null) {
                throw new Exception("Response timeout");
            }
            return response;
        } finally {
            try {
                connection.close();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down client connection");
        isShutdown = true;
        closePersistentConnection();
    }

    public String getHost() {
        return connectionFactory.getHost();
    }

    public int getPort() {
        return connectionFactory.getPort();
    }

    public void reconfigureTarget(String host, int port) {
        log.info("Reconfiguring TCP client target to {}:{}", host, port);
        try {
            closePersistentConnection();
            connectionFactory.stop();
        } catch (Exception e) {
            log.warn("Error stopping connection factory: {}", e.getMessage());
        }
        connectionFactory.setHost(host);
        connectionFactory.setPort(port);
        triggerReconnection("Endpoint reconfigured");
    }
}
