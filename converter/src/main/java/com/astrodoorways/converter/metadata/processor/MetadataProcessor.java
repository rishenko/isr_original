package com.astrodoorways.converter.metadata.processor;

import java.text.ParseException;
import com.astrodoorways.converter.metadata.handler.TimeFormats;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.metadata.IIOMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.astrodoorways.converter.metadata.handler.CassiniHandler;
import com.astrodoorways.converter.metadata.handler.GalileoHandler;
import com.astrodoorways.converter.metadata.handler.GenericMetadataHandler;
import com.astrodoorways.converter.metadata.handler.LROHandler;
import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.metadata.handler.RosettaHandler;
import com.astrodoorways.converter.metadata.handler.SDOHandler;

public class MetadataProcessor {

	public static final String EXPOSURE = "exposure";
	public static final String TIME = "time";
	public static final String CAMERA = "camera";
	public static final String FILTER = "filter";
	public static final String TARGET = "target";
	public static final String MISSION = "mission";
	public static final String METADATAFORMAT = "javax_imageio_1.0";

	public final SimpleDateFormat VICAR_FORMATTER = TimeFormats.utc("yyyy-DDD'T'HH:mm:ss.SSS'Z'");
	public final SimpleDateFormat EXIF_FORMATTER = TimeFormats.utc("yyyy:MM:dd HH:mm:ss");

	private MetadataHandler handler;

	private final List<MetadataHandler> handlerList = new ArrayList<>();

	final Logger logger = LoggerFactory.getLogger(MetadataProcessor.class);

	public MetadataProcessor() {
		buildHandlerMap();
	}

	public Map<String, String> process(IIOMetadata metadata) {
		if (metadata == null)
			throw new IllegalArgumentException("metadata cannot be empty");

		Node node = metadata.getAsTree(METADATAFORMAT);
		if (node == null)
			node = metadata.getAsTree(metadata.getNativeMetadataFormatName());
		handler = findMetadataHandler(node);
		return handler.buildValueMapFromMetadata(node);
	}

	private MetadataHandler findMetadataHandler(Node node) {
		for (MetadataHandler handler : handlerList) {
			try {
				if (handler.canProcessMetadata(node))
					return handler;
			} catch (Exception e) {
				logger.error("{} errored attempting to inspect metadata", handler);
			}
		}
		return new GenericMetadataHandler();
	}

	private void buildHandlerMap() {
		handlerList.add(new CassiniHandler());
		handlerList.add(new GalileoHandler());
		handlerList.add(new SDOHandler());
		handlerList.add(new LROHandler());
		handlerList.add(new RosettaHandler());
	}

	/**
	 * Parse a label time with the format of the handler that read the label, falling back to the
	 * VICAR and ISO formats.
	 *
	 * @return the time, or null if it can't be parsed
	 */
	public Date parseTime(String time) {
		String normalized = TimeFormats.normalizeFraction(time);
		if (normalized == null)
			return null;
		List<SimpleDateFormat> formats = new ArrayList<>();
		if (handler != null)
			formats.add((SimpleDateFormat) handler.getFormatter().clone()); // never share a formatter across threads
		formats.add(TimeFormats.utc("yyyy-DDD'T'HH:mm:ss.SSS'Z'"));
		formats.add(TimeFormats.utc("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"));
		for (SimpleDateFormat format : formats) {
			try {
				return format.parse(normalized);
			} catch (ParseException e) {
				// try the next format
			}
		}
		return null;
	}

	public String convertVicarDateToExifFormat(String vicarDate) throws ParseException {
		Date date = handler.getFormatter().parse(TimeFormats.normalizeFraction(vicarDate));
		return EXIF_FORMATTER.format(date);
	}

	/**
	 * convert a node containing metadata into a flat map
	 * 
	 * @param node
	 * @return
	 */
	public static Map<String, String> buildValueMapFromNode(Node node) {
		Map<String, String> valueMap = new HashMap<>();
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			Node child = list.item(i);
			if (child.getNodeName().equals("Other")) {
				NamedNodeMap nodeMap = child.getAttributes();
				valueMap.put(EXPOSURE, nodeMap.getNamedItem("exposure").getNodeValue());
				valueMap.put(TIME, nodeMap.getNamedItem("time").getNodeValue());
				valueMap.put(CAMERA, nodeMap.getNamedItem("camera").getNodeValue());
				valueMap.put(FILTER, nodeMap.getNamedItem("filter").getNodeValue());
				valueMap.put(TARGET, nodeMap.getNamedItem("target").getNodeValue());
				valueMap.put(MISSION, nodeMap.getNamedItem("mission").getNodeValue());
			}
		}
		return valueMap;
	}

	public String getFileStructure() {
		return handler.getFileStructure();
	}

	public String getFileName() throws ParseException {
		return handler.getFileName();
	}

	public String getFileName(int sequenceNumber) throws ParseException {
		return handler.getFileName(sequenceNumber);
	}
}
