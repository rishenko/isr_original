package com.astrodoorways.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Command-line wrapper to run the Converter.
 * 
 * @author kmcabee
 * 
 */
@SpringBootApplication(scanBasePackages = "com.astrodoorways.converter")
@EnableJpaRepositories(basePackages = "com.astrodoorways")
@EntityScan(basePackages = "com.astrodoorways")
public class RunConverter {

	private static final Logger logger = LoggerFactory.getLogger(RunConverter.class);

	public static void main(String... args) {
		processProperties(args.length > 0 ? args[0] : "RunConverter.properties");
		SpringApplication.run(RunConverter.class, args);
	}

	/**
	 * @param args
	 *            process program properties based on the passed in arguments
	 */
	public static Properties processProperties(String... args) {
		// retrieve properties file
		if (args.length == 0) {
			logger.error("No properties file was specified.");
			throw new IllegalArgumentException(
					"syntax: java com.astrodoorways.cli.RunConverter <propertiesFile or defaults to RunConverter.properties>");
		}

		Properties prop = new Properties();
		File file = new File(args[0]);

		try {
			InputStream stream = new FileInputStream(file);
			prop.load(stream);
		} catch (IOException e) {
			logger.error("There was an error reading the properties file.", e);
			throw new RuntimeException(
					"There was an error reading the properties file. Syntax: java com.astrodoorways.cli.RunConverter <propertiesFile or defaults to RunConverter.properties>",
					e);
		}

		// parse properties file for values
		if (!prop.containsKey(ApplicationProperties.READ_DIRECTORY_PROPERTY)
				&& !prop.containsKey(ApplicationProperties.WRITE_DIRECTORY_PROPERTY)) {
			logger.debug("properties {}", prop);
			logger.error("The properties file must have both the {} and {} properties set.", new Object[] {
					ApplicationProperties.READ_DIRECTORY_PROPERTY, ApplicationProperties.WRITE_DIRECTORY_PROPERTY });
			throw new RuntimeException("The properties file must have both the "
					+ ApplicationProperties.READ_DIRECTORY_PROPERTY + " and "
					+ ApplicationProperties.WRITE_DIRECTORY_PROPERTY + " properties set.");
		}

		// Cassini calibration location
		if (prop.containsKey(ApplicationProperties.CASSINI_CALIBRATION_DIR)) {
			String cassiniCalibDir = prop.getProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR);
			File cassiniCalibDirRef = new File(cassiniCalibDir);
			if (!cassiniCalibDirRef.exists() || !cassiniCalibDirRef.isDirectory()) {
				throw new IllegalArgumentException("cassini.calibration.dir must point to a real directory");
			}
			ApplicationProperties.setProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR,
					prop.getProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR));
		}

		// default percent system utilization
		if (prop.containsKey(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION)) {
			ApplicationProperties.setProperty(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION,
					prop.getProperty(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION));
		} else {
			ApplicationProperties.setProperty(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION, "90");
		}

		// default num of processors
		if (prop.containsKey(ApplicationProperties.MAX_NUM_PROCESSORS)) {
			ApplicationProperties.setProperty(ApplicationProperties.MAX_NUM_PROCESSORS,
					prop.getProperty(ApplicationProperties.MAX_NUM_PROCESSORS));
		} else {
			ApplicationProperties.setProperty(ApplicationProperties.MAX_NUM_PROCESSORS, "2");
		}

		// Add all properties to the system properties
		for (Map.Entry<Object, Object> entry : prop.entrySet()) {
			String key = (String) entry.getKey();
			String val = (String) entry.getValue();
			ApplicationProperties.setProperty(key, val);
		}

		return prop;
	}
}
