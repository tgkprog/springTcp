package com.tcp.tgk.server.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Service
public class ManagementService {

    private static final Logger log = LoggerFactory.getLogger(ManagementService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<String> cachedIps = new ArrayList<>();

    @PostConstruct
    public void init() {
        log.info("=== LOCAL IP ADDRESSES AT STARTUP ===");
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    String ip = addr.getHostAddress();
                    int percentIdx = ip.indexOf('%');
                    if (percentIdx > 0) {
                        ip = ip.substring(0, percentIdx);
                    }
                    cachedIps.add(ip);
                    log.info("Interface: {} -> IP: {}", iface.getName(), ip);
                }
            }
        } catch (Exception e) {
            log.error("Failed to retrieve network interfaces at startup", e);
        }
        log.info("======================================");
    }

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
            .append(System.getProperty("os.version")).append("\n");
        info.append("IP Addresses:\n");
        if (cachedIps.isEmpty()) {
            info.append("  - (none found)\n");
        } else {
            for (String ip : cachedIps) {
                info.append("  - ").append(ip).append("\n");
            }
        }
        return info.toString().trim();
    }

    private String handleTime() {
        LocalDateTime now = LocalDateTime.now();
        return "SERVER_TIME: " + now.format(DATE_FORMATTER);
    }

    private String handleCapabilities() {
        StringBuilder capabilities = new StringBuilder("CAPABILITIES:\n");
        capabilities.append("1. Probe Commands:\n");
        capabilities.append("   - fast - Basic server health check\n");
        capabilities.append("   - deep - Detailed system information\n");
        capabilities.append("2. Management Commands:\n");
        capabilities.append("   - info - Server information\n");
        capabilities.append("   - time - Current server time\n");
        capabilities.append("   - help - List all capabilities\n");
        capabilities.append("3. Math Server:\n");
        capabilities.append("   - m <expression> - Calculate arithmetic expressions\n");
        capabilities.append("   - Supported operations: +, -, *, /, %, ^ (power)");
        return capabilities.toString();
    }
}
