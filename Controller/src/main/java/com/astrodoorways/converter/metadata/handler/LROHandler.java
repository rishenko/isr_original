package com.astrodoorways.converter.metadata.handler;

import java.text.SimpleDateFormat;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class LROHandler extends AbstractVicarHandler {

	Logger logger = LoggerFactory.getLogger(LROHandler.class);

	public final SimpleDateFormat LROC_TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // 2011-02-28T17:07:39

	@Override
	public Map<String, String> buildValueMapFromMetadata(Node node) {
		convertMetadataNodeToHashMap(node, "n");
		//		String nodeString = node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();
		getValueMap().put(EXPOSURE, "1");
		String time = getValueMap().get("PRODUCT_CREATION_TIME");
		if (!time.endsWith("Z"))
			time += "Z";
		getValueMap().put(TIME, time);
		getValueMap().put(CAMERA, getValueMap().get("INSTRUMENT_ID"));
		getValueMap().put(TARGET, "MOON");
		getValueMap().put(MISSION, "LROC");
		getValueMap().put(FILTER, getValueMap().get("CENTER_FILTER_WAVELENGTH"));
		return getValueMap();
	}

	@Override
	public boolean canProcessMetadata(Node node) {
		convertMetadataNodeToHashMap(node, "n");
		logger.debug("Value Map: {}", getValueMap());
		return getValueMap().containsKey("INSTRUMENT_ID") && getValueMap().get("INSTRUMENT_ID").equals("LROC");
	}

	@Override
	public SimpleDateFormat getFormatter() {
		return LROC_TIME_FORMATTER;
	}

	@Override
	public String getFileStructure() {
		String fileStructure = "";
		//		try {
		//			Calendar date = convertVicarDateToDate(getValueMap().get(TIME));
		//			fileStructure += date.get(Calendar.YEAR) + "/" + date.get(Calendar.MONTH) + "/"
		//					+ date.get(Calendar.DAY_OF_MONTH);
		//		} catch (Exception e) {
		//			// do nothing
		//		}
		return fileStructure;
	}

	public void convertMetadataNodeToHashMap(Node node, String prefix) {
		String val = node.getTextContent();
		logger.trace("{}: {}", new Object[] { prefix, val });

		if (!prefix.endsWith("a")) {
			NamedNodeMap map = node.getAttributes();
			if (map != null && map.getLength() > 0) {
				String key = map.item(0).getTextContent();
				getValueMap().put(key, val);
				convertMetadataNodeToHashMap(map.item(0), prefix + "a");
			}
			NodeList nodeList = node.getChildNodes();
			if (nodeList != null) {
				for (int i = 0; i < nodeList.getLength(); i++) {
					convertMetadataNodeToHashMap(nodeList.item(i), prefix + "c");
				}
			}
		}
	}

}
