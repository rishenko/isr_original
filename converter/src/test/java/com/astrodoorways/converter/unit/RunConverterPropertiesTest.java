package com.astrodoorways.converter.unit;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.RunConverter;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** RunConverter.processProperties: loading and validating the properties file. */
public class RunConverterPropertiesTest {

	private static final String[] KEYS = { ApplicationProperties.READ_DIRECTORY_PROPERTY,
			ApplicationProperties.WRITE_DIRECTORY_PROPERTY, ApplicationProperties.CASSINI_CALIBRATION_DIR,
			ApplicationProperties.SYSTEM_PERCENT_UTILIZATION, ApplicationProperties.MAX_NUM_PROCESSORS,
			"custom.key" };

	@After
	public void clear() {
		for (String key : KEYS) {
			System.clearProperty(key);
		}
	}

	private static String write(String... lines) throws IOException {
		File file = File.createTempFile("isr", ".properties");
		file.deleteOnExit();
		Files.write(file.toPath(), Arrays.asList(lines), StandardCharsets.UTF_8);
		return file.getAbsolutePath();
	}

	@Test
	public void loadsPropertiesAndAppliesDefaults() throws IOException {
		Properties properties = RunConverter.processProperties(
				write("image.directory.read=/in", "image.directory.write=/out", "custom.key=custom"));

		assertEquals("/in", properties.getProperty(ApplicationProperties.READ_DIRECTORY_PROPERTY));
		assertEquals("/in", System.getProperty(ApplicationProperties.READ_DIRECTORY_PROPERTY));
		assertEquals("/out", System.getProperty(ApplicationProperties.WRITE_DIRECTORY_PROPERTY));
		assertEquals("every property is copied to system properties", "custom", System.getProperty("custom.key"));
		assertEquals("90", System.getProperty(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION));
		assertEquals("2", System.getProperty(ApplicationProperties.MAX_NUM_PROCESSORS));
	}

	@Test
	public void explicitValuesOverrideDefaults() throws IOException {
		RunConverter.processProperties(write("image.directory.read=/in", "image.directory.write=/out",
				"system.percent.utilization=50", "max.num.processors=8"));
		assertEquals("50", System.getProperty(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION));
		assertEquals("8", System.getProperty(ApplicationProperties.MAX_NUM_PROCESSORS));
	}

	@Test
	public void missingBothDirectoriesIsRejected() throws IOException {
		try {
			RunConverter.processProperties(write("custom.key=x"));
			fail("expected a RuntimeException");
		} catch (RuntimeException e) {
			assertTrue(e.getMessage().contains("must have both"));
		}
	}

	/**
	 * Current behaviour (bug): the check uses && instead of ||, so a file with only one of the two
	 * directories is accepted. Fixing this should flip this test.
	 */
	@Test
	public void onlyOneDirectoryIsCurrentlyAccepted() throws IOException {
		RunConverter.processProperties(write("image.directory.read=/in"));
		assertEquals("/in", System.getProperty(ApplicationProperties.READ_DIRECTORY_PROPERTY));
	}

	@Test(expected = IllegalArgumentException.class)
	public void calibrationDirectoryMustExist() throws IOException {
		RunConverter.processProperties(write("image.directory.read=/in", "image.directory.write=/out",
				"cassini.calibration.dir=/definitely/not/a/real/dir"));
	}

	@Test
	public void existingCalibrationDirectoryIsAccepted() throws IOException {
		String dir = Files.createTempDirectory("isr-calib").toString();
		RunConverter.processProperties(write("image.directory.read=/in", "image.directory.write=/out",
				"cassini.calibration.dir=" + dir));
		assertEquals(dir, System.getProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR));
	}

	@Test(expected = RuntimeException.class)
	public void missingFileIsRejected() {
		RunConverter.processProperties("/definitely/not/a/real/file.properties");
	}

	@Test(expected = IllegalArgumentException.class)
	public void noArgumentsIsRejected() {
		RunConverter.processProperties();
	}
}
