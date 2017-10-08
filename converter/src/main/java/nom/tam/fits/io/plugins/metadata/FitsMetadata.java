package nom.tam.fits.io.plugins.metadata;

import java.util.Iterator;

import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;

import org.w3c.dom.Node;

public class FitsMetadata extends IIOMetadata {
	private Node node;

	public FitsMetadata(Header header) {
		this.nativeMetadataFormatName = "javax_imageio_1.0";
		buildNode(header);
	}

	private void buildNode(Header header) {

		IIOMetadataNode root = new IIOMetadataNode(nativeMetadataFormatName);

		Iterator it = header.iterator();
		StringBuilder textNodeStr = new StringBuilder();
		while (it.hasNext()) {
			HeaderCard card = (HeaderCard) it.next();
			IIOMetadataNode element = new IIOMetadataNode("item");
			String key = card.getKey();
			String value = card.getValue();
			String comment = card.getComment();

			if (key == null && comment != null) {
				String[] vals = comment.split("=");
				if (vals.length == 2) {
					key = vals[0];
					value = vals[1];
				}
			}

			if (key == null) {
				key = "NA";
			}

			if (value == null) {
				value = "NA";
			}
			key = key.trim();
			value = value.trim();

			textNodeStr.append(key).append("=").append(value).append(System.lineSeparator());
			element.setAttribute("key", key);
			element.setAttribute("value", value);
			root.appendChild(element);
		}

		IIOMetadataNode textElement = new IIOMetadataNode("Text");
		IIOMetadataNode textEntryElement = new IIOMetadataNode("TextEntry");
		textEntryElement.setAttribute("keyword", "description");
		String fullHeaderText = textNodeStr.toString();
		textEntryElement.setAttribute("value", fullHeaderText);
		textEntryElement.setAttribute("ImageDescription", fullHeaderText);
		textElement.appendChild(textEntryElement);
		root.appendChild(textElement);

		node = root;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public Node getAsTree(String formatName) {
		return node;
	}

	@Override
	public void mergeTree(String formatName, Node root) throws IIOInvalidTreeException {
		throw new RuntimeException("you cannot merge the FITs tree");
	}

	@Override
	public void reset() {

	}

}
