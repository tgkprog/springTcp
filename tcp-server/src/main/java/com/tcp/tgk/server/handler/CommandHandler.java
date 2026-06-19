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
        if (command.startsWith("probe:")) {
            String type = command.substring(6).trim();
            return probeService.handleProbe(type);
        } else if (command.startsWith("mgt:")) {
            String type = command.substring(4).trim();
            return managementService.handleManagement(type);
        } else if (command.startsWith("m:")) {
            String expression = command.substring(2).trim();
            return mathService.calculate(expression);
        } else {
            return "ERROR: Unknown command. Use probe:, mgt:, or m:";
        }
    }
}
