package com.astrodoorways.converter.metadata.handler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

public class GenericMetadataHandler implements MetadataHandler {

	public static final SimpleDateFormat VICAR_FORMATTER = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS'Z'");

	Map<String, String> valueMap = new HashMap<String, String>();

	@Override
	public Map<String, String> buildValueMapFromMetadata(Node node) {
		//		String nodeString = node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();
		Map<String, String> valueMap = new HashMap<String, String>();
		valueMap.put(EXPOSURE, "0.0");
		valueMap.put(TIME, VICAR_FORMATTER.format(new Date()));
		valueMap.put(CAMERA, "");
		valueMap.put(FILTER, "NA");
		valueMap.put(TARGET, "EMPTY");
		valueMap.put(MISSION, "NOTAVAILABLE");
		this.valueMap = valueMap;
		return valueMap;
	}

	@Override
	public boolean canProcessMetadata(Node node) {
		return true;
	}

	public SimpleDateFormat getFormatter() {
		return VICAR_FORMATTER;
	}

	@Override
	public String getFileStructure() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getFileName() {
		// TODO Auto-generated method stub
		return null;
	}

	public Map<String, String> getValueMap() {
		return valueMap;
	}

	public void setValueMap(Map<String, String> valueMap) {
		this.valueMap = valueMap;
	}
}
