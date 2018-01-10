//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.astrodoorways.converter.vicar.cassini;

import com.google.common.base.Strings;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

public abstract class AbstractBaseCalibrator implements Calibrator {
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractBaseCalibrator.class);

    public static final String METADATAFORMAT = "javax_imageio_1.0";

    public AbstractBaseCalibrator() {
    }

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
        LOGGER.debug("trying to read: {}", file);
        return ImageIO.read(file);
    }

    public String getNodeString(IIOMetadata metadata) {
        Node node = metadata.getAsTree("javax_imageio_1.0");
        String nodeString = node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();
        return nodeString;
    }

    protected static String extractValue(String field, String stringToCheck) {
        Scanner scanner = new Scanner(stringToCheck);
        String matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*'([\\w\\s\\p{Punct}&&[^\\n]])*'", 0);
        if (matchedString != null) {
            String[] matchArray = matchedString.split("'");
            return matchArray.length > 1 ? matchArray[1].trim() : "";
        } else {
            scanner.reset();
            matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*\\([\\w\\s\\p{Punct}&&[^\\n]]+\\)", 0);
            if (matchedString != null) {
                String value = matchedString.split("=")[1].trim();
                value = value.substring(1, value.length() - 1);
                String[] subValues = value.split(",");
                String finalValue = "";
                String[] arr$ = subValues;
                int len$ = subValues.length;

                for (int i$ = 0; i$ < len$; ++i$) {
                    String subValue = arr$[i$];
                    subValue = subValue.trim();
                    if (subValue.startsWith("'")) {
                        subValue = subValue.substring(1, subValue.length() - 1);
                    }

                    finalValue = finalValue + subValue + ",";
                }

                return finalValue.substring(0, finalValue.length() - 1);
            } else {
                scanner.reset();
                matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*([\\w\\s\\p{Punct}&&[^\\n]])+", 0);
                if (matchedString != null) {
                    return matchedString.split("=")[1].trim();
                } else {
                    scanner.reset();
                    matchedString = scanner.findWithinHorizon(field + "\\s*=\\s*[\\d\\.&&[^\\n]]+", 0);
                    if (matchedString != null) {
                        return matchedString.split("=")[1].trim();
                    } else {
                        scanner.reset();
                        return "";
                    }
                }
            }
        }
    }

    public static String processFilter(String nodeString) {
        String filter = extractValue("FILTER_NAME", nodeString);
        if (!Strings.isNullOrEmpty(filter)) {
            filter = filter.replace(",", "-");
        } else {
            filter = extractValue("FILTER1_NAME", nodeString) + "-" + extractValue("FILTER2_NAME", nodeString);
        }

        if (Strings.isNullOrEmpty(filter) || filter.trim().equals("-")) {
            filter = "NA";
        }

        return filter;
    }
}
