package com.tcp.tgk.client.ui;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.function.Consumer;

public class LogEventAppender extends AppenderBase<ILoggingEvent> {
    
    private static Consumer<String> logConsumer;

    public static void setLogConsumer(Consumer<String> consumer) {
        logConsumer = consumer;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (logConsumer != null) {
            logConsumer.accept(eventObject.getFormattedMessage());
        }
    }
}
