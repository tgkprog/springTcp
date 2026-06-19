package com.tcp.tgk.client.service;

import com.tcp.tgk.client.service.ConnectionStateMachine.ConnectionState;

public class ConnectionStateChangeEvent {
    private final ConnectionState state;
    private final String error;

    public ConnectionStateChangeEvent(ConnectionState state, String error) {
        this.state = state;
        this.error = error;
    }

    public ConnectionState getState() {
        return state;
    }

    public String getError() {
        return error;
    }
}
