package com.astrodoorways.converter.metadata.handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.w3c.dom.Node;

public interface MetadataHandler {

	String METADATA_FORMAT = "javax_imageio_1.0";
	String EXPOSURE = "exposure";
	String TIME = "time";
	String CAMERA = "camera";
	String FILTER = "filter";
	String TARGET = "target";
	String MISSION = "mission";

	Map<String, String> buildValueMapFromMetadata(Node node);

	boolean canProcessMetadata(Node node);

	SimpleDateFormat getFormatter();

	String getFileStructure();

	String getFileName() throws ParseException;

	Map<String, String> getValueMap();

	void setValueMap(Map<String, String> valueMap);

}
