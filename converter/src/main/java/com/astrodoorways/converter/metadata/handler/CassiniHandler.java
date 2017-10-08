package com.astrodoorways.converter.metadata.handler;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.google.common.base.Strings;

public class CassiniHandler extends AbstractVicarHandler {

	Logger logger = LoggerFactory.getLogger(CassiniHandler.class);

	@Override
	public Map<String, String> buildValueMapFromMetadata(Node node) {
		Map<String, String> valueMap = new HashMap<String, String>();
		String nodeString = node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();
		valueMap.put(EXPOSURE, extractValue("EXPOSURE_DURATION", nodeString));
		String time = extractValue("IMAGE_TIME", nodeString);
		if (!time.endsWith("Z"))
			time += "Z";
		valueMap.put(TIME, time);
		valueMap.put(CAMERA, extractValue("INSTRUMENT_ID", nodeString));
		valueMap.put(TARGET, extractValue("TARGET_NAME", nodeString));
		valueMap.put(MISSION, extractValue("MISSION_NAME", nodeString));

		processFilter(nodeString, valueMap);
		setValueMap(valueMap);
		return valueMap;
	}

	public void processFilter(String nodeString, Map<String, String> valueMap) {
		String filter = extractValue("FILTER_NAME", nodeString);
		if (!Strings.isNullOrEmpty(filter)) {
			filter = filter.replace(",", "-");
		} else {
			filter = extractValue("FILTER1_NAME", nodeString) + "-" + extractValue("FILTER2_NAME", nodeString);
		}
		if (Strings.isNullOrEmpty(filter) || filter.trim().equals("-"))
			filter = "NA";
		valueMap.put(FILTER, filter);
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
		String mission_name = extractValue("BLTYPE", nodeString);
		return mission_name.matches("CAS[\\w\\d-]*");
	}
}
