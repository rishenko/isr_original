package com.astrodoorways.converter.metadata.handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.base.Strings;

public class GalileoHandler extends AbstractVicarHandler {

	private final Logger logger = LoggerFactory.getLogger(GalileoHandler.class);

	public final SimpleDateFormat GALILEO_FORMATTER = new SimpleDateFormat("yyyyDDDHHmmssSSS");

	@Override
	public Map<String, String> buildValueMapFromMetadata(Node node) {
		Map<String, String> valueMap = new HashMap<>();
		String nodeString = node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();
		valueMap.put(EXPOSURE, "0");
		// example values: ERTYEAR=2001 ERTDAY=350 ERTHOUR=2 ERTMIN=35 ERTSEC=10
		// ERTMSEC=742
		String year = extractValue("ERTYEAR", nodeString);
		String day = extractValue("ERTDAY", nodeString);
		String hour = extractValue("ERTHOUR", nodeString);
		String min = extractValue("ERTMIN", nodeString);
		String sec = extractValue("ERTSEC", nodeString);
		String msec = extractValue("ERTMSEC", nodeString);
		String time = year + Strings.padStart(day, 3, ' ') + Strings.padStart(hour, 2, ' ')
				+ Strings.padStart(min, 2, ' ') + Strings.padStart(sec, 2, ' ') + Strings.padStart(msec, 3, ' ');
		try {
			Date date = GALILEO_FORMATTER.parse(time);
			time = VICAR_FORMATTER.format(date);
		} catch (ParseException e) {
			logger.error("could not process time " + time, e);
			time = VICAR_FORMATTER.format(new Date());
		}
		valueMap.put(TIME, time);
		valueMap.put(CAMERA, extractValue("SENSOR", nodeString));
		valueMap.put(FILTER, extractValue("FILTER", nodeString));
		valueMap.put(TARGET, extractValue("TARGET", nodeString));
		valueMap.put(MISSION, extractValue("MISSION", nodeString));

		return valueMap;
	}

	@Override
	public boolean canProcessMetadata(Node node) {
		Node firstLastChild = node.getLastChild();
		if (firstLastChild == null)
			return false;

		Node secondChild = firstLastChild.getLastChild();
		if (secondChild == null)
			return false;

		NamedNodeMap nodeMap = secondChild.getAttributes();
		if (nodeMap == null || nodeMap.getLength() < 3)
			return false;

		String nodeString = nodeMap.item(2).getNodeValue();
		String mission_name = extractValue("MISSION", nodeString);

		return "GALILEO".equals(mission_name);
	}
}
