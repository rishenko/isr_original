//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.astrodoorways.converter.converters;

import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.vicar.cassini.BitweightCalibrator;
import com.astrodoorways.converter.vicar.cassini.CassiniDustRingCalibrator;
import com.astrodoorways.converter.vicar.cassini.DebiasCalibrator;
import com.astrodoorways.converter.vicar.cassini.DivideByFlatsCalibrator;
import com.astrodoorways.converter.vicar.cassini.Lut8to12BitCalibrator;
import com.google.common.io.Files;
import com.sun.media.jai.codec.ImageCodec;
import com.tomgibara.imageio.impl.tiff.TIFFLZWCompressor;
import com.tomgibara.imageio.tiff.TIFFCompressor;
import com.tomgibara.imageio.tiff.TIFFImageWriteParam;

import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.RescaleOp;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VicarImageConverter {
    private final String inputFilePath;
    private String outputtedFilePath;
    private final String writeDirectory;
    private BufferedImage image;
    private Metadata metadata;
    private IIOMetadata iioMetadata;
    private final String format;
    private final boolean stretch;
    private AtomicInteger counter;
    private Integer seqCount;
    private MetadataProcessor metadataProcessor;
    public static final String SYSTEM_PERCENT_UTILIZATION = "system.percent.utilization";
    Logger logger;

    public VicarImageConverter(Metadata metadata, Integer seqCount, String writeDirectory, String format, AtomicInteger counter) {
        this(metadata, seqCount, writeDirectory, format, false, counter);
    }

    public VicarImageConverter(Metadata metadata, Integer seqCount, String writeDirectory, String format, boolean stretch, AtomicInteger counter) {
        this.metadataProcessor = new MetadataProcessor();
        this.logger = LoggerFactory.getLogger(VicarImageConverter.class);
        this.metadata = metadata;
        this.inputFilePath = metadata.getFileInfo().getFilePath();
        if (!writeDirectory.endsWith("/")) {
            writeDirectory = writeDirectory + "/";
        }

        this.writeDirectory = writeDirectory;
        this.format = format;
        this.stretch = stretch;
        this.counter = counter;
        this.seqCount = seqCount;
    }

    public void convert() throws IOException, ParseException {
        File file = new File(this.inputFilePath);
        if (!file.canRead()) {
            this.logger.error("file cannot be found {}", this.inputFilePath);
            throw new RuntimeException("file cannot be found " + this.inputFilePath);
        } else {
            this.readImage(file);
            Map<String, String> valueMap = this.metadataProcessor.process(this.iioMetadata);
            this.generateFilePath();
            if ((new File(this.outputtedFilePath)).exists()) {
                throw new IllegalStateException("image already exists: " + this.outputtedFilePath);
            } else {
                if (((String) valueMap.get("mission")).startsWith("CAS")) {
                    this.calibrate();
                } else {
                    this.normalize();
                }

                try {
                    this.sleepTask();
                } catch (InterruptedException var4) {
                    this.logger.error("could not put vicar image converter to sleep", var4);
                }

                if (this.stretch) {
                    this.stretchHistogram();
                }

                this.writeImage(file.getName());
                this.image = null;
            }
        }
    }

    private void normalize() {
        int width = this.image.getWidth();
        int height = this.image.getHeight();
        int bitDepthExp = this.image.getColorModel().getPixelSize();
        double bitDepth = (double) (1 << bitDepthExp);
        DataBuffer buffer = this.image.getRaster().getDataBuffer();
        double[] rasterArray = null;
        if (buffer instanceof DataBufferDouble) {
            DataBufferDouble doubleBuffer = (DataBufferDouble) this.image.getRaster().getDataBuffer();
            rasterArray = doubleBuffer.getData();
        } else {
            rasterArray = this.image.getRaster().getPixels(0, 0, width, height, new double[width * height]);
        }

        this.logger.debug("double buffer length: {}", rasterArray.length);
        this.image = null;
        double finalDivisor = bitDepth;
        if (bitDepthExp == 32) {
            double sH = 0.0D;
            double highestAmount = 0.0D;

            for (int i = 0; i < rasterArray.length; ++i) {
                if (i % 65536 == 0) {
                    try {
                        this.sleepTask();
                    } catch (InterruptedException var18) {
                        this.logger.error("problem sleeping while normalizing", var18);
                    }
                }

                double val = rasterArray[i];
                if (val > highestAmount) {
                    sH = highestAmount;
                    highestAmount = val;
                }
            }

            if (highestAmount > 1.0D) {
                finalDivisor = highestAmount;
            } else {
                finalDivisor = 1.0D;
            }

            this.logger.debug("highest/sh {}/{}", highestAmount, sH);
        }

        if (finalDivisor != 1.0D) {
            for (int i = 0; i < rasterArray.length; ++i) {
                if (i % 65536 == 0) {
                    try {
                        this.sleepTask();
                    } catch (InterruptedException var17) {
                        this.logger.error("problem sleeping while normalizing", var17);
                    }
                }

                if (rasterArray[i] > 0.0D) {
                    rasterArray[i] /= finalDivisor;
                } else {
                    rasterArray[i] = 0.0D;
                }
            }
        }

        this.logger.debug("normalized against bit depth {}", bitDepth);
        SampleModel sampleModel = new PixelInterleavedSampleModel(4, width, height, 1, width, new int[1]);
        ColorModel colorModel = ImageCodec.createComponentColorModel(sampleModel, ColorSpace.getInstance(1003));
        DataBuffer dataBuffer = new DataBufferDouble(rasterArray, rasterArray.length);
        WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
        rasterArray = null;
        this.image = new BufferedImage(colorModel, raster, false, (Hashtable) null);
    }

    public void calibrate() throws IOException {
        int width = this.image.getWidth();
        int height = this.image.getHeight();
        double[] rasterArray = this.image.getRaster().getPixels(0, 0, width, height, new double[width * height]);
        Utils.logMinMaxValues("before calibration", rasterArray);
        this.image = null;
        String cassiniCalibrationDirectory = System.getProperty("cassini.calibration.dir");
        Lut8to12BitCalibrator lutCalibrator = new Lut8to12BitCalibrator();
        boolean lut = lutCalibrator.calibrate(rasterArray, this.iioMetadata);
        Utils.logMinMaxValues("after lut", rasterArray);
        BitweightCalibrator bwCalibrator = new BitweightCalibrator(cassiniCalibrationDirectory);
        boolean bw = bwCalibrator.calibrate(rasterArray, this.iioMetadata);
        Utils.logMinMaxValues("after bitweight", rasterArray);
        DebiasCalibrator debiasCalibrator = new DebiasCalibrator();
        boolean de = debiasCalibrator.calibrate(rasterArray, this.iioMetadata);
        Utils.logMinMaxValues("after debias", rasterArray);
        CassiniDustRingCalibrator cassiniCalibrator = new CassiniDustRingCalibrator(cassiniCalibrationDirectory);
        boolean ca = cassiniCalibrator.calibrate(rasterArray, this.iioMetadata);
        Utils.logMinMaxValues("after dust calib", rasterArray);
        DivideByFlatsCalibrator divideByFlatsCalibrator = new DivideByFlatsCalibrator(cassiniCalibrationDirectory);
        boolean dbf = divideByFlatsCalibrator.calibrate(rasterArray, this.iioMetadata);
        Utils.logMinMaxValues("after divide by flats", rasterArray);
        BufferedImage imageNew = null;
        if (!bw && !de && !ca && !dbf) {
            imageNew = new BufferedImage(width, height, 11);
            imageNew.getRaster().setPixels(0, 0, width, height, rasterArray);
            Utils.logMinMaxValues("if no calib", rasterArray);
        } else {
            this.logger.debug("{} has been CALIBRATED", this.inputFilePath);
            boolean needsNormalized = false;

            for (int i = 0; i < rasterArray.length; ++i) {
                if (rasterArray[i] > 1.0D) {
                    needsNormalized = true;
                    break;
                }
            }

            if (needsNormalized) {
                double bitDivisor = 1.0D;
                if (lut) {
                    bitDivisor = 4096.0D;
                } else {
                    bitDivisor = 65536.0D;
                }

                for (int i = 0; i < rasterArray.length; ++i) {
                    rasterArray[i] /= bitDivisor;
                }

                this.logger.debug("{} has been NORMALIZED with bit depth of {}", new Object[]{this.inputFilePath, bitDivisor});
            }
            Utils.logMinMaxValues("after possible normalization", rasterArray);

            SampleModel sampleModel = new PixelInterleavedSampleModel(4, width, height, 1, width, new int[1]);
            ColorModel colorModel = ImageCodec.createComponentColorModel(sampleModel, ColorSpace.getInstance(1003));
            DataBuffer dataBuffer = new DataBufferDouble(rasterArray, rasterArray.length);
            WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
            rasterArray = null;
            imageNew = new BufferedImage(colorModel, raster, false, (Hashtable) null);
        }

        this.image = imageNew;
    }

    private void generateFilePath() throws ParseException {
        String sep = "/";
        this.outputtedFilePath = this.writeDirectory + sep + this.metadataProcessor.getFileStructure() + sep;
        if (this.seqCount == null) {
            this.outputtedFilePath = this.outputtedFilePath + this.metadataProcessor.getFileName();
        } else {
            String sequence = this.seqCount.toString();
            sequence = "0000000000".substring(sequence.length()) + sequence;
            this.outputtedFilePath = this.outputtedFilePath + this.metadata.getMission() + "_" + this.metadata.getTarget() + "_" + this.metadata.getFilterOne() + "-" + this.metadata.getFilterTwo() + "_" + sequence;
        }

        this.outputtedFilePath = this.outputtedFilePath + "." + this.format;

        try {
            Files.createParentDirs(new File(this.outputtedFilePath));
        } catch (IOException var3) {
            this.logger.error("problem creating directory for {}", this.outputtedFilePath);
            throw new RuntimeException("problem creating directory for " + this.outputtedFilePath, var3);
        }
    }

    public void writeImage(String fileName) throws IOException {
        File output = new File(this.outputtedFilePath);
        ImageOutputStream stream = null;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(this.format);
        if (!writers.hasNext()) {
            this.logger.error("we could not find a writer. make no changes and do not attempt to convert the image");
            throw new IllegalStateException("could not find a writer");
        } else {
            ImageWriter writer = (ImageWriter) writers.next();

            try {
                output.delete();
                this.logger.trace("writing the converted image to {}", this.outputtedFilePath);
                this.buildLocalDirectoryStructure(this.outputtedFilePath);
                output.createNewFile();
                stream = ImageIO.createImageOutputStream(output);
            } catch (IOException var13) {
                throw new IIOException("Can't create output stream!", var13);
            }

            try {
                this.doWrite(this.image, writer, stream);
            } catch (IOException var11) {
                this.logger.error("image writing failed", var11);
                throw var11;
            } finally {
                stream.flush();
                writer.dispose();
                stream.close();
            }

        }
    }

    private void buildLocalDirectoryStructure(String link) {
        String[] fileStructure = link.split("/");
        String dir = "";
        String[] arr$ = fileStructure;
        int len$ = fileStructure.length;

        for (int i$ = 0; i$ < len$; ++i$) {
            String nextDir = arr$[i$];
            if (link.endsWith(nextDir)) {
                break;
            }

            dir = dir + nextDir + "/";
            File tempFile = new File(dir);
            if (!tempFile.canRead()) {
                this.logger.trace("cannot read directory {}, creating it", tempFile.getAbsolutePath());
                tempFile.mkdir();
                this.logger.trace("created directory {}", tempFile.getAbsolutePath());
            }
        }

    }

    private boolean doWrite(RenderedImage im, ImageWriter writer, ImageOutputStream output) throws IOException {
        if (writer == null) {
            return false;
        } else {
            writer.setOutput(output);
            ImageWriteParam imageWriteParam = new TIFFImageWriteParam((Locale) null);
            TIFFCompressor compressor = new TIFFLZWCompressor(0);
            ((TIFFImageWriteParam) imageWriteParam).setCompressionMode(2);
            ((TIFFImageWriteParam) imageWriteParam).setTIFFCompressor(compressor);
            ((TIFFImageWriteParam) imageWriteParam).setCompressionType(compressor.getCompressionType());
            writer.write(this.iioMetadata, new IIOImage(this.image, (List) null, this.iioMetadata), imageWriteParam);
            return true;
        }
    }

    public void stretchHistogram() {
        Double maxValue = Math.pow(2.0D, (double) this.image.getColorModel().getPixelSize());
        float scaleFactor = (float) (maxValue / (double) ((float) this.image.getHeight() * (float) this.image.getWidth())) + 1.0F;
        RescaleOp rescaleOp = new RescaleOp(scaleFactor, 0.0F, (RenderingHints) null);
        rescaleOp.filter(this.image, this.image);
    }

    public void readImage(File file) throws IOException {
        Iterator<ImageReader> readers = ImageIO.getImageReaders(ImageIO.createImageInputStream(file));
        if (!readers.hasNext()) {
            this.logger.error("no image reader was found for {}", this.inputFilePath);
            throw new RuntimeException("no image reader was found for " + this.inputFilePath);
        } else {
            ImageReader reader = (ImageReader) readers.next();
            reader.setInput(ImageIO.createImageInputStream(file));
            this.image = reader.read(0);
            this.iioMetadata = reader.getImageMetadata(0);
            reader.dispose();
        }
    }

    public String getOutputtedFilePath() {
        return this.outputtedFilePath;
    }

    public IIOMetadata getIIOMetaData() {
        return this.iioMetadata;
    }

    public void sleepTask() throws InterruptedException {
        int timeToSubtract = 990;
        int percentage;
        if (System.getProperties().containsKey("system.percent.utilization")) {
            percentage = Integer.parseInt(System.getProperties().getProperty("system.percent.utilization"));
            if (percentage == 100) {
                timeToSubtract = 0;
            } else {
                double finalPercent = (double) percentage / 100.0D;
                double firstPercentage = 1000.0D * finalPercent;
                double secondPercentage = 100.0D * finalPercent;
                if (firstPercentage + secondPercentage < 1000.0D) {
                    timeToSubtract = (int) (firstPercentage + secondPercentage);
                } else {
                    timeToSubtract = 995;
                }
            }

            this.logger.trace("percentage for subtraction: {}", percentage);
        }

        this.logger.trace("time to subtract from 1000 milliseconds: {}", timeToSubtract);
        if (timeToSubtract != 0) {
            percentage = 1000 - timeToSubtract;
            Thread.sleep((long) percentage);
        }

    }
}
