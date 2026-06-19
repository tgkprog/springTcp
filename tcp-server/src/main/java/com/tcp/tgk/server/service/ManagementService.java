package com.tcp.tgk.server.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class ManagementService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String handleManagement(String type) {
        switch (type.toLowerCase()) {
            case "info":
                return handleInfo();
            case "time":
                return handleTime();
            case "capabilities":
                return handleCapabilities();
            default:
                return "ERROR: Unknown management command. Use 'info', 'time', or 'capabilities'";
        }
    }

    private String handleInfo() {
        StringBuilder info = new StringBuilder("SERVER_INFO:\n");
        info.append("Name: TCP TGK Server\n");
        info.append("Version: 1.0.0\n");
        info.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        info.append("OS: ").append(System.getProperty("os.name")).append(" ")
            .append(System.getProperty("os.version"));
        return info.toString();
    }

    private String handleTime() {
        LocalDateTime now = LocalDateTime.now();
        return "SERVER_TIME: " + now.format(DATE_FORMATTER);
    }

    private String handleCapabilities() {
        StringBuilder capabilities = new StringBuilder("CAPABILITIES:\n");
        capabilities.append("1. Probe Commands:\n");
        capabilities.append("   - probe:shallow - Basic server health check\n");
        capabilities.append("   - probe:deep - Detailed system information\n");
        capabilities.append("2. Management Commands:\n");
        capabilities.append("   - mgt:info - Server information\n");
        capabilities.append("   - mgt:time - Current server time\n");
        capabilities.append("   - mgt:capabilities - List all capabilities\n");
        capabilities.append("3. Math Server:\n");
        capabilities.append("   - m: <expression> - Calculate arithmetic expressions\n");
        capabilities.append("   - Supported operations: +, -, *, /, %, ^ (power)");
        return capabilities.toString();
    }
}
