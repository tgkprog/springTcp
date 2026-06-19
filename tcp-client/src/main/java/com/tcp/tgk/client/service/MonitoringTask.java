package com.tcp.tgk.client.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitoringTask {
    
    private static final Logger log = LoggerFactory.getLogger(MonitoringTask.class);

    @Scheduled(fixedDelay = 3000, initialDelay = 3000)
    public void testTask() {
        log.info("★★★ TEST TASK RUNNING! Thread: {}", Thread.currentThread().getName());
    }
}
