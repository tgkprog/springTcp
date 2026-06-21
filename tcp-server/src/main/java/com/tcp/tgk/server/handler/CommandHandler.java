package com.tcp.tgk.server.handler;

import com.tcp.tgk.server.service.MathService;
import com.tcp.tgk.server.service.ProbeService;
import com.tcp.tgk.server.service.ManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.nio.charset.StandardCharsets;

@MessageEndpoint
public class CommandHandler {

    @Autowired
    private ProbeService probeService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private MathService mathService;

    @Transformer(inputChannel = "inboundChannel", outputChannel = "outboundChannel")
    public byte[] handleMessage(Message<byte[]> message) {
        byte[] payload = message.getPayload();
        String command = new String(payload, StandardCharsets.UTF_8).trim();
        
        String response = processCommand(command);
        
        return (response + "\r\n").getBytes(StandardCharsets.UTF_8);
    }

    private String processCommand(String command) {
        if (command.equalsIgnoreCase("fast")) {
            return probeService.handleProbe("shallow");
        } else if (command.equalsIgnoreCase("deep")) {
            return probeService.handleProbe("deep");
        } else if (command.equalsIgnoreCase("info")) {
            return managementService.handleManagement("info");
        } else if (command.equalsIgnoreCase("time")) {
            return managementService.handleManagement("time");
        } else if (command.equalsIgnoreCase("help")) {
            return managementService.handleManagement("capabilities");
        } else if (command.length() >= 2 && command.charAt(0) == 'm' && command.charAt(1) == ' ') {
            // "m <expr>": everything after the first space is the math expression
            String expression = command.substring(1).trim();
            return mathService.calculate(expression);
        } else {
            return "ERROR: Unknown command. Available: fast, deep, info, time, help, m <num> <op> <num>";
        }
    }
}
