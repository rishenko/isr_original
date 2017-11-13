package com.astrodoorways.converter.vicar.cassini;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.metadata.IIOMetadata;

public class BitweightCalibrator extends AbstractBaseCalibrator {

	private final String calibrationDirectory;

	public BitweightCalibrator(String calibrationDirectory) {
		this.calibrationDirectory = calibrationDirectory;
	}

	@Override
	public boolean calibrate(double[] imageArray, IIOMetadata metadata) throws IOException {
		String nodeString = getNodeString(metadata);
		String instrument = extractValue("INSTRUMENT_ID", nodeString);
		String encType = extractValue("DATA_CONVERSION_TYPE", nodeString);

		if (encType.equals("TABLE") || encType.equals("LOSSY"))
			return false;

		if (instrument.equals("ISSNA"))
			instrument = "NA";
		else if (instrument.equals("ISSWA"))
			instrument = "WA";
		else
			return false;

		double[] calTemps = new double[] { -10, 5, 25 };
		String opticsTemp = extractValue("OPTICS_TEMPERATURE", nodeString);
		opticsTemp = opticsTemp.substring(0, opticsTemp.length() - 1).split(",")[0];
		Double opticsTempDbl = Double.parseDouble(opticsTemp);

		double[] tempDiffs = new double[calTemps.length];
		for (int i = 0; i < calTemps.length; i++) {
			tempDiffs[i] = Math.abs(calTemps[i] - opticsTempDbl);
		}

		Arrays.sort(tempDiffs);
		double useTemp = tempDiffs[0];

		//selecting temp range

		String fname = "wac";
		if (instrument.equals("NA"))
			fname = "nac";

		String gainModeId = extractValue("GAIN_MODE_ID", nodeString);
		int gainState = 0;
		switch (gainModeId) {
		case "1400K":
		case "215 e/DN":
		case "215 ELECTRONS PER DN":
			gainState = 0;
			break;
		case "400K":
		case "95 e/DN":
		case "95 ELECTRONS PER DN":
			gainState = 1;
			break;
		case "100K":
		case "29 e/DN":
		case "29 ELECTRONS PER DN":
			gainState = 2;
			break;
		case "40K":
		case "12 e/DN":
		case "12 ELECTRONS PER DN":
			gainState = 3;
			break;
		default:
			return false;
		}

		fname += "g" + gainState;
		if (opticsTempDbl < -5.0) {
			fname += "m10";
		} else if (opticsTempDbl < 25.0) {
			fname += "p5";
		} else {
			fname += "p25";
		}

		fname += "_bwt.tab";

		// build bitweight table;
		File btwFile = new File(calibrationDirectory + "/calib/bitweight/" + fname);
		BufferedReader reader = new BufferedReader(new FileReader(btwFile));
		String line = "";
		boolean markFound = false;
		List<Double> bitweightData = new ArrayList<>();
		while ((line = reader.readLine()) != null) {
			if (markFound == false && !line.trim().equals("\\begindata")) {
				continue;
			} else if (markFound == false && line.trim().equals("\\begindata")) {
				markFound = true;
				continue;
			}
			String[] values = null;
			if (line.indexOf(",") != -1)
				values = line.trim().split(",");
			else
				values = line.trim().split(" ");
			for (String strVal : values) {
				bitweightData.add(Double.parseDouble(strVal));
			}
		}
		reader.close();

		for (int i = 0; i < imageArray.length; i++) {
			imageArray[i] = bitweightData.get((int) imageArray[i]);
		}

		return true;
	}
}
