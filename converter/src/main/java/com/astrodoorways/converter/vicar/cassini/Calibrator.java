package com.astrodoorways.converter.vicar.cassini;

import java.io.IOException;

import javax.imageio.metadata.IIOMetadata;

public interface Calibrator {
	boolean calibrate(double[] imageArray, IIOMetadata metadata) throws IOException;
}
