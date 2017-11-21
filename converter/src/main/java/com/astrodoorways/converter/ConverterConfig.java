package com.astrodoorways.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration()
@ComponentScan("com.astrodoorways.converter")
public class ConverterConfig {

	private final Logger logger = LoggerFactory.getLogger(ConverterConfig.class);

	private static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
	private static final int MAX_POOL_SIZE = (int) (POOL_SIZE * 1.5);
	private static final int QUEUE_CAPACITY = POOL_SIZE * 50;

	@Bean
	public ThreadPoolTaskExecutor metadataExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(POOL_SIZE);
		executor.setMaxPoolSize(MAX_POOL_SIZE);
		executor.setQueueCapacity(QUEUE_CAPACITY);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		logger.trace("metadata task executor built");
		return executor;
	}

	@Bean
	public ThreadPoolTaskExecutor converterExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(POOL_SIZE);
		executor.setMaxPoolSize(MAX_POOL_SIZE);
		executor.setQueueCapacity(QUEUE_CAPACITY);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		logger.trace("converter task executor built");
		return executor;
	}
}
