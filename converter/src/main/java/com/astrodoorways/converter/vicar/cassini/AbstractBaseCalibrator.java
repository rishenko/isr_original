package com.astrodoorways.converter.vicar.cassini;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;

import org.w3c.dom.Node;

import com.google.common.base.Strings;

public abstract class AbstractBaseCalibrator implements Calibrator {

	public static final String METADATAFORMAT = "javax_imageio_1.0";

	public static double[] readImageAsDoubleArray(File file) throws IOException {
		return readImageAsDoubleArray(readImage(file));
	}

	public static double[] readImageAsDoubleArray(BufferedImage imageRing) throws IOException {
		int width = imageRing.getWidth();
		int height = imageRing.getHeight();
		return imageRing.getData().getPixels(0, 0, width, height, new double[width * height]);
	}

	public static int[] readImageAsArray(File file) throws IOException {
		return readImageAsArray(readImage(file));
	}

	public static int[] readImageAsArray(BufferedImage imageRing) throws IOException {
		int width = imageRing.getWidth();
		int height = imageRing.getHeight();
		return imageRing.getData().getPixels(0, 0, width, height, new int[width * height]);
	}

	public static BufferedImage readImage(File file) throws IOException {
		return ImageIO.read(file);
	}

	public String getNodeString(IIOMetadata metadata) {
		Node node = metadata.getAsTree(METADATAFORMAT);
		return node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();
	}

	protected static String extractValue(String field, String stringToCheck) {
		Scanner scanner = new Scanner(stringToCheck);

		// simple string extraction
		String matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*'([\\w\\s\\p{Punct}&&[^\\n]])*'", 0);
		if (matchedString != null) {
			String[] matchArray = matchedString.split("'");
			if (matchArray.length > 1) {
				scanner.close();
				return matchArray[1].trim();
			}
			scanner.close();
			return "";

		}
		scanner.reset();

		// list extraction
		matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*\\([\\w\\s\\p{Punct}&&[^\\n]]+\\)", 0);
		if (matchedString != null) {
			String value = matchedString.split("=")[1].trim();
			value = value.substring(1, value.length() - 1);
			String[] subValues = value.split(",");
			StringBuilder finalValue = new StringBuilder();
			for (String subValue : subValues) {
				subValue = subValue.trim();
				if (subValue.startsWith("'")) {
					subValue = subValue.substring(1, subValue.length() - 1);
				}
				finalValue.append(subValue).append(",");
			}
			scanner.close();
			return finalValue.substring(0, finalValue.length() - 1);
		}
		scanner.reset();

		// numerical extraction
		matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*([\\w\\s\\p{Punct}&&[^\\n]])+", 0);
		if (matchedString != null) {
			scanner.close();
			return matchedString.split("=")[1].trim();
		}
		scanner.reset();

		// numerical extraction
		matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*[\\d\\.&&[^\\n]]+", 0);
		if (matchedString != null) {
			scanner.close();
			return matchedString.split("=")[1].trim();
		}
		scanner.close();
		return "";
	}

	public static String processFilter(String nodeString) {
		String filter = extractValue("FILTER_NAME", nodeString);
		if (!Strings.isNullOrEmpty(filter)) {
			filter = filter.replace(",", "-");
		} else {
			filter = extractValue("FILTER1_NAME", nodeString) + "-" + extractValue("FILTER2_NAME", nodeString);
		}
		if (Strings.isNullOrEmpty(filter) || filter.trim().equals("-"))
			filter = "NA";
		return filter;
	}
}
