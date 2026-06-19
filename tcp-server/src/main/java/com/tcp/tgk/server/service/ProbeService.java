package com.tcp.tgk.server.service;

import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Service
public class ProbeService {

    public String handleProbe(String type) {
        switch (type.toLowerCase()) {
            case "shallow":
                return handleShallowProbe();
            case "deep":
                return handleDeepProbe();
            default:
                return "ERROR: Unknown probe type. Use 'shallow' or 'deep'";
        }
    }

    private String handleShallowProbe() {
        return "OK: Server is running";
    }

    private String handleDeepProbe() {
        StringBuilder result = new StringBuilder("DEEP_PROBE:\n");
        
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            result.append("Hostname: ").append(localHost.getHostName()).append("\n");
            result.append("IP: ").append(localHost.getHostAddress()).append("\n");
        } catch (UnknownHostException e) {
            result.append("Host info unavailable\n");
        }
        
        Runtime runtime = Runtime.getRuntime();
        result.append("Available Processors: ").append(runtime.availableProcessors()).append("\n");
        result.append("Free Memory: ").append(runtime.freeMemory() / 1024 / 1024).append(" MB\n");
        result.append("Total Memory: ").append(runtime.totalMemory() / 1024 / 1024).append(" MB\n");
        result.append("Max Memory: ").append(runtime.maxMemory() / 1024 / 1024).append(" MB");
        
        return result.toString();
    }
}
