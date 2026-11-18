package com.astrodoorways.converter.unit;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.ConvertRunnable;
import com.astrodoorways.converter.metadata.handler.CassiniHandler;
import com.astrodoorways.converter.metadata.handler.MetadataHandler;
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
 * Output naming and date handling. Several tests pin known bugs on purpose (12-hour clock,
 * zero-based month folders); when those bugs are fixed, update the expectations in the same change.
 * Runs in UTC (set in the surefire configuration).
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

	/** Current behaviour (bug): 'hh' is a 12-hour clock, so 13:45 is written as 01. */
	@Test
	public void afternoonTimestampUsesTwelveHourClock() throws ParseException {
		assertEquals("20120327014559975", handlerWith(null, null).convertVicarDateToTimestamp("2012-087T13:45:59.975Z"));
	}

	/**
	 * Current behaviour (bug): a six-digit fraction is not truncated. The lenient parser reads
	 * 975123 as milliseconds (16 min 15.123 s), so 08:45:59.975123 becomes 09:02:14.123. The
	 * truncation fallback only runs on a ParseException, which never happens here.
	 */
	@Test
	public void microsecondTimesAreMisreadAsMilliseconds() throws ParseException {
		assertEquals("20120327090214123",
				handlerWith(null, null).convertVicarDateToTimestamp("2012-087T08:45:59.975123Z"));
	}

	/** Current behaviour (bug): Calendar.MONTH is zero-based, so March is folder 2. */
	@Test
	public void folderStructureUsesZeroBasedMonth() {
		assertEquals("2012/2/27", handlerWith("2012-087T08:45:59.975Z", "CL1-CL2").getFileStructure());
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

	/** ConvertRunnable has its own copy of the timestamp format, with the same 12-hour bug. */
	@Test
	public void convertRunnableTimestamp() throws ParseException {
		SimpleDateFormat utc = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		utc.setTimeZone(TimeZone.getTimeZone("UTC"));
		ConvertRunnable runnable = new ConvertRunnable();
		assertEquals("20120327014559975", runnable.convertVicarDateToTimestamp(utc.parse("2012-03-27 13:45:59.975")));
	}
}
