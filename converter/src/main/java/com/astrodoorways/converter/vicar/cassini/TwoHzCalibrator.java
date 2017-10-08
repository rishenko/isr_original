package com.astrodoorways.converter.vicar.cassini;

import java.io.IOException;

import javax.imageio.metadata.IIOMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Jama.Matrix;

public class TwoHzCalibrator extends AbstractBaseCalibrator {

	private String metadataString;
	private double[][] overclockedPixels;
	private double[] overclockedAvg;

	Logger logger = LoggerFactory.getLogger(TwoHzCalibrator.class);

	@Override
	public boolean calibrate(double[] imageArray, IIOMetadata metadata) throws IOException {
		metadataString = getNodeString(metadata);

		String sum = extractValue("INSTRUMENT_MODE_ID", metadataString);
		if (sum.equals("FULL")) {
			logger.debug("No valid prefix bytes to supply overclocked values, 2hz removal skipped");
		}

		// overclock
		if (true) {

			//OverclockAvg
			Integer nl = Integer.parseInt(extractValue("NL", metadataString));

			//Overclock setup
			buildOverclockedData(imageArray);

			// handle delut from 8 to 12
			String convType = extractValue("CONVERSION_TYPE", metadataString);
			if (convType.equals("TABLE")) {
				Lut8to12BitCalibrator lutCalibrator = new Lut8to12BitCalibrator();
				lutCalibrator.lut8to12BitConvert(overclockedAvg);
			}

			// from cassimg_twohz.pro, check this out again some other time
			/*String blType = extractValue("BLTYPE", metadataString);
			if (blType.equals("CAS-ISS3") || blType.equals("CAS-ISS4")) {
				Matrix overclockedAvgMatrix = new Matrix(overclockedAvg, imageArray.length / 2);
				if (overclockedAvg[0] > 2.0 * overclockedAvg[1]) {
					overclockedAvg[0] = overclockedAvg[1];
				}
			}*/

			double[] linbias = CassiniMath.linfit(new double[imageArray.length / 2], overclockedAvg);
			double[] hum = new Matrix(overclockedAvg, 1).plus(new Matrix(linbias, 1).times(-1)).getColumnPackedCopy();

		} else { // image mean

		}

		return false;
	}

	public void buildOverclockedData(double[] imageArray) {
		String format = extractValue("FORMAT", metadataString);
		int rows = imageArray.length / 2;
		overclockedPixels = new double[2][rows];
		Integer nbb = Integer.parseInt(extractValue("NBB", metadataString));
		String blType = extractValue("BLTYPE", metadataString);
		Matrix overclockMatrix = new Matrix(imageArray, rows);
		// handle overclocked pixels
		if (nbb == 24) {
			switch (format) {
			case "BYTE":
				switch (blType) {
				case "CAS-ISS2":
				case "CASSINI-ISS":
					overclockedPixels[0] = overclockMatrix.getMatrix(0, rows, 23, 23).getColumnPackedCopy();
					break;
				case "CAS-ISS3":
				case "CAS-ISS4":
					overclockedPixels[0] = overclockMatrix.getMatrix(0, rows, 13, 13).getColumnPackedCopy();
					overclockedPixels[1] = overclockMatrix.getMatrix(0, rows, 23, 23).getColumnPackedCopy();
					break;
				default:
					logger.debug("no overclock pixels found for the image");
					break;
				}
				break;
			case "HALF":
				switch (blType) {
				case "CAS-ISS2":
				case "CASSINI-ISS":
					overclockedPixels[0] = overclockMatrix.getMatrix(0, rows, 11, 11).getColumnPackedCopy();
					break;
				case "CAS-ISS3":
				case "CAS-ISS4":
					overclockedPixels[0] = overclockMatrix.getMatrix(0, rows, 6, 6).getColumnPackedCopy();
					overclockedPixels[1] = overclockMatrix.getMatrix(0, rows, 11, 11).getColumnPackedCopy();
					break;
				default:
					logger.debug("no overclock pixels found for the image");
					break;
				}
				break;
			}
		}

		// handle overclocked pixel average
		Matrix overclockedPixelMatrix = new Matrix(overclockedPixels);
		if (blType.equals("CAS-ISS2") || blType.equals("CASSINI-ISS")) {
			overclockedAvg = overclockedPixelMatrix.getMatrix(0, rows, 0, 0).getColumnPackedCopy();
		} else if (blType.equals("CAS-ISS3") || blType.equals("CAS-ISS4")) {

			Matrix firstColMatrix = overclockedPixelMatrix.getMatrix(0, rows, 0, 0);
			Matrix secondColMatrix = overclockedPixelMatrix.getMatrix(0, rows, 1, 1);
			switch (format) {
			case "FULL":
				overclockedAvg = firstColMatrix.times(.5).plus(secondColMatrix.times(1d / 6d)).times(.5)
						.getColumnPackedCopy();
				break;
			case "SUM2":
				overclockedAvg = firstColMatrix.plus(secondColMatrix.times(1d / 3d)).times(.5).getColumnPackedCopy();
				break;
			case "SUM4":
				overclockedAvg = firstColMatrix.plus(secondColMatrix).times(.5).getColumnPackedCopy();
				break;
			}
		}
	}

	public void healMissingMin(double[] imageArray) {

	}

	public void healMissingAvg(double[] imageArray) {
	}

	public void findMissing(double[] imageArray) {
		//		List<Integer> missingCoords = new ArrayList<Integer>();
		//		Matrix missingCoords = new Matrix(imageArray, imageArray.length/1024);
		//		missingCoords.
		//		for (int i=0; i<imageArray.length; i++) {
		//			if (imageArray[i] == 0.0) {
		//				missingCoords.add(i);
		//			}
		//		}
	}

	public void autoStarLevel(double[] imageArray) {
	}

}
