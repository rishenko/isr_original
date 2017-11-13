package com.astrodoorways.converter.metadata.handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import com.astrodoorways.converter.ApplicationProperties;

public abstract class AbstractVicarHandler implements MetadataHandler {

	public final SimpleDateFormat VICAR_FORMATTER = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS'Z'");

	private static final Logger logger = LoggerFactory.getLogger(AbstractVicarHandler.class);

	private Map<String, String> valueMap = new HashMap<String, String>();

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
			String finalValue = "";
			for (String subValue : subValues) {
				subValue = subValue.trim();
				if (subValue.startsWith("'")) {
					subValue = subValue.substring(1, subValue.length() - 1);
				}
				finalValue += subValue + ",";
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
		Date date;
		try {
			date = getFormatter().parse(vicarDate);
		} catch (ParseException e) {
			if (Pattern.matches(".*\\.\\d{6}\\D*", vicarDate)) {
				vicarDate = vicarDate.replaceAll("(.*\\.)(\\d{3})(\\d{3})(Z.*)*", "$1$2$4");
				date = getFormatter().parse(vicarDate);
			} else {
				throw e;
			}
		}

		Calendar calendar = Calendar.getInstance();
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
			fileStructure += date.get(Calendar.YEAR) + "/" + date.get(Calendar.MONTH) + "/"
					+ date.get(Calendar.DAY_OF_MONTH);
		} catch (Exception e) {
			// do nothing
		}
		return fileStructure;
	}

	public final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyyMMddhhmmssSSS");

	public String convertVicarDateToTimestamp(String vicarDate) throws ParseException {
		Date date;
		try {
			date = getFormatter().parse(vicarDate);
		} catch (ParseException e) {
			if (Pattern.matches(".*\\.\\d{6}\\D*", vicarDate)) {
				vicarDate = vicarDate.replaceAll("(.*\\.)(\\d{3})(\\d{3})(Z.*)*", "$1$2$4");
				date = getFormatter().parse(vicarDate);
			} else {
				throw e;
			}
		}

		String finalDate = TIMESTAMP_FORMATTER.format(date);
		return finalDate;
	}

	@Override
	public String getFileName() throws ParseException {
		String mission = getValueMap().get(MISSION);
		String target = getValueMap().get(TARGET);
		String filter = getValueMap().get(FILTER);
		String fileName = "";
		try {
			String time = convertVicarDateToTimestamp(getValueMap().get(TIME));
			if (isSequencedFile()) {
				String sequence = Sequencer.incrementAndGet(target, filter) + "";
				sequence = "0000000000".substring(sequence.length()) + sequence;
				fileName = target + "_" + filter + "_" + getSequencedFileText() + "_" + sequence;
			} else {
				fileName = mission + "_" + target + "_" + filter + "_" + time;
			}
		} catch (ParseException e) {
			String time = convertVicarDateToTimestamp(getFormatter().format(Calendar.getInstance().getTime()));
			fileName = target + "_" + filter + "_" + time;
		}
		return fileName;
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
