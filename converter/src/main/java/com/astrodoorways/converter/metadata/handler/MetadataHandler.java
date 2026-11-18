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

	/**
	 * The file name for an image in a sequence run, using a sequence number assigned by the caller.
	 * Handlers without sequence support ignore the number.
	 */
	default String getFileName(int sequenceNumber) throws ParseException {
		return getFileName();
	}

	Map<String, String> getValueMap();

	void setValueMap(Map<String, String> valueMap);

}
