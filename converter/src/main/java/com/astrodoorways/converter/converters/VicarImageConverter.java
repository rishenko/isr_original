package com.astrodoorways.converter.converters;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.vicar.cassini.BitweightCalibrator;
import com.astrodoorways.converter.vicar.cassini.CassiniDustRingCalibrator;
import com.astrodoorways.converter.vicar.cassini.DebiasCalibrator;
import com.astrodoorways.converter.vicar.cassini.DivideByFlatsCalibrator;
import com.astrodoorways.converter.vicar.cassini.Lut8to12BitCalibrator;
import com.google.common.io.Files;
import com.tomgibara.imageio.impl.tiff.TIFFLZWCompressor;
import com.tomgibara.imageio.tiff.TIFFCompressor;
import com.tomgibara.imageio.tiff.TIFFImageWriteParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

/**
 * Convert a Vicar/PDS formatted image into another image format.
 *
 * @author kmcabee
 */
public class VicarImageConverter {

    private final String inputFilePath;
    private final String writeDirectory;
    private final Metadata metadata;
    private final String format;
    private final Integer seqCount;
    private final MetadataProcessor metadataProcessor = new MetadataProcessor();
    private final List<String> outputtedFilePaths = new ArrayList<>();
    private final Logger logger = LoggerFactory.getLogger(VicarImageConverter.class);
    private IIOMetadata iioMetadata;

    /**
     * @param metadata       the metadata describing the conversion info for this image
     * @param seqCount       the sequence number of this image in the conversion pipeline
     * @param writeDirectory the directory to write the outgoing image to
     * @param format         the format of the output image
     */
    public VicarImageConverter(Metadata metadata, Integer seqCount, String writeDirectory, String format) {
        this.metadata = metadata;
        this.inputFilePath = metadata.getFileInfo().getFilePath();
        if (!writeDirectory.endsWith("/")) {
            writeDirectory += "/";
        }
        this.writeDirectory = writeDirectory;
        this.format = format;
        this.seqCount = seqCount;
    }

    /**
     * This performs the actual conversion of a VICAR based image, including any
     * necessary calibration, post-processing, and writing the output to the
     * filesystem.
     *
     * @throws IOException    issues with reading or writing image related files
     * @throws ParseException issues related to building the filename
     */
    public void convert() throws IOException, ParseException {
        // open file with list of paths
        final File file = new File(inputFilePath);
        if (!file.canRead()) {
            logger.error("file cannot be found {}", inputFilePath);
            throw new RuntimeException("file cannot be found " + inputFilePath);
        }

        logger.trace("reading in the image");
        BufferedImage inputImage = readImage(file);

        logger.trace("building iiometadata from image");
        final Map<String, String> valueMap = buildMetadataMap(iioMetadata);

        logger.trace("post processing and calibration of image");
        inputImage = postProcessImage(buildOutputImage(inputImage), valueMap);

        logger.trace("writing out image");
        writeImage(inputImage);
    }

    /**
     * Create a map of key value pairs based on the provided IIOMetadata.
     * @param iioMetadata   metadata used to build key/value map
     * @return              map of key/value pairs based on IIOMetadata
     */
    public Map<String,String> buildMetadataMap(IIOMetadata iioMetadata) {
        return metadataProcessor.process(iioMetadata);
    }

    private BufferedImage postProcessImage(BufferedImage image, Map<String, String> valueMap) throws IOException {
        if (valueMap.get(MetadataProcessor.MISSION).startsWith("CAS")
                && ApplicationProperties.hasProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR)) {
            logger.trace("calibrating");
            return calibrateCassiniImages(image);
        }
        return image;
    }

    /**
     * Build the initial output image, specifically color and sample models.
     *
     * @param inputImage input image
     * @return initial pass at output image
     */
    public BufferedImage buildOutputImage(BufferedImage inputImage) {
        final int width = inputImage.getWidth();
        final int height = inputImage.getHeight();

        final SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height,
                1, width, new int[1]);
        final ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false,
                false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);

        // build the output raster array and populate it
        double[] outputRasterArray = new double[width * height];
        outputRasterArray = inputImage.getRaster().getPixels(0, 0, width, height, outputRasterArray);

        final DataBuffer dataBuffer = new DataBufferDouble(outputRasterArray, outputRasterArray.length);
        final WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        return new BufferedImage(colorModel, raster, false, null);
    }

    /**
     * Perform all Cassini related image calibrations.
     *
     * @param image the image to be calibrated
     * @return a calibrated version of the image
     * @throws IOException issues related to reading calibration related files
     */
    public BufferedImage calibrateCassiniImages(BufferedImage image) throws IOException {
        final int width = image.getWidth();
        final int height = image.getHeight();

        logger.trace("getting cassini calibration directory");
        final String cassCalibDir = ApplicationProperties.getPropertyAsString("cassini.calibration.dir");

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
            logger.trace("{} has been CALIBRATED", inputFilePath);

            boolean needsNormalized = Arrays.stream(rasterArray).anyMatch((val) -> val > 1.0d);
            if (needsNormalized) {
                double bitDivisor = isCalibratedLut ? 1 << 12 : 1 << 16;
                for (int i = 0; i < rasterArray.length; i++) {
                    rasterArray[i] = rasterArray[i] / bitDivisor;
                }
                logger.trace("{} has been NORMALIZED with bit depth of {}", new Object[]{inputFilePath, bitDivisor});
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

    /**
     * @return a path to the file with all necessary directories created
     * @throws ParseException issues related to building the filename from metadata
     */
    public String generateOutputFilePath(int w, int h) throws ParseException {
        final String sep = "/";
        StringBuilder outputtedFilePath = new StringBuilder(writeDirectory);
        if (seqCount == null) {
            outputtedFilePath.append(metadataProcessor.getFileStructure())
                    .append(sep)
                    .append(metadataProcessor.getFileName());
        } else {
            buildSequenceOutputFilePath(sep, outputtedFilePath);
        }

        outputtedFilePath.append("w").append(w)
                .append("-h").append(h)
                .append(".").append(format);

        try {
            Files.createParentDirs(new File(outputtedFilePath.toString()));
        } catch (IOException e) {
            logger.error("problem creating directory for " + outputtedFilePath, e);
            throw new RuntimeException("problem creating directory for " + outputtedFilePath, e);
        }

        return outputtedFilePath.toString();
    }

    /**
     * Build output file path for an image being converted in a sequence of images.
     * @param sep               the separator for the system's file structure
     * @param outputtedFilePath the output file path for the image
     */
    private void buildSequenceOutputFilePath(String sep, StringBuilder outputtedFilePath) {
        // BUILD DIRECTORY
        outputtedFilePath.append(metadata.getMission().replace("/", "-"))
            .append("_")
            .append(metadata.getTarget())
            .append("_")
            .append(metadata.getFilterOne());

        // not all missions have a secondary filter for the cameras
        if (metadata.getFilterTwo() != null)
            outputtedFilePath.append("-").append(metadata.getFilterTwo());

        // BUILD FILENAME
        outputtedFilePath.append(sep);
        outputtedFilePath.append(metadata.getMission().replace("/", "-")).append("_")
                .append(metadata.getTarget())
                .append("_")
                .append(metadata.getFilterOne());

        // not all missions have a seondary filter for the cameras
        if (metadata.getFilterTwo() != null)
            outputtedFilePath.append(metadata.getFilterTwo());

        String sequence = seqCount.toString();
        sequence = "0000000000".substring(sequence.length()) + sequence;
        outputtedFilePath.append("_").append(sequence);
    }

    /**
     * Write the output image to the filesystem.
     *
     * @param image             the finalized image to be written
     * @throws IOException issues related to writing the image file
     */
    public void writeImage(BufferedImage image) throws IOException, ParseException {
        // write out image
        final int width = image.getWidth();
        final int height = image.getHeight();
        final String outputtedFilePath = generateOutputFilePath(width, height);
        logger.trace("generating output path");

        // the image has already been created
        if (new File(outputtedFilePath).exists()) {
            throw new IllegalStateException("image already exists: " + outputtedFilePath);
        }
        final File output = new File(outputtedFilePath);

        // get the writer
        final Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new IllegalStateException("could not find a writer, made no changes and do not attempt to convert the image: " + outputtedFilePath);
        }

        ImageOutputStream stream = null;
        final ImageWriter writer = writers.next();
        try {
            boolean isDeleted = output.delete();
            if (isDeleted) logger.trace("file deleted in prep for writing: {}", outputtedFilePath);
            Files.createParentDirs(output);
            logger.trace("writing the converted image to {}", outputtedFilePath);
            boolean isCreated = output.createNewFile();
            if (!isCreated) throw new IOException("couldn't create file for writing image: " + outputtedFilePath);
            stream = ImageIO.createImageOutputStream(output);

            // setting the byte order to be the more standard PC format
            stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
            doWrite(image, writer, stream);

            outputtedFilePaths.add(outputtedFilePath);
        } catch (IOException e) {
            logger.error("image writing failed", e);
            throw e;
        } finally {
            if (stream != null) {
                stream.flush();
                writer.dispose();
                stream.close();
            }
        }
    }

    /**
     * Writes image to output stream using given image writer.
     */
    private void doWrite(RenderedImage im, ImageWriter writer, ImageOutputStream output) throws IOException {
        writer.setOutput(output);
        final TIFFImageWriteParam imageWriteParam = new TIFFImageWriteParam(null);
        final TIFFCompressor compressor = new TIFFLZWCompressor(0);
        imageWriteParam.setCompressionMode(TIFFImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setTIFFCompressor(compressor);
        imageWriteParam.setCompressionType(compressor.getCompressionType());
        writer.write(iioMetadata, new IIOImage(im, null, iioMetadata), imageWriteParam);
    }

    /**
     * Read an image data and metadata.
     *
     * @param file the image to be read
     * @return the read image
     * @throws IOException any issues related to reading the image
     */
    public BufferedImage readImage(File file) throws IOException {
        final Iterator<ImageReader> readers = ImageIO.getImageReaders(ImageIO.createImageInputStream(file));
        if (!readers.hasNext()) {
            logger.error("no image reader was found for {}", inputFilePath);
            throw new RuntimeException("no image reader was found for " + inputFilePath);
        }

        ImageReader reader = null;
        BufferedImage image;
        try {
            reader = readers.next();
            reader.setInput(ImageIO.createImageInputStream(file));
            image = reader.read(0);
            iioMetadata = reader.getImageMetadata(0);
        } catch (IOException e) {
            throw e;
        } finally {
            if (reader != null) reader.dispose();
        }

        return image;
    }

    /**
     * @return the original image's metadata
     */
    public IIOMetadata getIIOMetaData() {
        return iioMetadata;
    }

    /**
     * @return the path to the finalized output image
     */
    public List<String> getOutputtedFilePaths() {
        return outputtedFilePaths;
    }
}
