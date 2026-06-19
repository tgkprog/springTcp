package com.tcp.tgk.client;

import com.tcp.tgk.client.service.TcpClientService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Scanner;

@SpringBootApplication
@EnableScheduling
public class TcpClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(TcpClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(TcpClientService clientService) {
        return args -> {
            // Connect to server immediately on startup
            System.out.println("Connecting to server...");
            clientService.connectToServer();
            
            // Interactive CLI - this blocks and keeps app alive
            Scanner scanner = new Scanner(System.in);
            System.out.println("TCP Client Started. Type commands or 'exit' to quit.");
            System.out.println("Special commands:");
            System.out.println("  status       - Show connection status");
            System.out.println("Examples:");
            System.out.println("  probe:shallow");
            System.out.println("  probe:deep");
            System.out.println("  mgt:info");
            System.out.println("  mgt:time");
            System.out.println("  mgt:capabilities");
            System.out.println("  m: 5 + 10");
            System.out.println();

            while (scanner.hasNextLine()) {
                System.out.print("> ");
                System.out.flush();
                String command = scanner.nextLine().trim();
                
                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("Exiting...");
                    break;
                }
                
                if ("status".equalsIgnoreCase(command)) {
                    System.out.println("Connection Status: " + clientService.getConnectionStatus());
                    continue;
                }
                
                if (command.isEmpty()) {
                    continue;
                }

                try {
                    String response = clientService.sendCommand(command);
                    System.out.println(response);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
                System.out.println();
            }
        };
    }
}
