package com.tcp.tgk.client.logging;

import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import java.io.File;

public class RollOnStartupAndSizeTriggeringPolicy<E> extends SizeBasedTriggeringPolicy<E> {
    private boolean isFirst = true;

    @Override
    public boolean isTriggeringEvent(File activeFile, E event) {
        if (isFirst) {
            isFirst = false;
            // Roll over on startup if the log file exists and is not empty
            if (activeFile != null && activeFile.exists() && activeFile.length() > 0) {
                return true;
            }
        }
        return super.isTriggeringEvent(activeFile, event);
    }
}
