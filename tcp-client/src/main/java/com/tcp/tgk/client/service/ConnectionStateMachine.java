package com.tcp.tgk.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Connection state machine to track TCP connection status
 */
@Component
public class ConnectionStateMachine {
    
    private static final Logger log = LoggerFactory.getLogger(ConnectionStateMachine.class);

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    
    public enum ConnectionState {
        UNKNOWN("Initial state, not yet attempted"),
        CONNECTING("Attempting to establish connection"),
        CONNECTED("Successfully connected to server"),
        DISCONNECTED("Cleanly disconnected from server"),
        ERROR("Error occurred, connection lost"),
        RECONNECTING("Attempting to reconnect after failure");
        
        private final String description;
        
        ConnectionState(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final AtomicReference<ConnectionState> currentState = new AtomicReference<>(ConnectionState.UNKNOWN);
    private volatile long stateChangeTimestamp = System.currentTimeMillis();
    private volatile String lastErrorMessage = null;
    
    /**
     * Transition to a new state
     */
    public boolean transitionTo(ConnectionState newState) {
        return transitionTo(newState, null);
    }
    
    /**
     * Transition to a new state with optional error message
     */
    public boolean transitionTo(ConnectionState newState, String errorMessage) {
        ConnectionState oldState = currentState.get();
        
        if (oldState == newState) {
            if (errorMessage != null) {
                lastErrorMessage = errorMessage;
            }
            return true;
        }
        
        if (!isValidTransition(oldState, newState)) {
            log.warn("Invalid state transition: {} -> {}", oldState, newState);
            return false;
        }
        
        if (currentState.compareAndSet(oldState, newState)) {
            stateChangeTimestamp = System.currentTimeMillis();
            if (errorMessage != null) {
                lastErrorMessage = errorMessage;
            } else if (newState != ConnectionState.ERROR) {
                lastErrorMessage = null; // Clear error on successful state
            }
            
            log.info("State transition: {} -> {} {}", 
                oldState, newState, 
                errorMessage != null ? "(Error: " + errorMessage + ")" : "");
            
            if (eventPublisher != null) {
                eventPublisher.publishEvent(new ConnectionStateChangeEvent(newState, errorMessage));
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * Get current connection state
     */
    public ConnectionState getCurrentState() {
        return currentState.get();
    }
    
    /**
     * Get timestamp of last state change
     */
    public long getStateChangeTimestamp() {
        return stateChangeTimestamp;
    }
    
    /**
     * Get time elapsed since last state change (milliseconds)
     */
    public long getTimeInCurrentState() {
        return System.currentTimeMillis() - stateChangeTimestamp;
    }
    
    /**
     * Get last error message (if any)
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
    
    /**
     * Check if connection is usable
     */
    public boolean isConnected() {
        return currentState.get() == ConnectionState.CONNECTED;
    }
    
    /**
     * Check if in error state
     */
    public boolean isError() {
        ConnectionState state = currentState.get();
        return state == ConnectionState.ERROR || state == ConnectionState.DISCONNECTED;
    }
    
    /**
     * Validate if a state transition is allowed
     */
    private boolean isValidTransition(ConnectionState from, ConnectionState to) {
        if (from == to) {
            return true;
        }
        // Define allowed transitions
        switch (from) {
            case UNKNOWN:
                return to == ConnectionState.CONNECTING || to == ConnectionState.CONNECTED || to == ConnectionState.ERROR;
                
            case CONNECTING:
                return to == ConnectionState.CONNECTED || 
                       to == ConnectionState.ERROR ||
                       to == ConnectionState.DISCONNECTED ||
                       to == ConnectionState.RECONNECTING;
                
            case CONNECTED:
                return to == ConnectionState.DISCONNECTED || 
                       to == ConnectionState.ERROR ||
                       to == ConnectionState.RECONNECTING;
                
            case DISCONNECTED:
                return to == ConnectionState.CONNECTING || 
                       to == ConnectionState.RECONNECTING ||
                       to == ConnectionState.ERROR ||
                       to == ConnectionState.CONNECTED;
                
            case ERROR:
                return to == ConnectionState.RECONNECTING || 
                       to == ConnectionState.CONNECTING ||
                       to == ConnectionState.CONNECTED ||
                       to == ConnectionState.DISCONNECTED;
                
            case RECONNECTING:
                return to == ConnectionState.CONNECTED || 
                       to == ConnectionState.ERROR ||
                       to == ConnectionState.DISCONNECTED ||
                       to == ConnectionState.CONNECTING;
                
            default:
                return false;
        }
    }
    
    /**
     * Get detailed status information
     */
    public String getStatusInfo() {
        ConnectionState state = currentState.get();
        long timeInState = getTimeInCurrentState();
        
        StringBuilder info = new StringBuilder();
        info.append("State: ").append(state);
        info.append(" (").append(state.getDescription()).append(")");
        info.append(" | Time in state: ").append(timeInState).append("ms");
        
        if (lastErrorMessage != null && state == ConnectionState.ERROR) {
            info.append(" | Error: ").append(lastErrorMessage);
        }
        
        return info.toString();
    }
}
