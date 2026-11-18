package com.astrodoorways.converter.mission.cassini;

import java.io.IOException;

import javax.imageio.metadata.IIOMetadata;

public interface Calibrator {
	boolean calibrate(double[] imageArray, IIOMetadata metadata) throws IOException;
}
