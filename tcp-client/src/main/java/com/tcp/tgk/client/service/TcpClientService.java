package com.tcp.tgk.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class TcpClientService {

    private static final Logger log = LoggerFactory.getLogger(TcpClientService.class);

    @Autowired
    private AbstractClientConnectionFactory connectionFactory;

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

    private Socket persistentSocket;
    private PrintWriter persistentOut;
    private BufferedReader persistentIn;
    private volatile boolean isConnected = false;
    private final AtomicReference<ConnectionStatus> connectionStatus = new AtomicReference<>(ConnectionStatus.UNKNOWN);

    @PostConstruct
    public void init() {
        log.info("TCP Client Service initialized");
        log.info("Connection Monitoring: {} (tick: {}ms)", monitoringEnabled ? "ENABLED" : "DISABLED", monitoringTickMs);
        log.info("Monitoring enabled flag value: {}", monitoringEnabled);
        log.info("@EnableScheduling should be active for monitoring to work");
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    public void testScheduling() {
        log.info("TEST: Scheduled task is working! Time: {} Thread: {}", System.currentTimeMillis(), Thread.currentThread().getName());
    }

    public ConnectionStatus getConnectionStatus() {
        return connectionStatus.get();
    }

    @Scheduled(fixedDelayString = "${tcp.client.monitoring.tick-ms:2000}", 
               initialDelayString = "${tcp.client.monitoring.tick-ms:2000}")
    public void monitorConnection() {
        log.info("MONITOR METHOD CALLED - enabled={}", monitoringEnabled);
        if (!monitoringEnabled) {
            log.info("MONITOR: Skipping because disabled");
            return;
        }

        // Skip monitoring if a user command is in progress
        synchronized (this) {
            try {
                log.info("Running connection health check (shallow probe)");
                String response = sendShallowProbe();
                
                if (response != null && response.contains("OK")) {
                    if (connectionStatus.get() != ConnectionStatus.CONNECTED) {
                        log.info("Connection status changed: {} -> CONNECTED", connectionStatus.get());
                        connectionStatus.set(ConnectionStatus.CONNECTED);
                    }
                } else {
                    handleConnectionError("Invalid probe response: " + response);
                }
            } catch (Exception e) {
                handleConnectionError(e.getMessage());
            }
        }
    }

    private void handleConnectionError(String errorMessage) {
        ConnectionStatus previousStatus = connectionStatus.get();
        log.warn("Connection health check failed: {}", errorMessage);
        
        // Set to ERROR (transient)
        connectionStatus.set(ConnectionStatus.ERROR);
        log.info("Connection status changed: {} -> ERROR", previousStatus);
        
        // Immediately trigger reconnection
        log.info("Triggering immediate reconnection...");
        connectionStatus.set(ConnectionStatus.CONNECTING);
        log.info("Connection status changed: ERROR -> CONNECTING");
        
        closePersistentConnection();
        isConnected = false;
        
        // Reconnect in background
        new Thread(() -> {
            try {
                connectToServer();
            } catch (Exception e) {
                log.error("Background reconnection failed: {}", e.getMessage());
            }
        }, "reconnection-thread").start();
    }

    private String sendShallowProbe() throws Exception {
        if (!isConnected || persistentSocket == null || persistentSocket.isClosed() || !persistentSocket.isConnected()) {
            throw new Exception("Not connected");
        }

        // Already synchronized by caller
        // Send with CRLF as server expects
        persistentOut.print("probe:shallow\r\n");
        persistentOut.flush();
        if (persistentOut.checkError()) {
            throw new Exception("Failed to send probe");
        }

        StringBuilder response = new StringBuilder();
        String line;
        long startTime = System.currentTimeMillis();
        
        while ((line = persistentIn.readLine()) != null) {
            // Timeout check
            if (System.currentTimeMillis() - startTime > 1000) {
                throw new Exception("Probe timeout");
            }
            
            if (response.length() > 0) {
                response.append("\n");
            }
            response.append(line);
            
            if (persistentIn.ready()) {
                continue;
            } else {
                break;
            }
        }
        
        return response.toString();
    }

    public void connectToServer() {
        connectionStatus.set(ConnectionStatus.CONNECTING);
        log.info("Connection status: CONNECTING");
        
        int attempt = 0;
        while (!isConnected && (attempt < maxRetryAttempts || maxRetryAttempts == 9999)) {
            attempt++;
            try {
                log.info("Attempting to connect to {}:{} (attempt {})", 
                    connectionFactory.getHost(), connectionFactory.getPort(), attempt);
                connectPersistent();
                isConnected = true;
                connectionStatus.set(ConnectionStatus.CONNECTED);
                log.info("Successfully connected to server");
                log.info("Connection status: CONNECTED");
                return;
            } catch (Exception e) {
                log.warn("Connection attempt {} failed: {}", attempt, e.getMessage());
                if (retryEnabled) {
                    try {
                        Thread.sleep(waitBetweenRetries);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        if (!isConnected) {
            connectionStatus.set(ConnectionStatus.ERROR);
            log.error("Failed to connect to server after {} attempts", attempt);
            log.info("Connection status: ERROR");
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
                isConnected = false;
                
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
        if (!isConnected || persistentSocket == null || persistentSocket.isClosed() || !persistentSocket.isConnected()) {
            log.info("Connection lost, reconnecting...");
            connectToServer();
        }
    }

    private String sendViaPersistentConnection(String command) throws Exception {
        synchronized (this) {
            // Send with CRLF as server expects
            persistentOut.print(command + "\r\n");
            persistentOut.flush();
            if (persistentOut.checkError()) {
                throw new Exception("Failed to send command - connection error");
            }
            
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = persistentIn.readLine()) != null) {
                if (response.length() > 0) {
                    response.append("\n");
                }
                response.append(line);
                
                if (persistentIn.ready()) {
                    continue;
                } else {
                    break;
                }
            }
            
            return response.toString();
        }
    }

    private void connectPersistent() throws Exception {
        String host = connectionFactory.getHost();
        int port = connectionFactory.getPort();
        
        persistentSocket = new Socket();
        persistentSocket.connect(new java.net.InetSocketAddress(host, port), connectionTimeout);
        persistentSocket.setSoTimeout(readTimeout);
        persistentSocket.setKeepAlive(true);
        persistentOut = new PrintWriter(persistentSocket.getOutputStream(), true);
        persistentIn = new BufferedReader(new InputStreamReader(persistentSocket.getInputStream()));
    }

    private void closePersistentConnection() {
        try {
            if (persistentIn != null) persistentIn.close();
            if (persistentOut != null) persistentOut.close();
            if (persistentSocket != null) persistentSocket.close();
        } catch (Exception e) {
            // Ignore
        } finally {
            persistentSocket = null;
            persistentOut = null;
            persistentIn = null;
            isConnected = false;
        }
    }

    private String sendViaNewConnection(String command) throws Exception {
        String host = connectionFactory.getHost();
        int port = connectionFactory.getPort();
        
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(host, port), connectionTimeout);
            socket.setSoTimeout(readTimeout);
            
            try (PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                
                // Send with CRLF as server expects
                out.print(command + "\r\n");
                out.flush();
                
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = in.readLine()) != null) {
                    if (response.length() > 0) {
                        response.append("\n");
                    }
                    response.append(line);
                    
                    if (in.ready()) {
                        continue;
                    } else {
                        break;
                    }
                }
                
                return response.toString();
            }
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down client connection");
        closePersistentConnection();
    }
}
