package com.astrodoorways.converter.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Command-line wrapper to run the Converter.
 * 
 * @author kmcabee
 * 
 */
@SpringBootApplication(scanBasePackages = "com.astrodoorways")
@EnableJpaRepositories(basePackages = "com.astrodoorways")
@EntityScan(basePackages = "com.astrodoorways")
public class RunConverter {
	public static void main(String... args) {
		SpringApplication.run(RunConverter.class, args);
	}

}
