package com.astrodoorways.converter.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.Converter;
import com.astrodoorways.converter.ConverterConfig;
import com.astrodoorways.db.PersistenceConfig;

/**
 * Command-line wrapper to run the Converter.
 * 
 * @author kmcabee
 * 
 */
public class RunConverter {

	private static final Logger logger = LoggerFactory.getLogger(RunConverter.class);

	public static void main(String... args) {
		// process the properties file
		Properties properties = null;
		if (args != null && args.length != 0)
			properties = processProperties(args);
		else
			properties = processProperties("RunConverter.properties");

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ConverterConfig.class,
				PersistenceConfig.class);
		Converter converter = (Converter) context.getBean("converter");

		converter.setReadDirectory(properties.getProperty(ApplicationProperties.READ_DIRECTORY_PROPERTY));
		converter.setWriteDirectory(properties.getProperty(ApplicationProperties.WRITE_DIRECTORY_PROPERTY));
		try {
			converter.beginConversion();
		} catch (Exception e) {
			logger.debug(e.getMessage());
			for (StackTraceElement st : e.getStackTrace()) {
				logger.debug(st.getClassName() + ":" + st.getLineNumber());
			}
		}
		logger.debug("The converter has finished. If the application is still running, feel free to kill it.");

		context.close();
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
		for (Entry<Object, Object> entry : prop.entrySet()) {
			String key = (String) entry.getKey();
			String val = (String) entry.getValue();
			ApplicationProperties.setProperty(key, val);
		}

		return prop;
	}
}
