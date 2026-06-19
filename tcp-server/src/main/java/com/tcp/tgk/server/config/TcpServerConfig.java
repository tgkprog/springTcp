package com.tcp.tgk.server.config;

import com.tcp.tgk.server.handler.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.messaging.MessageChannel;

@Configuration
public class TcpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(TcpServerConfig.class);

    @Value("${tcp.server.port:5039}")
    private int port;

    @Value("${tcp.server.connection.pool-size:10}")
    private int poolSize;

    @Value("${tcp.server.connection.backlog:100}")
    private int backlog;

    @Value("${tcp.server.socket.so-timeout:30000}")
    private int soTimeout;

    @Value("${tcp.server.socket.so-keep-alive:true}")
    private boolean soKeepAlive;

    @Value("${tcp.server.socket.tcp-no-delay:true}")
    private boolean tcpNoDelay;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("=================================================");
        log.info("TCP Server Configuration Loaded");
        log.info("=================================================");
        log.info("Port: {}", port);
        log.info("Connection Pool Size: {}", poolSize);
        log.info("Connection Backlog: {}", backlog);
        log.info("Socket Timeout: {} ms", soTimeout);
        log.info("Socket Keep-Alive: {}", soKeepAlive);
        log.info("TCP No-Delay: {}", tcpNoDelay);
        log.info("=================================================");
        log.info("TCP Server is ready and listening on port {}", port);
        log.info("=================================================");
    }

    @Bean
    public AbstractServerConnectionFactory serverConnectionFactory() {
        TcpNetServerConnectionFactory connectionFactory = new TcpNetServerConnectionFactory(port);
        connectionFactory.setSerializer(new ByteArrayCrLfSerializer());
        connectionFactory.setDeserializer(new ByteArrayCrLfSerializer());
        connectionFactory.setBacklog(backlog);
        connectionFactory.setSoTimeout(soTimeout);
        connectionFactory.setSoKeepAlive(soKeepAlive);
        connectionFactory.setSoTcpNoDelay(tcpNoDelay);
        return connectionFactory;
    }

    @Bean
    public TcpReceivingChannelAdapter tcpReceivingChannelAdapter() {
        TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
        adapter.setConnectionFactory(serverConnectionFactory());
        adapter.setOutputChannel(inboundChannel());
        return adapter;
    }

    @Bean
    public MessageChannel inboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageChannel outboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public org.springframework.integration.ip.tcp.TcpSendingMessageHandler tcpSendingMessageHandler() {
        org.springframework.integration.ip.tcp.TcpSendingMessageHandler handler = 
            new org.springframework.integration.ip.tcp.TcpSendingMessageHandler();
        handler.setConnectionFactory(serverConnectionFactory());
        return handler;
    }

    @Bean
    @ServiceActivator(inputChannel = "outboundChannel")
    public org.springframework.integration.ip.tcp.TcpSendingMessageHandler outboundAdapter() {
        return tcpSendingMessageHandler();
    }
}
