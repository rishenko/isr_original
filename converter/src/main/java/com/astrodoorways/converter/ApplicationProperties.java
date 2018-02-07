package com.astrodoorways.converter;

import java.util.Arrays;
import java.util.List;

public final class ApplicationProperties {

	public static final String SYSTEM_PERCENT_UTILIZATION = "system.percent.utilization";
	public static final String MAX_NUM_PROCESSORS = "max.num.processors";
	public static final String SEQUENCE = "image.name.sequence";
	public static final String PREPROCESSED_PATH = "preprocessed.file.dir";
	public static final String CASSINI_CALIBRATION_DIR = "cassini.calibration.dir";

	public static final String READ_DIRECTORY_PROPERTY = "image.directory.read";
	public static final String WRITE_DIRECTORY_PROPERTY = "image.directory.write";

	public static final String FUNPACK_PATH = "funpack.path";
	public static final String FILTER_LIST = "filter.process.list";
	public static final String TARGET_LIST = "target.process.list";

	public static void setProperty(String property, String value) {
		System.setProperty(property, value);
	}

	public static boolean hasProperty(String property) {
		return System.getProperties().containsKey(property);
	}

	public static String getPropertyAsString(String property) {
		if (System.getProperties().containsKey(property)) {
			return System.getProperties().getProperty(property);
		}

		return null;
	}

	public static List<String> getPropertyAsStringList(String property) {
		String propertyStr = getPropertyAsString(property);
		if (propertyStr != null)
			return Arrays.asList(propertyStr.split(","));

		return null;
	}

	public static boolean getPropertyAsBoolean(String property) {
		boolean result = false;
		if (System.getProperties().containsKey(property)) {
			result = Boolean.parseBoolean(System.getProperties().getProperty(property));
		}
		return result;
	}

	public static Integer getPropertyAsInteger(String property) {
		if (System.getProperties().containsKey(property)) {
			return Integer.parseInt(System.getProperties().getProperty(property));
		}

		return null;
	}

	public static Long getPropertyAsLong(String property) {
		if (System.getProperties().containsKey(property)) {
			return Long.parseLong(System.getProperties().getProperty(property));
		}

		return null;
	}

	public static Double getPropertyAsDouble(String property) {
		if (System.getProperties().containsKey(property)) {
			return Double.parseDouble(System.getProperties().getProperty(property));
		}

		return null;
	}
}
