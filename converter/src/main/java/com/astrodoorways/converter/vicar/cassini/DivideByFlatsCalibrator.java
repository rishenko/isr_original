package com.astrodoorways.converter.vicar.cassini;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.metadata.IIOMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DivideByFlatsCalibrator extends AbstractBaseCalibrator {

	Logger logger = LoggerFactory.getLogger(DivideByFlatsCalibrator.class);

	private final String calibrationDirectory;

	public DivideByFlatsCalibrator(String calibrationDirectory) {
		this.calibrationDirectory = calibrationDirectory;
	}

	@Override
	public boolean calibrate(double[] imageArray, IIOMetadata metadata) throws IOException {
		String nodeString = getNodeString(metadata);
		String sum = extractValue("INSTRUMENT_MODE_ID", nodeString);
		if (!sum.equals("FULL")) {
			return false;
		}

		String opticsTempStr = extractValue("OPTICS_TEMPERATURE", nodeString);
		double[] opticsTemp = new double[2];
		if (opticsTempStr.startsWith("(")) {
			String[] opticsTempStrArray = opticsTempStr.substring(1, opticsTempStr.length() - 1).split(",");
			opticsTemp[0] = Double.parseDouble(opticsTempStrArray[0]);
			opticsTemp[1] = Double.parseDouble(opticsTempStrArray[1]);
		} else if (opticsTempStr.contains(",")) {
			String[] opticsTempStrArray = opticsTempStr.split(",");
			opticsTemp[0] = Double.parseDouble(opticsTempStrArray[0]);
			opticsTemp[1] = Double.parseDouble(opticsTempStrArray[1]);
		}

		String tName = "";
		if (opticsTemp[0] < -5.0)
			tName = "m10";
		else if (opticsTemp[0] < 25.0)
			tName = "p5";
		else
			tName = "p25";

		//until mid 2000, filters were name individ, then latterly as a parenthesised pair of quoted strings
		String filter1 = extractValue("FILTER1_NAME", nodeString);
		String filter2 = extractValue("FILTER2_NAME", nodeString);
		if (filter1 == null || filter1.equals("")) {
			String[] filters = extractValue("FILTER_NAME", nodeString).split(",");
			filter1 = filters[0];
			filter2 = filters[1];
		}

		List<SlopeFile> slopeFileList = buildSlopeFileList();
		String instrument = extractValue("INSTRUMENT_ID", nodeString);
		String slopeFile = null;
		for (SlopeFile testSlopeFile : slopeFileList) {
			if (testSlopeFile.getInstrument().equals(instrument)
					&& (testSlopeFile.getOpticsTemp().equals(tName) || testSlopeFile.getInstrument().equals("ISSWA"))
					&& testSlopeFile.getFilter1().equals(filter1) && testSlopeFile.getFilter2().equals(filter2)) {
				slopeFile = testSlopeFile.getFileName();
				break;
			}
		}

		File slopeFileRef = new File(calibrationDirectory + "/calib/slope/" + slopeFile.toLowerCase());
		BufferedImage slopeImage = readImage(slopeFileRef);

		double[] slopePixels = readImageAsDoubleArray(slopeImage);
		//		double[] slopePixels = pivotArray(slopePixelsOrig, 1024);

		//		slopePixels = resize(slopePixels, 1024, 1024);
		double[] centerFlatFieldPixels = Matrix.getArrayByRange(slopePixels, 312, 711, 312, 711, 1024);
		Arrays.sort(centerFlatFieldPixels);
		double median2 = 0;
		if (centerFlatFieldPixels.length % 2 > 0) {
			median2 = centerFlatFieldPixels[(centerFlatFieldPixels.length - 1) / 2];
		} else {
			median2 = (centerFlatFieldPixels[centerFlatFieldPixels.length / 2] + centerFlatFieldPixels[(centerFlatFieldPixels.length - 1) / 2]) / 2;
		}

		slopePixels = Matrix.divideByScalar(slopePixels, median2);
		//		slopePixels = resize(slopePixels, dimension, dimension);
		//		double[] slopePixelsFinal = pivotArray(slopePixels, 1024);
		double[] finalImageArrayCalc = Matrix.multipleByArray(imageArray, slopePixels);

		//		double[] finalImageArrayCalc = new Matrix(imageArray, dimension).arrayRightDivide(
		//				new Matrix(slopePixels, dimension)).getColumnPackedCopy();

		for (int i = 0; i < imageArray.length; i++) {
			imageArray[i] = finalImageArrayCalc[i];
		}

		return true;
	}

	public double[] pivotArray(double vals[], int m) {
		int n = (m != 0 ? vals.length / m : 0);
		if (m * n != vals.length) {
			throw new IllegalArgumentException("Array length must be a multiple of m.");
		}
		double[] A = new double[m * n];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				A[i + j * m] = vals[j + i * m];
			}
		}
		return A;
	}

	public double[] reversePivotArray(double vals[], int m) {
		int n = (m != 0 ? vals.length / m : 0);
		if (m * n != vals.length) {
			throw new IllegalArgumentException("Array length must be a multiple of m.");
		}
		double[] A = new double[m * n];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				A[j + i * m] = vals[i + j * m];
			}
		}
		return A;
	}

	public List<SlopeFile> buildSlopeFileList() throws NumberFormatException, IOException {
		List<SlopeFile> slopeFiles = new ArrayList<SlopeFile>();
		BufferedReader reader = new BufferedReader(new FileReader(new File(calibrationDirectory
				+ "/calib/slope/slope_db_2.tab")));
		boolean beginParsing = false;
		String line = "";
		while ((line = reader.readLine()) != null) {
			if (!beginParsing && line.equals("\\begindata")) {
				beginParsing = true;
				continue;
			} else if (!beginParsing) {
				continue;
			}
			String[] dataRow = line.replaceAll("\\s+", " ").split(" ");
			slopeFiles.add(new SlopeFile(dataRow[0], dataRow[1], dataRow[2], dataRow[3], Integer.parseInt(dataRow[4]),
					dataRow[5], Integer.parseInt(dataRow[6]), dataRow[7]));
		}
		reader.close();
		return slopeFiles;
	}

	public static class SlopeFile {
		private final String instrument;
		private final String opticsTemp;
		private final String filter1;
		private final String filter2;
		private final Integer gainState;
		private final String antibloomingMode;
		private final Integer fileNumber; //not used
		private final String fileName;

		public SlopeFile(String instrument, String opticsTemp, String filter1, String filter2, Integer gainState,
						 String antibloomingMode, Integer fileNumber, String fileName) {
			this.instrument = instrument.replace("'", "");
			this.opticsTemp = opticsTemp;
			this.filter1 = filter1.replace("'", "");
			;
			this.filter2 = filter2.replace("'", "");
			;
			this.gainState = gainState;
			this.antibloomingMode = antibloomingMode;
			this.fileNumber = fileNumber;
			this.fileName = fileName.toLowerCase();
		}

		public String getInstrument() {
			return instrument;
		}

		public String getOpticsTemp() {
			return opticsTemp;
		}

		public String getFilter1() {
			return filter1;
		}

		public String getFilter2() {
			return filter2;
		}

		public Integer getGainState() {
			return gainState;
		}

		public String getAntibloomingMode() {
			return antibloomingMode;
		}

		public Integer getFileNumber() {
			return fileNumber;
		}

		public String getFileName() {
			return fileName;
		}
	}

	private void setLabels(String nodeString) {

		String bltype = extractValue("BLTYPE", nodeString);
		String instrumentModeId = extractValue("INSTRUMENT_MODE_ID", nodeString);
		String shutterModeId = extractValue("SHUTTER_MODE_ID", nodeString);
		String gainModeId = extractValue("GAIN_MODE_ID", nodeString);

		int gainState = 0;

		if (gainModeId.equals("1400K") || gainModeId.equals("215 e/DN") || gainModeId.equals("215 ELECTRONS PER DN")) {
			gainState = 0;
		} else if (gainModeId.equals("400K") || gainModeId.equals("95 e/DN")
				|| gainModeId.equals("95 ELECTRONS PER DN")) {
			gainState = 1;
		} else if (gainModeId.equals("100K") || gainModeId.equals("29 e/DN")
				|| gainModeId.equals("29 ELECTRONS PER DN")) {
			gainState = 2;
		} else if (gainModeId.equals("40K") || gainModeId.equals("12 e/DN") || gainModeId.equals("12 ELECTRONS PER DN")) {
			gainState = 3;
		} else {
			logger.debug("unexpected gain state value: {}", gainModeId);
		}

		String lfFlag = extractValue("LIGHT_FLOOD_STATE_FLAG", nodeString);
		String abFlag = extractValue("ANTIBLOOMING_STATE_FLAG", nodeString);
		String target = extractValue("TARGET_NAME", nodeString);
		String obsId = extractValue("OBSERVATION_ID", nodeString);
		String missLnsStr = extractValue("MISSING_LINES", nodeString);
		int missLns = missLnsStr.equals("N/A") ? 0 : Integer.parseInt(missLnsStr);
		String instrument = extractValue("INSTRUMENT_ID", nodeString);

		// needed for new linetime code
		String flightSoftwareVersion = extractValue("FLIGHT_SOFTWARE_VERSION_ID", nodeString);
		double fws = 0;
		if (flightSoftwareVersion != null)
			fws = Double.parseDouble(flightSoftwareVersion);
		else
			fws = 1.3;

		/// accommodate changed keywords as of Fomalhaut/Jupiter
		String encType = extractValue("ENCODING_TYPE", nodeString);
		if (encType == null) {
			encType = extractValue("INST_CMPRS_TYPE", nodeString);
		}

		String convType = extractValue("CONVERSION_TYPE", nodeString);
		if (convType == null) {
			convType = extractValue("DATA_CONVERSION_TYPE", nodeString);
		}

		String clsFlag = extractValue("CALIB_STATE_FLAG", nodeString);
		if (clsFlag == null) {
			clsFlag = extractValue("CALIBRATION_LAMP_STATE_FLAG", nodeString);
		}

		String offset = extractValue("BIAS_STRIP_MEAN", nodeString);
		if (offset == null) {
			offset = extractValue("BIAS", nodeString);
		}
		if (offset == null) {
			offset = extractValue("OFFSET", nodeString);
		}

		//until mid 2000, filters were name individ, then latterly as a parenthesised pair of quoted strings
		String filter1 = extractValue("FILTER1_NAME", nodeString);
		String filter2 = extractValue("FILTER2_NAME", nodeString);
		if (filter1 == null) {
			String[] filters = extractValue("FILTER_NAME", nodeString).split(",");
			filter1 = filters[0];
			filter2 = filters[1];
		}

		String calibrated = extractValue("CALIBRATION_STAGE", nodeString);
	}

	double ll_ll = 0;
	double ll_l = 0;
	double ll_r = 0;
	double ll_rr = 0;
	double l_ll = 0;
	double l_rr = 0;
	double u_rr = 0;
	double u_ll = 0;
	double uu_ll = 0;
	double uu_l = 0;
	double uu_r = 0;
	double uu_rr = 0;
	private double[] pixels;
	private int width = 0;

	public double[] resize(double[] pixels, int dstWidth, int dstHeight) {
		double width = Math.sqrt(pixels.length);
		double srcCenterX = 0 + width / 2.0;
		double srcCenterY = 0 + width / 2.0;
		double dstCenterX = dstWidth / 2.0;
		double dstCenterY = dstHeight / 2.0;
		double xScale = (double) dstWidth / width;
		double yScale = (double) dstHeight / width;

		boolean interpolate = true;
		if (interpolate) {
			dstCenterX += xScale / 2.0;
			dstCenterY += yScale / 2.0;
		}

		double[] pixels2 = new double[dstWidth * dstHeight];
		double xs, ys;
		double xlimit = width - 1.0, xlimit2 = width - 1.001;
		double ylimit = width - 1.0, ylimit2 = width - 1.001;
		int index1, index2;
		for (int y = 0; y <= dstHeight - 1; y++) {
			ys = (y - dstCenterY) / yScale + srcCenterY;
			if (interpolate) {
				if (ys < 0.0)
					ys = 0.0;
				if (ys >= ylimit)
					ys = ylimit2;
			}
			index1 = (int) (width * (int) ys);
			index2 = y * dstWidth;
			for (int x = 0; x <= dstWidth - 1; x++) {
				xs = (x - dstCenterX) / xScale + srcCenterX;
				if (interpolate) {
					if (xs < 0.0)
						xs = 0.0;
					if (xs >= xlimit)
						xs = xlimit2;
					pixels2[index2++] = (float) getInterpolatedPixel(xs, ys, pixels);
				} else
					pixels2[index2++] = pixels[index1 + (int) xs];
			}
		}
		return pixels2;
	}

	/** Uses cubic interpolation to find the pixel value at real coordinates (x,y). */
	public double getInterpolatedPixel(double x, double y) {
		if (x < 0.0)
			x = 0.0;
		if (x >= width - 1.0)
			x = width - 1.001;
		if (y < 0.0)
			y = 0.0;
		if (y >= width - 1.0)
			y = width - 1.001;
		return getInterpolatedPixel(x, y, pixels);
	}

	/** Uses bicubic interpolation to find the pixel value at real coordinates (x,y). */
	private final double getInterpolatedPixel(double x, double y, double[] pixels) {
		int xBase = (int) x;
		int yBase = (int) y;
		double xFraction = x - xBase;
		double yFraction = y - yBase;
		int offset = yBase * width + xBase;
		double ll = pixels[offset];
		double lr = pixels[offset + 1];
		double ur = pixels[offset + width + 1];
		double ul = pixels[offset + width];
		/*
		     UpperLeft, LowerRight, etc.
		     The base point is ll
		     xFraction and yFraction are >= 0 and < 1
		     It could happen that the two-index points, like l_ll, are off the image.
		     In this case they will be extrapolated from the inside points.

						x->

				ll_ll	ll_l	ll_r	ll_rr
		y		l_ll	ll		lr		l_rr
		|		u_ll	ul		ur		u_rr
		v		uu_ll	uu_l	uu_r	uu_rr

		*/
		//Initially extrapolate everybody to make sure something
		//happens
		ll_ll = 2 * ll - ur;
		ll_l = 2 * ll - ul;
		ll_r = 2 * lr - ur;
		ll_rr = 2 * lr - ul;
		l_rr = 2 * lr - ll;
		u_rr = 2 * ur - ul;
		uu_rr = 2 * ur - ll;
		uu_r = 2 * ur - lr;
		uu_l = 2 * ul - ll;
		uu_ll = 2 * ul - lr;
		u_ll = 2 * ul - ur;
		l_ll = 2 * ll - lr;
		//Look up real pixels if they are in the image.
		//Top row of matrix
		if (yBase > 0) {
			ll_l = pixels[offset - width];
			ll_r = pixels[offset + 1 - width];
			if (xBase > 0) {
				ll_ll = pixels[offset - width - 1];
			}
			if (xBase < (width - 2)) {
				ll_rr = pixels[offset + 2 - width];
			}
		}
		//Bottom row of matrix
		if (yBase < (width - 2)) {
			uu_l = pixels[offset + width + width];
			uu_r = pixels[offset + width + 1 + width];
			if (xBase > 0) {
				uu_ll = pixels[offset + width + width - 1];
			}
			if (xBase < (width - 2)) {
				uu_rr = pixels[offset + width + width + 2];
			}
		}
		//left column
		if (xBase > 0) {
			l_ll = pixels[offset - 1];
			u_ll = pixels[offset + width - 1];
		}
		//right column
		if (xBase < (width - 2)) {
			l_rr = pixels[offset + 2];
			u_rr = pixels[offset + width + 2];
		}
		//Interpolate each of the rows
		double lli = cubic(xFraction, ll_ll, ll_l, ll_r, ll_rr);
		double li = cubic(xFraction, l_ll, ll, lr, l_rr);
		double ui = cubic(xFraction, u_ll, ul, ur, u_rr);
		double uui = cubic(xFraction, uu_ll, uu_l, uu_r, uu_rr);
		//Interpolate in the vertical direction
		double result = cubic(yFraction, lli, li, ui, uui);
		return result;
	}

	//Four point cubic interpolation, where it is assumed that the evaluation
	//point is within the center interval of the three intervals.  Overshoot
	//is controlled by constraining the result to not differ too much from
	//linear interpolation using just the center points.  The amount of
	//allowed difference is determined by performing linear extrapolation
	//from the outside intervals.
	double cubic(double t, double fm1, double f0, double f1, double f2) {
		double dm1 = fm1 - f0;
		double d1 = f1 - f0;
		double d2 = f2 - f0;
		double b = (d1 + dm1) / 2;
		double d1_minus_b = d1 - b;
		double d2_minus_4b = d2 - 4 * b;
		double c = (d2_minus_4b - 2 * d1_minus_b) / 6;
		double a = d1_minus_b - c;
		double cubic = f0 + t * (a + t * (b + t * c));
		double linear = linInterp(t, f0, f1);
		double extrapLeft = linInterp(t + 1, fm1, f0);
		double extrapRight = linInterp(t - 1, f1, f2);
		//Overshoot control idea: the interpolated answer must be
		//between the linear interpolation result and whichever
		//linear extrapolation result is closer to the linear
		//interpolation result.  If the cubic result is not in this
		//interval, then the answer is either the linear or the closer-
		//to-linear extrapolated.
		double closerExtrap = extrapLeft;
		if (Math.abs(extrapRight - linear) < Math.abs(extrapLeft - linear)) {
			closerExtrap = extrapRight;
		}
		double upperBound = closerExtrap;
		double lowerBound = linear;
		if (upperBound < lowerBound) {
			upperBound = linear;
			lowerBound = closerExtrap;
		}
		if (cubic > upperBound) {
			return upperBound;
		} else if (cubic < lowerBound) {
			return lowerBound;
		}
		return cubic;
	}

	double linInterp(double t, double f0, double f1) {
		return (1 - t) * f0 + t * f1;
	}

	public static class Matrix {
		public static double[] divideByScalar(double[] vals, double scalar) {
			for (int i = 0; i < vals.length; i++) {
				vals[i] = vals[i] / scalar;
			}
			return vals;
		}

		public static double[] multiplyByScalar(double[] vals, double scalar) {
			for (int i = 0; i < vals.length; i++) {
				vals[i] = vals[i] * scalar;
			}
			return vals;
		}

		public static double[] multipleByArray(double[] vals, double[] multiplyBy) {
			for (int i = 0; i < vals.length; i++) {
				vals[i] = vals[i] * multiplyBy[i];
			}
			return vals;
		}

		public static double[] divideByArray(double[] vals, double[] divideBy) {
			for (int i = 0; i < vals.length; i++) {
				vals[i] = vals[i] / divideBy[i];
			}
			return vals;
		}

		public static double[] getArrayByRange(double[] vals, int xStart, int xEnd, int yStart, int yEnd, int width) {
			double[] valsToReturn = new double[(xEnd - xStart + 1) * (yEnd - yStart + 1)];
			int offset = 0;
			for (int i = yStart; i <= yEnd; i++) {
				for (int j = xStart; j <= xEnd; j++) {
					valsToReturn[offset++] = vals[j + i * width];
				}
			}
			return valsToReturn;
		}
	}

}
