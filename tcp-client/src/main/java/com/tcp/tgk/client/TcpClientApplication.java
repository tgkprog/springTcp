package com.tcp.tgk.client;

import com.tcp.tgk.client.service.TcpClientService;
import com.tcp.tgk.client.shell.TerminalRunner;
import com.tcp.tgk.client.ui.SwingUiFrame;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TcpClientApplication {

    public static void main(String[] args) {
        // Read mode from env var or system property (default to both)
        String mode = System.getenv("TCP_CLIENT_MODE");
        if (mode == null) {
            mode = System.getProperty("tcp.client.mode", "both");
        }
        
        boolean headless = "terminal".equalsIgnoreCase(mode);
        
        new SpringApplicationBuilder(TcpClientApplication.class)
            .headless(headless)
            .run(args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(
            TcpClientService clientService,
            TerminalRunner terminalRunner,
            SwingUiFrame swingUiFrame) {
        return args -> {
            System.out.println("Establishing initial connection...");
            clientService.connectToServer();
            
            // Launch Swing UI frame if enabled
            swingUiFrame.showUi();
            
            // Launch Terminal CLI console shell if enabled
            terminalRunner.startShell();
        };
    }
}
