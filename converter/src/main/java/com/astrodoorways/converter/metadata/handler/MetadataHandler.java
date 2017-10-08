package com.astrodoorways.converter.metadata.handler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.w3c.dom.Node;

public interface MetadataHandler {

	public static final String METADATA_FORMAT = "javax_imageio_1.0";
	public static final String EXPOSURE = "exposure";
	public static final String TIME = "time";
	public static final String CAMERA = "camera";
	public static final String FILTER = "filter";
	public static final String TARGET = "target";
	public static final String MISSION = "mission";

	Map<String, String> buildValueMapFromMetadata(Node node);

	boolean canProcessMetadata(Node node);

	SimpleDateFormat getFormatter();

	String getFileStructure();

	String getFileName() throws ParseException;

	Map<String, String> getValueMap();

	void setValueMap(Map<String, String> valueMap);

}
