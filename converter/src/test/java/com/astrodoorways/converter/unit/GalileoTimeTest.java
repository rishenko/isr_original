package com.astrodoorways.converter.unit;

import com.astrodoorways.converter.metadata.handler.GalileoHandler;
import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.Assert.assertEquals;

/**
 * Galileo labels split the time into separate fields (ERTYEAR,\nERTDAY, ...). They were padded
 * with spaces, so any one-digit field failed to parse and the image silently got the current time.
 */
public class GalileoTimeTest {

	private static Document labelTree(String label) throws Exception {
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element root = doc.createElement("root");
		Element child = doc.createElement("child");
		Element leaf = doc.createElement("leaf");
		// the handler reads the third attribute; give all three the label so attribute order doesn't matter
		leaf.setAttribute("a", label);
		leaf.setAttribute("b", label);
		leaf.setAttribute("c", label);
		child.appendChild(leaf);
		root.appendChild(child);
		doc.appendChild(root);
		return doc;
	}

	private static String timeFor(String label) throws Exception {
		return new GalileoHandler().buildValueMapFromMetadata(labelTree(label).getDocumentElement()).get(MetadataHandler.TIME);
	}

	@Test
	public void singleDigitFields() throws Exception {
		assertEquals("2001-350T02:05:01.042Z",
				timeFor("MISSION='GALILEO'\nERTYEAR=2001\nERTDAY=350\nERTHOUR=2\nERTMIN=5\nERTSEC=1\nERTMSEC=42"));
	}

	@Test
	public void fullWidthFields() throws Exception {
		assertEquals("1996-045T14:35:10.742Z",
				timeFor("MISSION='GALILEO'\nERTYEAR=1996\nERTDAY=45\nERTHOUR=14\nERTMIN=35\nERTSEC=10\nERTMSEC=742"));
	}
}
