package com.tcp.tgk.client.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.messaging.MessageChannel;

@Configuration
public class TcpClientConfig {

    private static final Logger log = LoggerFactory.getLogger(TcpClientConfig.class);

    @Value("${tcp.client.host:localhost}")
    private String host;

    @Value("${tcp.client.port:5039}")
    private int port;

    @Value("${tcp.client.connection.persistent:true}")
    private boolean persistentConnection;

    @Value("${tcp.client.connection.timeout:10000}")
    private int connectionTimeout;

    @Value("${tcp.client.connection.read-timeout:5000}")
    private int readTimeout;

    @Value("${tcp.client.socket.so-timeout:5000}")
    private int soTimeout;

    @Value("${tcp.client.socket.so-keep-alive:true}")
    private boolean soKeepAlive;

    @Value("${tcp.client.socket.tcp-no-delay:true}")
    private boolean tcpNoDelay;

    @Value("${tcp.client.connection.retry.enabled:true}")
    private boolean retryEnabled;

    @Value("${tcp.client.connection.retry.max-attempts:9999}")
    private int maxRetryAttempts;

    @Value("${tcp.client.connection.retry.wait-between-retries:150}")
    private long waitBetweenRetries;

    @Value("${tcp.client.monitoring.enabled:false}")
    private boolean monitoringEnabled;

    @Value("${tcp.client.monitoring.tick-ms:2000}")
    private long monitoringTickMs;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=================================================");
        log.info("TCP Client Configuration Loaded");
        log.info("=================================================");
        log.info("Host: {}", host);
        log.info("Port: {}", port);
        log.info("Persistent Connection: {}", persistentConnection);
        log.info("Connection Timeout: {} ms", connectionTimeout);
        log.info("Read Timeout: {} ms", readTimeout);
        log.info("Socket Timeout: {} ms", soTimeout);
        log.info("Socket Keep-Alive: {}", soKeepAlive);
        log.info("TCP No-Delay: {}", tcpNoDelay);
        log.info("Retry Enabled: {}", retryEnabled);
        log.info("Max Retry Attempts: {}", maxRetryAttempts);
        log.info("Wait Between Retries: {} ms", waitBetweenRetries);
        log.info("Connection Monitoring: {}", monitoringEnabled ? "ENABLED" : "DISABLED");
        if (monitoringEnabled) {
            log.info("Monitoring Tick Interval: {} ms", monitoringTickMs);
        }
        log.info("=================================================");
    }

    @Bean
    public AbstractClientConnectionFactory clientConnectionFactory() {
        TcpNetClientConnectionFactory connectionFactory = new TcpNetClientConnectionFactory(host, port);
        connectionFactory.setSerializer(new ByteArrayCrLfSerializer());
        connectionFactory.setDeserializer(new ByteArrayCrLfSerializer());
        connectionFactory.setSingleUse(false);
        connectionFactory.setSoTimeout(soTimeout);
        connectionFactory.setSoKeepAlive(soKeepAlive);
        connectionFactory.setSoTcpNoDelay(tcpNoDelay);
        connectionFactory.setConnectTimeout(connectionTimeout);
        return connectionFactory;
    }

    @Bean
    public MessageChannel outboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public TcpSendingMessageHandler tcpSendingMessageHandler() {
        TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
        handler.setConnectionFactory(clientConnectionFactory());
        return handler;
    }
}
