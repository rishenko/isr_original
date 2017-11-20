package com.astrodoorways.converter;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public interface Converter {
    void beginConversion() throws Exception;

    void executorThrottleBasic(ThreadPoolTaskExecutor executor) throws InterruptedException;

    void executorThrottle(ThreadPoolTaskExecutor executor, int count, int sleepSeconds)
            throws InterruptedException;

    void setReadDirectory(String readDirectory);

    void setWriteDirectory(String writeDirectory);
}
