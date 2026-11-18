package com.astrodoorways.converter.mission.cassini;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.mission.MissionPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import javax.imageio.metadata.IIOMetadata;

/**
 * Cassini ISS: label handling plus the calibration chain (a partial port of CISSCAL), which runs
 * when cassini.calibration.dir is set.
 */
public class CassiniMission implements MissionPlugin {

    private final Logger logger = LoggerFactory.getLogger(CassiniMission.class);

    @Override
    public String name() {
        return "Cassini";
    }

    @Override
    public MetadataHandler newHandler() {
        return new CassiniHandler();
    }

    @Override
    public BufferedImage process(BufferedImage image, IIOMetadata iioMetadata, Map<String, String> labelValues)
            throws IOException {
        if (labelValues.get(MetadataProcessor.MISSION).startsWith("CAS")
                && ApplicationProperties.hasProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR)) {
            logger.trace("calibrating");
            return calibrate(image, iioMetadata);
        }
        return image;
    }

    /**
     * Perform all Cassini related image calibrations.
     *
     * @param image the image to be calibrated
     * @return a calibrated version of the image
     * @throws IOException issues related to reading calibration related files
     */
    public BufferedImage calibrate(BufferedImage image, IIOMetadata iioMetadata) throws IOException {
        final int width = image.getWidth();
        final int height = image.getHeight();

        logger.trace("getting cassini calibration directory");
        final String cassCalibDir = ApplicationProperties.getPropertyAsString(ApplicationProperties.CASSINI_CALIBRATION_DIR);

        final double[] rasterArray = image.getRaster().getPixels(0, 0, width, height, new double[width * height]);

        boolean isCalibrated;
        logger.trace("lut 8to12 calibration");
        boolean isCalibratedLut = isCalibrated = new Lut8to12BitCalibrator().calibrate(rasterArray, iioMetadata);

        logger.trace("bitweight calibration");
        isCalibrated = isCalibrated | new BitweightCalibrator(cassCalibDir).calibrate(rasterArray, iioMetadata);

        logger.trace("debias calibration");
        isCalibrated = isCalibrated | new DebiasCalibrator().calibrate(rasterArray, iioMetadata);

        logger.trace("dust calibration");
        isCalibrated = isCalibrated | new CassiniDustRingCalibrator(cassCalibDir).calibrate(rasterArray, iioMetadata);

        logger.trace("divide by flats calibration");
        isCalibrated = isCalibrated | new DivideByFlatsCalibrator(cassCalibDir).calibrate(rasterArray, iioMetadata);

        return buildImagePostCalibration(width, height, rasterArray, isCalibrated, isCalibratedLut);
    }

    private BufferedImage buildImagePostCalibration(int width, int height, double[] rasterArray, boolean isCalibrated, boolean isCalibratedLut) {
        BufferedImage imageNew;
        if (isCalibrated) {
            logger.trace("image has been CALIBRATED");

            boolean needsNormalized = Arrays.stream(rasterArray).anyMatch((val) -> val > 1.0d);
            if (needsNormalized) {
                double bitDivisor = isCalibratedLut ? 1 << 12 : 1 << 16;
                for (int i = 0; i < rasterArray.length; i++) {
                    rasterArray[i] = rasterArray[i] / bitDivisor;
                }
                logger.trace("image has been NORMALIZED with bit depth of {}", bitDivisor);
            }

            final DataBuffer dataBuffer = new DataBufferDouble(rasterArray, rasterArray.length);
            final SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1, width,
                    new int[1]);
            final WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
            final ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
                    Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
            imageNew = new BufferedImage(colorModel, raster, false, null);

        } else {
            imageNew = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
            imageNew.getRaster().setPixels(0, 0, width, height, rasterArray);
        }
        return imageNew;
    }

}
