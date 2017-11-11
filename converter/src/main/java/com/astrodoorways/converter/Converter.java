package com.astrodoorways.converter;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public interface Converter {
    void beginConversion() throws Exception;

    void executorThrottleBasic(ThreadPoolTaskExecutor executor) throws InterruptedException;

    void executorThrottle(ThreadPoolTaskExecutor executor, int count, int sleepSeconds)
            throws InterruptedException;

    void sleepTask() throws InterruptedException;

    String getReadDirectory();

    void setReadDirectory(String readDirectory);

    String getWriteDirectory();

    void setWriteDirectory(String writeDirectory);
}
