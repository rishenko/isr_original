package com.astrodoorways.converter.vicar.cassini;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.metadata.IIOMetadata;

import org.w3c.dom.Node;

/**
 * @author kmcabee
 */

public class CassiniDustRingCalibrator extends AbstractBaseCalibrator {

	private final String calibrationDirectory;

	public CassiniDustRingCalibrator(String calibrationDirectory) {
		this.calibrationDirectory = calibrationDirectory;
	}

	public boolean calibrate(double[] imageArray, IIOMetadata metadata) throws IOException {
		Node node = metadata.getAsTree(METADATAFORMAT);
		String nodeString = node.getLastChild().getLastChild().getAttributes().item(2).getNodeValue();

		String instrument = "ISSNA";
		if (!instrument.equals("ISSNA")) {
			return false;
		}

		String sum = extractValue("INSTRUMENT_MODE_ID", nodeString);
		if (sum.equals("SUM2")) {
			//			if sum eq 'SUM2' then dustCorr = rebin(dustCorr, 512,512)			
		} else if (sum.equals("SUM4")) {
			//			if sum eq 'SUM4' then dustCorr = rebin(dustCorr, 256,256)			
		}

		removeDust(imageArray, nodeString, sum);
		processMottling(imageArray, nodeString, sum);

		return true;
	}

	public boolean removeDust(double[] imageArray, String nodeString, String sum) throws IOException {
		File file = new File(calibrationDirectory + "/calib/dustring/nac_dustring_venus.img");
		double[] dustCorrArray = readImageAsDoubleArray(file);

		for (int i = 0; i < imageArray.length; i++) {
			double dustCorrPix = dustCorrArray[i];
			double imagePix = imageArray[i];
			double modifiedVal = dustCorrPix * imagePix;

			imageArray[i] = (double) Math.round(modifiedVal);
		}
		return true;
	}

	private boolean processMottling(double[] imageArray, String nodeString, String sum) throws IOException {
		// HANDLE IMAGE MOTTLING
		Integer imageNumber = Integer.parseInt(extractValue("IMAGE_NUMBER", nodeString));
		if (sum.equals("FULL") && (imageNumber > 1455892746)) {

			String filter = processFilter(nodeString);
			String filters[] = filter.split("-");

			if (filters[0].equals("CL1") && filters[1].equals("CL2")) {
				filters[0] = "CLR";
			}

			List<String> possibleFilters = Arrays.asList("UV1", "UV2", "UV3", "BL1", "BL2", "GRN", "RED", "IR1", "IR2",
					"IR3", "IR4", "CB1", "CB2", "CB3", "MT1", "MT2", "MT3", "CLR");
			double[] sfacts = new double[] { 1.199, 1.186, 1.069, 1.00, 0.833, 0.890, 0.843, 0.997, 0.897, 0.505,
					0.780, 0.764, 0.781, 0.608, 0.789, 0.722, 0.546, 0.763 };

			double sfact = 0.0;
			if (!possibleFilters.containsAll(Arrays.asList(filters[0], filters[1]))) {
				if (possibleFilters.contains(filters[0])) {
					sfact = sfacts[possibleFilters.indexOf(filters[0])];
				} else if (possibleFilters.contains(filters[1])) {
					sfact = sfacts[possibleFilters.indexOf(filters[1])];
				}

			} else {
				EffwlHolder effwl = null;
				EffwlHolder effwlLookup = new EffwlHolder(filters[0], filters[1], 0.0, 0.0, 0.0, 0.0);
				List<EffwlHolder> lookupTable = buildLookupTable();
				if (lookupTable.contains(effwlLookup)) {
					effwl = lookupTable.get(lookupTable.indexOf(effwlLookup));
				}
				if (effwl != null) {
					sfact = 1.30280 - 0.000717552 * effwl.getEffectWavelength();
				}

			}

			double[] mottleArray = getMottleFileArray();
			for (int i = 0; i < imageArray.length; i++) {
				double imageArrayPix = imageArray[i];
				double mottleArrayPix = mottleArray[i];
				double val = imageArrayPix * (1.0 - sfact * mottleArrayPix / 1000.0);
				imageArray[i] = (double) Math.round(val);
			}
		}
		return true;
	}

	private static double[] mottleArray;

	public synchronized double[] getMottleFileArray() throws IOException {
		if (mottleArray == null) {
			String mottlefile = calibrationDirectory + "/calib/dustring/nac_mottle_1444733393.tif";
			mottleArray = readImageAsDoubleArray(new File(mottlefile));
		}
		return mottleArray;
	}

	public static List<EffwlHolder> buildLookupTable() {
		List<EffwlHolder> effwlList = new ArrayList<EffwlHolder>();

		effwlList.add(new EffwlHolder("CL1", "CL2", 610.67491, 340.056, 651.05671, 305.975));
		effwlList.add(new EffwlHolder("CL1", "GRN", 568.13358, 113.019, 569.23560, 109.947));
		effwlList.add(new EffwlHolder("CL1", "UV3", 338.28364, 68.0616, 343.13612, 53.1785));
		effwlList.add(new EffwlHolder("CL1", "BL2", 439.92261, 29.4692, 440.97984, 28.8250));
		effwlList.add(new EffwlHolder("CL1", "MT2", 727.42138, 4.11240, 727.41454, 4.11484));
		effwlList.add(new EffwlHolder("CL1", "CB2", 750.50505, 10.0129, 750.49504, 9.99648));
		effwlList.add(new EffwlHolder("CL1", "MT3", 889.19378, 10.4720, 889.19622, 10.4463));
		effwlList.add(new EffwlHolder("CL1", "CB3", 937.96440, 9.54761, 937.92783, 9.49512));
		effwlList.add(new EffwlHolder("CL1", "MT1", 618.94517, 3.68940, 618.94905, 3.66875));
		effwlList.add(new EffwlHolder("CL1", "CB1", 619.38053, 9.99526, 619.29245, 9.98508));
		effwlList.add(new EffwlHolder("CL1", "CB1A", 602.90783, 9.99526, 602.91700, 9.98508));
		effwlList.add(new EffwlHolder("CL1", "CB1B", 634.53144, 11.9658, 634.52648, 11.9666));
		effwlList.add(new EffwlHolder("CL1", "IR3", 929.76270, 66.9995, 928.30424, 64.0053));
		effwlList.add(new EffwlHolder("CL1", "IR1", 751.89445, 152.929, 750.04823, 143.919));
		effwlList.add(new EffwlHolder("RED", "CL2", 650.08572, 149.998, 648.87862, 149.979));
		effwlList.add(new EffwlHolder("RED", "GRN", 601.03171, 51.9801, 600.95891, 51.9769));
		effwlList.add(new EffwlHolder("RED", "MT2", 726.63326, 2.33906, 726.62372, 2.33613));
		effwlList.add(new EffwlHolder("RED", "CB2", 744.25482, 4.22393, 743.91206, 4.21616));
		effwlList.add(new EffwlHolder("RED", "MT1", 618.91104, 3.69858, 618.92155, 3.67744));
		effwlList.add(new EffwlHolder("RED", "CB1", 619.56767, 9.07488, 619.48074, 9.06626));
		effwlList.add(new EffwlHolder("RED", "IR3", 695.43499, 2.04887, 695.03967, 2.04780));
		effwlList.add(new EffwlHolder("RED", "IR1", 701.89980, 44.9603, 701.69196, 44.9653));
		effwlList.add(new EffwlHolder("BL1", "CL2", 450.85092, 102.996, 455.47051, 59.7593));
		effwlList.add(new EffwlHolder("BL1", "GRN", 497.44473, 5.00811, 497.43474, 4.43315));
		effwlList.add(new EffwlHolder("BL1", "UV3", 386.57073, 14.0295, 389.21982, 8.80547));
		effwlList.add(new EffwlHolder("BL1", "BL2", 440.03493, 29.6733, 441.07744, 23.8730));
		effwlList.add(new EffwlHolder("UV2", "CL2", 297.88030, 59.9535, 306.47710, 44.0266));
		effwlList.add(new EffwlHolder("UV2", "UV3", 315.62265, 28.9282, 317.60874, 27.9740));
		effwlList.add(new EffwlHolder("UV1", "CL2", 258.09836, 37.9542, 266.32112, 11.3965));
		effwlList.add(new EffwlHolder("UV1", "UV3", 350.69714, 9.07263, 353.87802, 8.96403));
		effwlList.add(new EffwlHolder("IRPO", "MT2", 727.43452, 4.11241, 727.42444, 4.11486));
		effwlList.add(new EffwlHolder("IRPO", "CB2", 750.51286, 10.0158, 750.50160, 9.99934));
		effwlList.add(new EffwlHolder("IRPO", "MT3", 889.21167, 10.4738, 889.20854, 10.4476));
		effwlList.add(new EffwlHolder("IRPO", "CB3", 938.00197, 9.54946, 937.96138, 9.49780));
		effwlList.add(new EffwlHolder("IRPO", "MT1", 618.97033, 3.69682, 618.96789, 3.67585));
		effwlList.add(new EffwlHolder("IRPO", "IR3", 930.04757, 67.9802, 928.58325, 65.0057));
		effwlList.add(new EffwlHolder("IRPO", "IR1", 752.82226, 153.994, 750.96799, 150.937));
		effwlList.add(new EffwlHolder("P120", "GRN", 568.53234, 112.946, 569.63066, 109.708));
		effwlList.add(new EffwlHolder("P120", "UV3", 341.10148, 66.0391, 345.49268, 48.1757));
		effwlList.add(new EffwlHolder("P120", "BL2", 440.02292, 29.4620, 441.07917, 28.8083));
		effwlList.add(new EffwlHolder("P120", "MT2", 727.43081, 4.11216, 727.42147, 4.11455));
		effwlList.add(new EffwlHolder("P120", "CB2", 750.53574, 10.0307, 750.52474, 10.0229));
		effwlList.add(new EffwlHolder("P120", "MT1", 618.90876, 3.69299, 618.92016, 3.67217));
		effwlList.add(new EffwlHolder("P120", "CB1", 619.96103, 9.99561, 619.87249, 9.98545));
		effwlList.add(new EffwlHolder("P60", "GRN", 568.53234, 112.946, 569.63066, 109.708));
		effwlList.add(new EffwlHolder("P60", "UV3", 341.10148, 66.0391, 345.49268, 48.1757));
		effwlList.add(new EffwlHolder("P60", "BL2", 440.02292, 29.4620, 441.07917, 28.8083));
		effwlList.add(new EffwlHolder("P60", "MT2", 727.43081, 4.11216, 727.42147, 4.11455));
		effwlList.add(new EffwlHolder("P60", "CB2", 750.53574, 10.0307, 750.52474, 10.0229));
		effwlList.add(new EffwlHolder("P60", "MT1", 618.90876, 3.69299, 618.92016, 3.67217));
		effwlList.add(new EffwlHolder("P60", "CB1", 619.96103, 9.99561, 619.87249, 9.98545));
		effwlList.add(new EffwlHolder("P0", "GRN", 568.53234, 112.946, 569.63066, 109.708));
		effwlList.add(new EffwlHolder("P0", "UV3", 341.10148, 66.0391, 345.49268, 48.1757));
		effwlList.add(new EffwlHolder("P0", "BL2", 440.02292, 29.4620, 441.07917, 28.8083));
		effwlList.add(new EffwlHolder("P0", "MT2", 727.43081, 4.11216, 727.42147, 4.11455));
		effwlList.add(new EffwlHolder("P0", "CB2", 750.53574, 10.0307, 750.52474, 10.0229));
		effwlList.add(new EffwlHolder("P0", "MT1", 618.90876, 3.69299, 618.92016, 3.67217));
		effwlList.add(new EffwlHolder("P0", "CB1", 619.96103, 9.99561, 619.87249, 9.98545));
		effwlList.add(new EffwlHolder("HAL", "CL2", 655.66262, 9.26470, 655.62131, 9.28066));
		effwlList.add(new EffwlHolder("HAL", "GRN", 648.02819, 5.58862, 647.80801, 4.60278));
		effwlList.add(new EffwlHolder("HAL", "CB1", 650.56667, 2.73589, 650.46557, 2.72520));
		effwlList.add(new EffwlHolder("HAL", "IR1", 663.47576, 5.25757, 663.43127, 4.30806));
		effwlList.add(new EffwlHolder("IR4", "CL2", 1002.3954, 35.9966, 1001.9070, 34.9802));
		effwlList.add(new EffwlHolder("IR4", "IR3", 996.72313, 36.0700, 996.45963, 35.0352));
		effwlList.add(new EffwlHolder("IR2", "CL2", 861.96189, 97.0431, 861.06574, 92.0721));
		effwlList.add(new EffwlHolder("IR2", "MT3", 889.17616, 10.4655, 889.17613, 10.4496));
		effwlList.add(new EffwlHolder("IR2", "CB3", 933.65654, 3.71709, 933.59346, 3.71802));
		effwlList.add(new EffwlHolder("IR2", "IR3", 901.84315, 44.0356, 901.63028, 44.0031));
		effwlList.add(new EffwlHolder("IR2", "IR1", 827.43827, 28.0430, 827.33120, 28.0490));

		return effwlList;

	}

	public static class EffwlHolder {
		private final String filter1;
		private final String filter2;
		private final double centralWavelength;
		private final double cwfullWidthHalfMax;
		private final double effectWavelength;
		private final double ewFullWidthHalfMax;

		public EffwlHolder(String filter1, String filter2, double cwl, double cwfwhm, double ewl, double ewfwhm) {
			this.filter1 = filter1;
			this.filter2 = filter2;
			this.centralWavelength = cwl;
			this.cwfullWidthHalfMax = cwfwhm;
			this.effectWavelength = ewl;
			this.ewFullWidthHalfMax = ewfwhm;
		}

		public String getFilter1() {
			return filter1;
		}

		public String getFilter2() {
			return filter2;
		}

		public double getCentralWavelength() {
			return centralWavelength;
		}

		public double getCwfullWidthHalfMax() {
			return cwfullWidthHalfMax;
		}

		public double getEffectWavelength() {
			return effectWavelength;
		}

		public double getEwFullWidthHalfMax() {
			return ewFullWidthHalfMax;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof EffwlHolder))
				return false;
			EffwlHolder holder = (EffwlHolder) obj;
			return this.filter1.equals(holder.getFilter1()) && this.filter2.equals(holder.getFilter2());
		}
	}
}
