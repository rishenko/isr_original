//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.astrodoorways.converter.vicar.cassini;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.imageio.metadata.IIOMetadata;

public class BitweightCalibrator extends AbstractBaseCalibrator {
    private final String calibrationDirectory;

    public BitweightCalibrator(String calibrationDirectory) {
        this.calibrationDirectory = calibrationDirectory;
    }

    public boolean calibrate(double[] imageArray, IIOMetadata metadata) throws IOException {
        String nodeString = this.getNodeString(metadata);
        String instrument = extractValue("INSTRUMENT_ID", nodeString);
        String encType = extractValue("DATA_CONVERSION_TYPE", nodeString);
        if (!encType.equals("TABLE") && !encType.equals("LOSSY")) {
            if (instrument.equals("ISSNA")) {
                instrument = "NA";
            } else {
                if (!instrument.equals("ISSWA")) {
                    return false;
                }

                instrument = "WA";
            }

            double[] calTemps = new double[]{-10.0D, 5.0D, 25.0D};
            String opticsTemp = extractValue("OPTICS_TEMPERATURE", nodeString);
            opticsTemp = opticsTemp.substring(0, opticsTemp.length() - 1).split(",")[0];
            Double opticsTempDbl = Double.parseDouble(opticsTemp);
            double[] tempDiffs = new double[calTemps.length];

            for (int i = 0; i < calTemps.length; ++i) {
                tempDiffs[i] = Math.abs(calTemps[i] - opticsTempDbl);
            }

            Arrays.sort(tempDiffs);
            double useTemp = tempDiffs[0];
            String fname = "wac";
            if (instrument.equals("NA")) {
                fname = "nac";
            }

            String gainModeId = extractValue("GAIN_MODE_ID", nodeString);
            int gainState = 0;
            byte var16 = -1;
            switch (gainModeId.hashCode()) {
                case -1204985683:
                    if (gainModeId.equals("29 e/DN")) {
                        var16 = 7;
                    }
                    break;
                case -965835138:
                    if (gainModeId.equals("215 e/DN")) {
                        var16 = 1;
                    }
                    break;
                case -948139034:
                    if (gainModeId.equals("215 ELECTRONS PER DN")) {
                        var16 = 2;
                    }
                    break;
                case -147604640:
                    if (gainModeId.equals("95 ELECTRONS PER DN")) {
                        var16 = 5;
                    }
                    break;
                case 51535:
                    if (gainModeId.equals("40K")) {
                        var16 = 9;
                    }
                    break;
                case 1507450:
                    if (gainModeId.equals("100K")) {
                        var16 = 6;
                    }
                    break;
                case 1596823:
                    if (gainModeId.equals("400K")) {
                        var16 = 3;
                    }
                    break;
                case 46849352:
                    if (gainModeId.equals("1400K")) {
                        var16 = 0;
                    }
                    break;
                case 598056184:
                    if (gainModeId.equals("95 e/DN")) {
                        var16 = 4;
                    }
                    break;
                case 1009452795:
                    if (gainModeId.equals("12 ELECTRONS PER DN")) {
                        var16 = 11;
                    }
                    break;
                case 1775409045:
                    if (gainModeId.equals("29 ELECTRONS PER DN")) {
                        var16 = 8;
                    }
                    break;
                case 2002073875:
                    if (gainModeId.equals("12 e/DN")) {
                        var16 = 10;
                    }
            }

            switch (var16) {
                case 0:
                case 1:
                case 2:
                    gainState = 0;
                    break;
                case 3:
                case 4:
                case 5:
                    gainState = 1;
                    break;
                case 6:
                case 7:
                case 8:
                    gainState = 2;
                    break;
                case 9:
                case 10:
                case 11:
                    gainState = 3;
                    break;
                default:
                    return false;
            }

            fname = fname + "g" + gainState;
            if (opticsTempDbl < -5.0D) {
                fname = fname + "m10";
            } else if (opticsTempDbl < 25.0D) {
                fname = fname + "p5";
            } else {
                fname = fname + "p25";
            }

            fname = fname + "_bwt.tab";
            File btwFile = new File(this.calibrationDirectory + "/calib/bitweight/" + fname);
            BufferedReader reader = new BufferedReader(new FileReader(btwFile));
            String line = "";
            boolean markFound = false;
            ArrayList bitweightData = new ArrayList();

            while (true) {
                while (true) {
                    do {
                        if ((line = reader.readLine()) == null) {
                            reader.close();

                            for (int i = 0; i < imageArray.length; ++i) {
                                imageArray[i] = (Double) bitweightData.get((int) imageArray[i]);
                            }

                            return true;
                        }
                    } while (!markFound && !line.trim().equals("\\begindata"));

                    if (!markFound && line.trim().equals("\\begindata")) {
                        markFound = true;
                    } else {
                        String[] values = null;
                        if (line.indexOf(",") != -1) {
                            values = line.trim().split(",");
                        } else {
                            values = line.trim().split(" ");
                        }

                        String[] arr$ = values;
                        int len$ = values.length;

                        for (int i$ = 0; i$ < len$; ++i$) {
                            String strVal = arr$[i$];
                            bitweightData.add(Double.parseDouble(strVal));
                        }
                    }
                }
            }
        } else {
            return false;
        }
    }
}
