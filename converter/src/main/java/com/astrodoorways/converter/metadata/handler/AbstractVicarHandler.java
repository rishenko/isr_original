package com.astrodoorways.converter.metadata.handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.astrodoorways.converter.ApplicationProperties;

public abstract class AbstractVicarHandler implements MetadataHandler {

	public final SimpleDateFormat VICAR_FORMATTER = TimeFormats.utc("yyyy-DDD'T'HH:mm:ss.SSS'Z'");

	private Map<String, String> valueMap = new HashMap<>();

	@Override
	abstract public Map<String, String> buildValueMapFromMetadata(Node node);

	@Override
	abstract public boolean canProcessMetadata(Node node);

	private String sequencedFileText;

	public AbstractVicarHandler() {
		setSequencedFileText(ApplicationProperties.getPropertyAsString(ApplicationProperties.SEQUENCE));
	}

	protected String extractValue(String field, String stringToCheck) {
		Scanner scanner = new Scanner(stringToCheck);

		// simple string extraction
		String matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*'([\\w\\s\\p{Punct}&&[^\\n]])*'", 0);
		if (matchedString != null) {
			String[] matchArray = matchedString.split("'");
			if (matchArray.length > 1) {
				scanner.close();
				return matchArray[1].trim();
			}
			scanner.close();
			return "";

		}
		scanner.reset();

		// list extraction
		matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*\\([\\w\\s\\p{Punct}&&[^\\n]]+\\)", 0);
		if (matchedString != null) {
			String value = matchedString.split("=")[1].trim();
			value = value.substring(1, value.length() - 1);
			String[] subValues = value.split(",");
			StringBuilder finalValue = new StringBuilder();
			for (String subValue : subValues) {
				subValue = subValue.trim();
				if (subValue.startsWith("'")) {
					subValue = subValue.substring(1, subValue.length() - 1);
				}
				finalValue.append(subValue).append(",");
			}
			scanner.close();
			return finalValue.substring(0, finalValue.length() - 1);
		}
		scanner.reset();

		// numerical extraction
		matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*([\\w\\s\\p{Punct}&&[^\\n]])+", 0);
		if (matchedString != null) {
			scanner.close();
			return matchedString.split("=")[1].trim();
		}
		scanner.reset();

		// numerical extraction
		matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*[\\d\\.&&[^\\n]]+", 0);
		if (matchedString != null) {
			scanner.close();
			return matchedString.split("=")[1].trim();
		}
		scanner.reset();
		scanner.close();
		return "";
	}

	public Calendar convertVicarDateToDate(String vicarDate) throws ParseException {
		Date date = getFormatter().parse(TimeFormats.normalizeFraction(vicarDate));

		Calendar calendar = TimeFormats.utcCalendar();
		calendar.setTime(date);

		return calendar;
	}

	@Override
	public SimpleDateFormat getFormatter() {
		return VICAR_FORMATTER;
	}

	@Override
	public String getFileStructure() {
		String fileStructure = "";

		try {
			Calendar date = convertVicarDateToDate(valueMap.get(TIME));
			fileStructure += dateFolder(date);
		} catch (Exception e) {
			// do nothing
		}
		return fileStructure;
	}

	/**
	 * The year/month/day output folder for a date, e.g. "2012/03/27". Calendar months are
	 * zero-based, which used to put March in folder "2".
	 */
	public static String dateFolder(Calendar date) {
		return String.format("%d/%02d/%02d", date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1,
				date.get(Calendar.DAY_OF_MONTH));
	}

	public final SimpleDateFormat TIMESTAMP_FORMATTER = TimeFormats.utc("yyyyMMddHHmmssSSS");

	public String convertVicarDateToTimestamp(String vicarDate) throws ParseException {
		Date date = getFormatter().parse(TimeFormats.normalizeFraction(vicarDate));

		return TIMESTAMP_FORMATTER.format(date);
	}

	@Override
	public String getFileName() throws ParseException {
		return buildFileName(null);
	}

	/**
	 * @param sequenceNumber the image's number within its target and filter, assigned in time order
	 *                       by the converter; used only when a sequence name is configured
	 */
	@Override
	public String getFileName(int sequenceNumber) throws ParseException {
		return buildFileName(sequenceNumber);
	}

	private String buildFileName(Integer sequenceNumber) throws ParseException {
		String mission = getValueMap().get(MISSION);
		String target = getValueMap().get(TARGET);
		String filter = getValueMap().get(FILTER);
		String fileName;
		try {
			String time = convertVicarDateToTimestamp(getValueMap().get(TIME));
			if (isSequencedFile()) {
				// standalone conversions without an assigned number fall back to a JVM-wide counter
				int number = sequenceNumber != null ? sequenceNumber : Sequencer.incrementAndGet(target, filter);
				String sequence = number + "";
				sequence = "0000000000".substring(sequence.length()) + sequence;
				fileName = target + "_" + filter + "_" + getSequencedFileText() + "_" + sequence;
			} else {
				fileName = mission + "_" + target + "_" + filter + "_" + time;
			}
		} catch (ParseException e) {
			String time = convertVicarDateToTimestamp(getFormatter().format(Calendar.getInstance().getTime()));
			fileName = target + "_" + filter + "_" + time;
		}
		// a "/" in a label value (SDO's mission is "SDO/JSOC-SDP") would add an unintended folder
		return fileName.replace("/", "-");
	}

	public Map<String, String> getValueMap() {
		return valueMap;
	}

	public void setValueMap(Map<String, String> valueMap) {
		this.valueMap = valueMap;
	}

	public boolean isSequencedFile() {
		return getSequencedFileText() != null && !getSequencedFileText().trim().equals("");
	}

	public String getSequencedFileText() {
		return sequencedFileText;
	}

	public void setSequencedFileText(String sequencedFileText) {
		this.sequencedFileText = sequencedFileText;
	}
}
