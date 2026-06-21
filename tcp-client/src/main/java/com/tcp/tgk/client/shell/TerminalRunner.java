package com.tcp.tgk.client.shell;

import com.tcp.tgk.client.service.ConnectionStateMachine;
import com.tcp.tgk.client.service.TcpClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class TerminalRunner {

    @Autowired
    private TcpClientService clientService;

    @Autowired
    private ConnectionStateMachine stateMachine;

    @Value("${tcp.client.mode:both}")
    private String mode;

    public void startShell() {
        if ("ui".equalsIgnoreCase(mode)) {
            System.out.println("Terminal CLI shell disabled (mode is UI).");
            return;
        }

        // Run the terminal shell in a separate thread so it doesn't block the main
        // startup thread
        new Thread(() -> {
            try {
                // Wait slightly for system startup messages to settle
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            Scanner scanner = new Scanner(System.in);
            System.out.println("TCP Client Terminal Started. Type commands or 'exit' to quit.");
            System.out.println("Special commands:");
            System.out.println("  status       - Show connection status");
            System.out.println("  state        - Show detailed state machine info");
            System.out.println("Examples:");
            System.out.println("  fast");
            System.out.println("  deep");
            System.out.println("  info");
            System.out.println("  time");
            System.out.println("  help");
            System.out.println("  m 5 + 10");
            System.out.println();

            while (true) {
                System.out.print("> ");
                System.out.flush();
                if (!scanner.hasNextLine()) {
                    break;
                }
                String command = scanner.nextLine().trim();

                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("Exiting...");
                    System.exit(0);
                }

                if ("status".equalsIgnoreCase(command)) {
                    System.out.println("Connection Status: " + stateMachine.getCurrentState());
                    continue;
                }

                if ("state".equalsIgnoreCase(command)) {
                    System.out.println(stateMachine.getStatusInfo());
                    continue;
                }

                if (command.isEmpty()) {
                    continue;
                }

                if (!stateMachine.isConnected()) {
                    System.out.println("[connecting...]");
                    System.out.println();
                    continue;
                }

                try {
                    String response = clientService.sendCommand(command);
                    System.out.println(response);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    if (msg.toLowerCase().contains("not connected") || msg.toLowerCase().contains("connect")) {
                        System.out.println("[connecting...] " + msg);
                    } else {
                        System.err.println("Error: " + msg);
                    }
                }
                System.out.println();

            }
        }, "terminal-shell-thread").start();
    }
}
