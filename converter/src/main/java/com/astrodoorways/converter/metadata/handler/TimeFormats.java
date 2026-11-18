package com.astrodoorways.converter.metadata.handler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared helpers for the date formats used in labels, output names and EXIF tags.
 *
 * Every mission time in the labels is UTC, so all formats are pinned to UTC instead of the
 * JVM's default time zone (which made output names depend on the machine running the job).
 */
public final class TimeFormats {

	public static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	/** Fractional seconds directly after the seconds field, e.g. ".975123" in "08:45:59.975123Z". */
	private static final Pattern FRACTION = Pattern.compile("(\\d{2}:\\d{2}:\\d{2})\\.(\\d+)");

	private TimeFormats() {
	}

	/** A SimpleDateFormat for the given pattern that formats and parses in UTC. */
	public static SimpleDateFormat utc(String pattern) {
		SimpleDateFormat format = new SimpleDateFormat(pattern);
		format.setTimeZone(UTC);
		return format;
	}

	/** A Calendar in UTC. */
	public static Calendar utcCalendar() {
		return Calendar.getInstance(UTC);
	}

	/**
	 * Rewrite the fractional seconds to exactly three digits (milliseconds) so they parse with an
	 * "SSS" (or "SS") pattern. Longer fractions are truncated, shorter ones are padded on the right:
	 * ".975123" becomes ".975" and ".34" becomes ".340".
	 *
	 * SimpleDateFormat reads the fraction as a whole number of milliseconds, so without this
	 * ".975123" was read as 975,123 ms (16 minutes) and ".34" as 34 ms.
	 */
	public static String normalizeFraction(String time) {
		if (time == null) {
			return null;
		}
		Matcher matcher = FRACTION.matcher(time);
		if (!matcher.find()) {
			return time;
		}
		String digits = (matcher.group(2) + "000").substring(0, 3);
		return time.substring(0, matcher.start(2)) + digits + time.substring(matcher.end(2));
	}
}
