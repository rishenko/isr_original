package com.astrodoorways.converter.mission.cassini;

import org.w3c.dom.Node;

import java.io.IOException;
import javax.imageio.metadata.IIOMetadata;

public class DebiasCalibrator extends AbstractBaseCalibrator {

	@Override
	public boolean calibrate(double[] imageArray, IIOMetadata metadata) throws IOException {
		Node node = metadata.getAsTree(METADATAFORMAT);
		String nodeString = node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();

		String convType = extractValue("DATA_CONVERSION_TYPE", nodeString);
		// cannot use bias_strip_mean on table and lossy due to encoding. will have to
		// switch to using overclocked pixels once implemented
		//		if (convType.equals("TABLE") || convType.equals("LOSSY"))
		//			return false;

		// some labels (e.g. 1996 ground-test images) have no BIAS_STRIP_MEAN; skip instead of
		// failing the whole image with a NumberFormatException
		String biasStripMean = extractValue("BIAS_STRIP_MEAN", nodeString);
		if (biasStripMean.isEmpty())
			return false;
		Double offset = Double.parseDouble(biasStripMean);

		for (int i = 0; i < imageArray.length; i++) {
			imageArray[i] = imageArray[i] - offset;
		}

		return true;
	}

}
