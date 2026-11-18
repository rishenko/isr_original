package com.astrodoorways.converter.unit;

import com.astrodoorways.converter.ApplicationProperties;
import org.junit.After;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** ApplicationProperties is a thin wrapper over JVM system properties. */
public class ApplicationPropertiesTest {

	private static final String KEY = "isr.test.property";

	@After
	public void clear() {
		System.clearProperty(KEY);
	}

	@Test
	public void missingPropertiesReturnNullOrFalse() {
		assertFalse(ApplicationProperties.hasProperty(KEY));
		assertNull(ApplicationProperties.getPropertyAsString(KEY));
		assertNull(ApplicationProperties.getPropertyAsStringList(KEY));
		assertNull(ApplicationProperties.getPropertyAsInteger(KEY));
		assertNull(ApplicationProperties.getPropertyAsLong(KEY));
		assertNull(ApplicationProperties.getPropertyAsDouble(KEY));
		assertFalse(ApplicationProperties.getPropertyAsBoolean(KEY));
	}

	@Test
	public void setPropertyWritesASystemProperty() {
		ApplicationProperties.setProperty(KEY, "value");
		assertTrue(ApplicationProperties.hasProperty(KEY));
		assertEquals("value", System.getProperty(KEY));
		assertEquals("value", ApplicationProperties.getPropertyAsString(KEY));
	}

	@Test
	public void typedGetters() {
		ApplicationProperties.setProperty(KEY, "42");
		assertEquals(Integer.valueOf(42), ApplicationProperties.getPropertyAsInteger(KEY));
		assertEquals(Long.valueOf(42), ApplicationProperties.getPropertyAsLong(KEY));
		assertEquals(Double.valueOf(42), ApplicationProperties.getPropertyAsDouble(KEY));

		ApplicationProperties.setProperty(KEY, "TRUE");
		assertTrue(ApplicationProperties.getPropertyAsBoolean(KEY));
		ApplicationProperties.setProperty(KEY, "yes");
		assertFalse("only 'true' (any case) is true", ApplicationProperties.getPropertyAsBoolean(KEY));
	}

	@Test
	public void stringListSplitsOnCommasWithoutTrimming() {
		ApplicationProperties.setProperty(KEY, "SATURN, TITAN,RHEA");
		assertEquals(Arrays.asList("SATURN", " TITAN", "RHEA"), ApplicationProperties.getPropertyAsStringList(KEY));
	}

	@Test(expected = NumberFormatException.class)
	public void nonNumericIntegerThrows() {
		ApplicationProperties.setProperty(KEY, "abc");
		ApplicationProperties.getPropertyAsInteger(KEY);
	}
}
