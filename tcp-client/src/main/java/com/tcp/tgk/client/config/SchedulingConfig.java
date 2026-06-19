package com.tcp.tgk.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;

@Configuration
public class SchedulingConfig implements SchedulingConfigurer {
    
    private static final Logger log = LoggerFactory.getLogger(SchedulingConfig.class);
    
    @PostConstruct
    public void init() {
        log.info("SchedulingConfig initialized - configuring scheduled task executor");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        log.info("Configuring scheduled tasks with thread pool of size 2");
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(2));
    }
}
