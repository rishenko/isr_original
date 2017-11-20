package com.astrodoorways.converter.metadata.handler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class SDOHandler extends AbstractVicarHandler {

	public final SimpleDateFormat SDO_TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");

	@Override
	public Map<String, String> buildValueMapFromMetadata(Node node) {
		String nodeString = node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();
		getValueMap().put(EXPOSURE, extractValue("EXPTIME", nodeString));
		String time = extractValue("T_OBS", nodeString);
		if (!time.endsWith("Z"))
			time += "Z";
		getValueMap().put(TIME, time);
		getValueMap().put(CAMERA, extractValue("INSTRUMEN", nodeString));
		getValueMap().put(TARGET, "SUN");
		getValueMap().put(MISSION, extractValue("ORIGIN", nodeString));
		getValueMap().put(FILTER, extractValue("WAVE_STR", nodeString));
		return getValueMap();
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
		String mission_name = extractValue("ORIGIN", nodeString);
		return mission_name.startsWith("SDO");
	}

	@Override
	public SimpleDateFormat getFormatter() {
		return SDO_TIME_FORMATTER;
	}

	@Override
	public String getFileStructure() {
		String fileStructure = "";
		try {
			Calendar date = convertVicarDateToDate(getValueMap().get(TIME));
			fileStructure += date.get(Calendar.YEAR) + "/" + date.get(Calendar.MONTH) + "/"
					+ date.get(Calendar.DAY_OF_MONTH);
		} catch (Exception e) {
			// do nothing
		}
		return fileStructure;
	}

	//	SDO_AIA1200_JAN1-APRIL1_3-SECONDINTERVALS_000000001.TIF. Next chronologicial file would be SDO_AIA1200_JAN1-APRIL1_3-SECONDINTERVALS_000000002.TIF

}
