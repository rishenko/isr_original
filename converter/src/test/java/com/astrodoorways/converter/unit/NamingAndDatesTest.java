package com.astrodoorways.converter.unit;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.ConvertRunnable;
import com.astrodoorways.converter.mission.cassini.CassiniHandler;
import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.mission.rosetta.RosettaHandler;
import com.astrodoorways.converter.mission.sdo.SDOHandler;
import com.astrodoorways.converter.metadata.handler.TimeFormats;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Output naming and date handling. Runs in UTC (set in the surefire configuration); the
 * formats are pinned to UTC, which {@link #timesDoNotDependOnTheDefaultTimeZone()} checks.
 */
public class NamingAndDatesTest {

	@Before
	public void requireUtc() {
		assertEquals("run tests with -Duser.timezone=UTC", "UTC", TimeZone.getDefault().getID());
	}

	@After
	public void clear() {
		System.clearProperty(ApplicationProperties.SEQUENCE);
	}

	private static CassiniHandler handlerWith(String time, String filter) {
		CassiniHandler handler = new CassiniHandler();
		Map<String, String> values = new HashMap<>();
		values.put(MetadataHandler.TIME, time);
		values.put(MetadataHandler.MISSION, "CASSINI-HUYGENS");
		values.put(MetadataHandler.TARGET, "TITAN");
		values.put(MetadataHandler.FILTER, filter);
		handler.setValueMap(values);
		return handler;
	}

	@Test
	public void morningTimestamp() throws ParseException {
		// 2012 day 87 = March 27
		assertEquals("20120327084559975", handlerWith(null, null).convertVicarDateToTimestamp("2012-087T08:45:59.975Z"));
	}

	/** Was a 12-hour clock ('hh'), which wrote 13:45 as 01. */
	@Test
	public void afternoonTimestampUsesTwentyFourHourClock() throws ParseException {
		assertEquals("20120327134559975", handlerWith(null, null).convertVicarDateToTimestamp("2012-087T13:45:59.975Z"));
	}

	/** Was read as 975,123 ms, which turned 08:45:59.975123 into 09:02:14.123. */
	@Test
	public void microsecondTimesAreTruncatedToMilliseconds() throws ParseException {
		assertEquals("20120327084559975",
				handlerWith(null, null).convertVicarDateToTimestamp("2012-087T08:45:59.975123Z"));
		assertEquals("2012/03/27", handlerWith("2012-087T23:59:59.999999Z", "CL1").getFileStructure());
	}

	/** Was zero-based and unpadded: March 27 went to folder "2012/2/27". */
	@Test
	public void folderStructureUsesPaddedCalendarMonth() {
		assertEquals("2012/03/27", handlerWith("2012-087T08:45:59.975Z", "CL1-CL2").getFileStructure());
		assertEquals("2012/12/31", handlerWith("2012-366T08:45:59.975Z", "CL1-CL2").getFileStructure());
		assertEquals("2013/01/01", handlerWith("2013-001T00:00:00.000Z", "CL1-CL2").getFileStructure());
	}

	@Test
	public void timesDoNotDependOnTheDefaultTimeZone() throws ParseException {
		TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
			CassiniHandler handler = handlerWith("2012-087T23:45:59.975Z", "CL1-CL2");
			assertEquals("2012/03/27", handler.getFileStructure());
			assertEquals("CASSINI-HUYGENS_TITAN_CL1-CL2_20120327234559975", handler.getFileName());
		} finally {
			TimeZone.setDefault(original);
		}
	}

	@Test
	public void normalizeFraction() {
		assertEquals("2012-087T08:45:59.975Z", TimeFormats.normalizeFraction("2012-087T08:45:59.975123Z"));
		assertEquals("2010-05-01T12:00:01.340Z", TimeFormats.normalizeFraction("2010-05-01T12:00:01.34Z"));
		assertEquals("2010-05-01T12:00:01.500", TimeFormats.normalizeFraction("2010-05-01T12:00:01.5"));
		assertEquals("2010-05-01T12:00:01Z", TimeFormats.normalizeFraction("2010-05-01T12:00:01Z"));
		assertEquals(null, TimeFormats.normalizeFraction(null));
	}

	/** SDO times have two fraction digits; ".34" was read as 34 ms instead of 340. */
	@Test
	public void sdoTwoDigitFraction() throws ParseException {
		SDOHandler handler = new SDOHandler();
		Map<String, String> values = new HashMap<>();
		values.put(MetadataHandler.TIME, "2010-05-01T12:00:01.34Z");
		values.put(MetadataHandler.MISSION, "SDO");
		values.put(MetadataHandler.TARGET, "SUN");
		values.put(MetadataHandler.FILTER, "171");
		handler.setValueMap(values);
		assertEquals("SDO_SUN_171_20100501120001340", handler.getFileName());
		values.put(MetadataHandler.MISSION, "SDO/JSOC-SDP");
		assertEquals("SDO-JSOC-SDP_SUN_171_20100501120001340", handler.getFileName());
		assertEquals("2010/05/01", handler.getFileStructure());
	}

	/** The pattern used "DD" (day of year) for the day of month, so every date landed in January. */
	@Test
	public void rosettaDate() throws ParseException {
		RosettaHandler handler = new RosettaHandler();
		Map<String, String> values = new HashMap<>();
		values.put(MetadataHandler.TIME, "2014-10-24T11:34:35.924");
		values.put(MetadataHandler.MISSION, "INTERNATIONAL-ROSETTA-MISSION");
		values.put(MetadataHandler.TARGET, "67P");
		values.put(MetadataHandler.FILTER, "F22");
		handler.setValueMap(values);
		assertEquals("2014/10/24", handler.getFileStructure());
		assertEquals("INTERNATIONAL-ROSETTA-MISSION_67P_F22_20141024113435924", handler.getFileName());
	}

	@Test
	public void unparseableTimeGivesEmptyFolder() {
		assertEquals("", handlerWith("not a date", "CL1-CL2").getFileStructure());
	}

	@Test
	public void fileNameWithoutSequence() throws ParseException {
		assertEquals("CASSINI-HUYGENS_TITAN_CL1-CL2_20120327084559975",
				handlerWith("2012-087T08:45:59.975Z", "CL1-CL2").getFileName());
	}

	@Test
	public void fileNameWithSequence() throws ParseException {
		ApplicationProperties.setProperty(ApplicationProperties.SEQUENCE, "NAMING_TEST_SEQ");
		CassiniHandler handler = handlerWith("2012-087T08:45:59.975Z", "NAMING_TEST_FILTER");
		assertTrue(handler.isSequencedFile());
		assertEquals("TITAN_NAMING_TEST_FILTER_NAMING_TEST_SEQ_0000000001", handler.getFileName());
		assertEquals("TITAN_NAMING_TEST_FILTER_NAMING_TEST_SEQ_0000000002", handler.getFileName());
	}

	@Test
	public void blankSequenceTextIsNotASequence() {
		ApplicationProperties.setProperty(ApplicationProperties.SEQUENCE, "  ");
		assertFalse(new CassiniHandler().isSequencedFile());
	}

	/** ConvertRunnable has its own copy of the timestamp format, which also used a 12-hour clock. */
	@Test
	public void convertRunnableTimestamp() throws ParseException {
		SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		utc.setTimeZone(TimeZone.getTimeZone("UTC"));
		ConvertRunnable runnable = new ConvertRunnable();
		assertEquals("20120327134559975", runnable.convertVicarDateToTimestamp(utc.parse("2012-03-27 13:45:59.975")));
	}
}
