package com.astrodoorways.converter.converters;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.mission.MissionPlugin;
import com.astrodoorways.converter.vicar.exif.TiffExifMetadata;
import com.google.common.io.Files;
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
import javax.imageio.ImageWriteParam;
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
    private boolean replaceExisting;

    /** Overwrite an existing output file instead of throwing (used when resuming a job). */
    public void setReplaceExisting(boolean replaceExisting) {
        this.replaceExisting = replaceExisting;
    }

    /**
     * @param metadata       the metadata describing the conversion info for this image
     * @param seqCount       the image's sequence number within its target and filter (sequence runs only), or null
     * @param writeDirectory the directory to write the outgoing image to
     * @param format         the format of the output image
     */
    public VicarImageConverter(Metadata metadata, Integer seqCount, String writeDirectory, String format) {
        this.metadata = metadata;
        // read the funpack intermediary when there is one (see ConverterImpl.preProcessImage)
        String preprocessed = metadata.getFileInfo().getPreprocessedFileName();
        this.inputFilePath = preprocessed != null ? preprocessed : metadata.getFileInfo().getFilePath();
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

    /** Mission-specific processing (e.g. Cassini calibration), done by the mission's plugin. */
    private BufferedImage postProcessImage(BufferedImage image, Map<String, String> valueMap) throws IOException {
        MissionPlugin mission = metadataProcessor.getMission();
        logger.trace("{} processing for {}", mission.name(), inputFilePath);
        return mission.process(image, iioMetadata, valueMap);
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
     * @return a path to the file with all necessary directories created
     * @throws ParseException issues related to building the filename from metadata
     */
    public String generateOutputFilePath(int w, int h) throws ParseException {
        final String sep = "/";
        String fileStructure = metadataProcessor.getFileStructure();
        String fileName = seqCount == null ? metadataProcessor.getFileName() : metadataProcessor.getFileName(seqCount);
        // images from an unrecognised mission have no structure or name (they were written as
        // "null/null...", so a second one failed with "image already exists"); fall back to the
        // input file's own name in an "unknown" folder
        if (fileStructure == null)
            fileStructure = UNKNOWN_FOLDER;
        if (fileName == null)
            fileName = inputBaseName();

        StringBuilder outputtedFilePath = new StringBuilder(writeDirectory);
        outputtedFilePath.append(fileStructure)
                .append(sep)
                .append(fileName);

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

    /** Output folder for images whose mission isn't recognised. */
    public static final String UNKNOWN_FOLDER = "unknown";

    private String inputBaseName() {
        String name = metadata.getFileInfo().getFileName();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
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
        if (new File(outputtedFilePath).exists() && !replaceExisting) {
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
     * Writes image to output stream using the JDK's TIFF writer: LZW compression, no predictor,
     * with the EXIF tags (see TiffExifMetadata) written in the same pass.
     */
    private void doWrite(RenderedImage im, ImageWriter writer, ImageOutputStream output) throws IOException {
        writer.setOutput(output);
        final ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();
        imageWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        imageWriteParam.setCompressionType("LZW");
        final IIOMetadata tiffMetadata = TiffExifMetadata.build(writer, im, imageWriteParam, iioMetadata);
        writer.write(null, new IIOImage(im, null, tiffMetadata), imageWriteParam);
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
