package com.astrodoorways.converter.converters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Utils {

    private static Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static void logMinMaxValues(String desc, double[] rasterArray) {
        double[] sortedRasterArray = Arrays.copyOf(rasterArray, rasterArray.length);
        Arrays.sort(sortedRasterArray);
        double min = sortedRasterArray[0];
        double max = sortedRasterArray[sortedRasterArray.length-1];
        LOGGER.debug("min/max values for {} - {}/{}", desc, min, max);
    }
}
