package com.tcp.tgk.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class TestingThreadService {

    private static final Logger testLog = LoggerFactory.getLogger("client-tests");
    private static final Logger log = LoggerFactory.getLogger(TestingThreadService.class);

    @Autowired
    private TcpClientService clientService;

    @EventListener(ApplicationReadyEvent.class)
    public void startTestingThread() {
        Thread testingThread = new Thread(this::runTestingFlow, "testing");
        testingThread.setDaemon(false);
        testingThread.start();
        // log.info("Started separate 'testing' thread successfully.");
    }

    private void runTestingFlow() {
        Random random = new Random();

        // Stats accumulators (t3 - t2 per cycle, stored in RAM)
        List<Long> diff32Values = new ArrayList<>(50);

        try {
            // 1. Wait for client to connect ticking every 70ms
            // testLog.info("Testing thread waiting for manual client connection...");
            while (clientService.getConnectionStatus() != ConnectionStateMachine.ConnectionState.CONNECTED) {
                Thread.sleep(70);
            }

            // 2. Once connected, sleep for a random amount between 4 to 7 seconds
            long initialSleep = 4000 + random.nextInt(3001);
            // testLog.info("Connected. Sleeping for {} ms before starting reconnect test
            // loop...", initialSleep);
            Thread.sleep(initialSleep);

            // Re-connect-loop (50 times)
            for (int cycle = 1; cycle <= 50; cycle++) {
                // testLog.info("Cycle {}/50: Initiating disconnect...", cycle);

                // Run d1 in sync to break connection
                boolean success = runScript("/data/code/gt/tcp_tgk/ubuntuNetwork/sc2/d1");
                if (success) {
                    // Wait for a random amount of time between 5 and 7 seconds
                    long waitBeforeRestore = 5000 + random.nextInt(2001);
                    Thread.sleep(waitBeforeRestore);

                    // Note time t1 when it awakes
                    long t1 = System.currentTimeMillis();

                    // Run u1 in sync
                    runScript("/data/code/gt/tcp_tgk/ubuntuNetwork/sc2/u1");

                    // Note time t2 when u1 finishes — no logging
                    long t2 = System.currentTimeMillis();

                    // Keep checking when the client reconnects ticking every 70ms
                    while (clientService.getConnectionStatus() != ConnectionStateMachine.ConnectionState.CONNECTED) {
                        Thread.sleep(30);
                    }
                    long t3 = System.currentTimeMillis();

                    long diff31 = t3 - t1;
                    long diff32 = t3 - t2;

                    // Accumulate t3-t2 in RAM
                    diff32Values.add(diff32);

                    // Brief cycle result log
                    // testLog.info("Cycle {} | t3-t1={}ms t3-t2={}ms", cycle, diff31, diff32);
                } else {
                    testLog.error("Cycle {} failed: d1 script error.", cycle);
                }

                // Sleep for 10-15 seconds before repeating
                if (cycle < 50) {
                    long sleepBetweenCycles = 10000 + random.nextInt(5001);
                    // testLog.info("Sleeping {} ms before next cycle...", sleepBetweenCycles);
                    Thread.sleep(sleepBetweenCycles);
                }
            }

            // --- Print summary stats as WARN ---
            if (!diff32Values.isEmpty()) {
                List<Long> sorted = new ArrayList<>(diff32Values);
                sorted.sort(Long::compareTo);

                long sum = 0;
                for (long v : diff32Values)
                    sum += v;
                double avg = (double) sum / diff32Values.size();

                long lowest1 = sorted.get(0);
                long lowest2 = sorted.size() > 1 ? sorted.get(1) : lowest1;
                long highest1 = sorted.get(sorted.size() - 1);
                long highest2 = sorted.size() > 1 ? sorted.get(sorted.size() - 2) : highest1;

                testLog.warn("=== 50-Cycle t3-t2 Summary ===");
                testLog.warn("  avg={} ms", String.format("%.1f", avg));
                testLog.warn("  highest1={} ms  highest2={} ms", highest1, highest2);
                testLog.warn("  lowest1={}  ms  lowest2={}  ms", lowest1, lowest2);
                testLog.warn("==============================");
            }

            // testLog.info("Finished 50 cycles of reconnection tests. Exiting
            // application.");

            // Disconnect the client
            try {
                // log.info("Disconnecting client and exiting...");
                clientService.cleanup();
            } catch (Exception e) {
                log.warn("Error during client shutdown: {}", e.getMessage());
            }

            System.exit(0);

        } catch (InterruptedException e) {
            testLog.warn("Testing thread was interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            testLog.error("Unhandled exception in testing thread: {}", e.getMessage(), e);
        }
    }

    private boolean runScript(String scriptPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(scriptPath);
            Map<String, String> env = pb.environment();
            env.put("ip2", clientService.getHost());
            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            log.error("Failed to run script {}: {}", scriptPath, e.getMessage(), e);
            return false;
        }
    }
}
