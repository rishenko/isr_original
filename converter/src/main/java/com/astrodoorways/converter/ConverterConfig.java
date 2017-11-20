package com.astrodoorways.converter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@ComponentScan("com.astrodoorways.converter")
public class ConverterConfig {
	public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
		return executor;
	}

	@Bean
	public ThreadPoolTaskExecutor metadataExecutor() {
		return threadPoolTaskExecutor();
	}

	@Bean
	public ThreadPoolTaskExecutor converterExecutor() {
		return threadPoolTaskExecutor();
	}
}
