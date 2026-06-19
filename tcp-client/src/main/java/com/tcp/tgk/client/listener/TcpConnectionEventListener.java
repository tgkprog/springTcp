package com.tcp.tgk.client.listener;

import com.tcp.tgk.client.service.ConnectionStateMachine;
import com.tcp.tgk.client.service.ConnectionStateMachine.ConnectionState;
import com.tcp.tgk.client.service.TcpClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.integration.ip.tcp.connection.TcpConnectionCloseEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionExceptionEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionFailedEvent;
import org.springframework.integration.ip.tcp.connection.TcpConnectionOpenEvent;
import org.springframework.stereotype.Component;

/**
 * Listener for Spring Integration TCP connection events
 */
@Component
public class TcpConnectionEventListener {
    
    private static final Logger log = LoggerFactory.getLogger(TcpConnectionEventListener.class);
    
    private final ConnectionStateMachine stateMachine;
    private final TcpClientService clientService;
    
    public TcpConnectionEventListener(ConnectionStateMachine stateMachine, @Lazy TcpClientService clientService) {
        this.stateMachine = stateMachine;
        this.clientService = clientService;
    }
    
    /**
     * Handle TCP connection open event
     */
    @EventListener
    public void handleConnectionOpen(TcpConnectionOpenEvent event) {
        log.info("═══════════════════════════════════════════");
        log.info("TCP CONNECTION OPENED");
        log.info("Connection ID: {}", event.getConnectionId());
        log.info("Connection: {}", event.getConnectionFactoryName());
        log.info("═══════════════════════════════════════════");
        
        stateMachine.transitionTo(ConnectionState.CONNECTED);
    }
    
    /**
     * Handle TCP connection close event
     */
    @EventListener
    public void handleConnectionClose(TcpConnectionCloseEvent event) {
        log.warn("═══════════════════════════════════════════");
        log.warn("TCP CONNECTION CLOSED");
        log.warn("Connection ID: {}", event.getConnectionId());
        log.warn("Connection: {}", event.getConnectionFactoryName());
        log.warn("═══════════════════════════════════════════");
        log.warn("Connection health check failed (event): Connection closed");
        
        stateMachine.transitionTo(ConnectionState.DISCONNECTED, "Connection closed");
        clientService.triggerReconnection("Connection closed event");
    }
    
    /**
     * Handle TCP connection exception event
     */
    @EventListener
    public void handleConnectionException(TcpConnectionExceptionEvent event) {
        log.error("═══════════════════════════════════════════");
        log.error("TCP CONNECTION EXCEPTION");
        log.error("Connection ID: {}", event.getConnectionId());
        log.error("Connection: {}", event.getConnectionFactoryName());
        log.error("Exception: {}", event.getCause().getMessage());
        log.error("═══════════════════════════════════════════");
        log.warn("Connection health check failed (event): {}", event.getCause().getMessage());
        
        stateMachine.transitionTo(ConnectionState.ERROR, event.getCause().getMessage());
        clientService.triggerReconnection("Connection exception: " + event.getCause().getMessage());
    }

    /**
     * Handle TCP connection failed event
     */
    @EventListener
    public void handleConnectionFailed(TcpConnectionFailedEvent event) {
        log.error("═══════════════════════════════════════════");
        log.error("TCP CONNECTION FAILED");
        log.error("Cause: {}", event.getCause() != null ? event.getCause().getMessage() : "Unknown connection failure");
        log.error("═══════════════════════════════════════════");
        
        stateMachine.transitionTo(ConnectionState.ERROR, event.getCause() != null ? event.getCause().getMessage() : "Connection failed");
    }
    
    /**
     * Handle any TCP connection event (catch-all)
     */
    @EventListener
    public void handleAnyConnectionEvent(TcpConnectionEvent event) {
        // Log all TCP events for debugging
        log.debug("TCP Event: {} - Connection: {} - ID: {}", 
            event.getClass().getSimpleName(),
            event.getConnectionFactoryName(),
            event.getConnectionId());
    }
}
