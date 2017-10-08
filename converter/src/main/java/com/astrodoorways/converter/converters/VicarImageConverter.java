package com.astrodoorways.converter.converters;

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
import java.awt.image.RenderedImage;
import java.awt.image.RescaleOp;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.vicar.cassini.BitweightCalibrator;
import com.astrodoorways.converter.vicar.cassini.CassiniDustRingCalibrator;
import com.astrodoorways.converter.vicar.cassini.DebiasCalibrator;
import com.astrodoorways.converter.vicar.cassini.DivideByFlatsCalibrator;
import com.astrodoorways.converter.vicar.cassini.Lut8to12BitCalibrator;
import com.astrodoorways.db.imagery.Metadata;
import com.google.common.io.Files;
import com.tomgibara.imageio.impl.tiff.TIFFLZWCompressor;
import com.tomgibara.imageio.tiff.TIFFCompressor;
import com.tomgibara.imageio.tiff.TIFFImageWriteParam;

/**
 * Convert a Vicar/PDS formatted image into another image format.
 * 
 * @author kmcabee
 * 
 */
public class VicarImageConverter {

	private final String inputFilePath;
	private final String writeDirectory;
	//private BufferedImage image;
	private BufferedImage inputImage;
	private Metadata metadata;
	private IIOMetadata iioMetadata;
	private final String format;
	private Integer seqCount;
	double currentMaxPixel = 0.0;
	private MetadataProcessor metadataProcessor = new MetadataProcessor();
	private List<String> outputtedFilePaths = new ArrayList<String>();

	Logger logger = LoggerFactory.getLogger(VicarImageConverter.class);

	public VicarImageConverter(Metadata metadata, Integer seqCount, String writeDirectory, String format,
			AtomicInteger counter) {
		this(metadata, seqCount, writeDirectory, format, false);
	}

	public VicarImageConverter(Metadata metadata, Integer seqCount, String writeDirectory, String format,
			boolean stretch) {
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
	 * main convert method
	 * 
	 * @throws IOException
	 * @throws ParseException
	 */
	public void convert() throws IOException, ParseException {
		// open file with list of paths
		final File file = new File(inputFilePath);
		if (!file.canRead()) {
			logger.error("file cannot be found {}", inputFilePath);
			throw new RuntimeException("file cannot be found " + inputFilePath);
		}

		logger.trace("reading in the image");
		readImage(file);
		Map<String, String> valueMap = metadataProcessor.process(iioMetadata);

		writeSegmentedImage(inputImage, valueMap);

		try {
			sleepTask();
		} catch (InterruptedException e) {
			logger.error("could not put vicar image converter to sleep", e);
		}
		inputImage = null;
	}

	/**
	 * Build a segmented image based on maximum width and height tiling values.
	 * 
	 * @param inputImage
	 * @param valueMap
	 * @throws IOException
	 * @throws ParseException
	 */
	private void writeSegmentedImage(BufferedImage inputImage, Map<String, String> valueMap) throws IOException,
			ParseException {
		int width = inputImage.getWidth();
		int height = inputImage.getHeight();

		// base width initializations
		int baseWidth = width;
		int segmentCountByWidth = 1;
		int remainderWidth = 0;
		if (width > 5000) {
			segmentCountByWidth = width / 5000;
			baseWidth = 5000;
			remainderWidth = width % 5000;
		} else {
			segmentCountByWidth = 1;
			baseWidth = width;
			remainderWidth = 0;
		}

		// base height initializations
		int baseHeight = height;
		int segmentCountByHeight = 1;
		int remainderHeight = 0;
		if (height > 5000) {
			segmentCountByHeight = height / 5000;
			baseHeight = 5000;
			remainderHeight = height % 5000;
		} else {
			segmentCountByHeight = 1;
			baseHeight = height;
			remainderHeight = 0;
		}

		logger.debug(
				"initial values: baseWidth: {}, segmented width count: {}, remainderWidth:{}, baseHeight: {}, segmented height count: {}, remainderHeight:{}",
				new Object[] { baseWidth, segmentCountByWidth, remainderWidth, baseHeight, segmentCountByHeight,
						remainderHeight });

		if (ApplicationProperties.getPropertyAsBoolean(ApplicationProperties.NORMALIZE)) {
			logger.debug("find the the maximum light value for the image");
			Raster raster = inputImage.getRaster();
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					double[] pixel = raster.getPixel(x, y, new double[1]);
					if (pixel[0] > currentMaxPixel)
						currentMaxPixel = pixel[0];
				}
			}
			logger.debug("maximum light value: {}", currentMaxPixel);
		}

		// Begin working through segments
		for (int w = 0; w <= segmentCountByWidth; w++) {
			for (int h = 0; h <= segmentCountByHeight; h++) {

				int tileWidth = baseWidth;
				int tileHeight = baseHeight;
				// final width, add remainder width
				if (w == segmentCountByWidth)
					tileWidth = remainderWidth;
				if (h == segmentCountByHeight)
					tileHeight = remainderHeight;

				int x = w * baseWidth;
				int y = h * baseHeight;

				logger.debug("w:{}, h:{}, tileW:{}, tileH:{}, x:{}, y:{}", new Object[] { w, h, tileWidth, tileHeight,
						x, y });
				// initial image prep
				SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, tileWidth, tileHeight,
						1, tileWidth, new int[1]);
				ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false,
						false, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);

				// build the output raster array and populate it
				double[] outputRasterArray = new double[tileWidth * tileHeight];
				outputRasterArray = inputImage.getRaster().getPixels(x, y, tileWidth, tileHeight, outputRasterArray);

				DataBuffer dataBuffer = new DataBufferDouble(outputRasterArray, outputRasterArray.length);
				WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
				BufferedImage outputImage = new BufferedImage(colorModel, raster, false, null);

				// post processing (calibration, normalizing, etc)
				outputImage = postProcessImage(outputImage, valueMap);

				// write out image
				String path = generateOutputFilePath(w, h);
				logger.trace("generating output path");
				// see if file already exists before trying to recreate

				// the image has already been created
				if (new File(path).exists()) {
					throw new IllegalStateException("image already exists: " + path);
				}
				writeImage(outputImage, path);
				outputtedFilePaths.add(path);

			}
		}
	}

	/**
	 * Normalize an image against its highest light value. Only accepts BufferedImages with DataBufferDouble buffers.
	 * @param image
	 * @return
	 */
	private BufferedImage normalizeDouble(BufferedImage image) {
		if (!(image.getRaster().getDataBuffer() instanceof DataBufferDouble))
			throw new IllegalArgumentException("can't normalize a non-double based buffered image");

		int width = image.getWidth();
		int height = image.getHeight();
		int bitDepthExp = image.getColorModel().getPixelSize();

		double bitDepth = (double) (1 << bitDepthExp);
		DataBufferDouble doubleBuffer = (DataBufferDouble) image.getRaster().getDataBuffer();
		double[] rasterArray = doubleBuffer.getData();
		inputImage = null;

		// actual normalization process

		double finalDivisor = bitDepth;
		if (currentMaxPixel != 1) {
			for (int i = 0; i < rasterArray.length; i++) {
				if (i % 65536 == 0) {
					try {
						sleepTask();
					} catch (InterruptedException e) {
						logger.error("problem sleeping while normalizing", e);
					}
				}
				if (rasterArray[i] > 0.0) {
					rasterArray[i] = rasterArray[i] / finalDivisor;
				} else {
					rasterArray[i] = 0.0;
				}
			}
		}

		SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1, width,
				new int[1]);
		//		ColorModel colorModel = ImageCodec.createComponentColorModel(sampleModel,
		//				ColorSpace.getInstance(ColorSpace.CS_GRAY));
		ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
				Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
		DataBuffer dataBuffer = new DataBufferDouble(rasterArray, rasterArray.length);
		WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
		rasterArray = null;
		image = new BufferedImage(colorModel, raster, false, null);

		return image;

	}

	private BufferedImage postProcessImage(BufferedImage image, Map<String, String> valueMap) {
		if (valueMap.get(MetadataProcessor.MISSION).startsWith("CAS")
				&& ApplicationProperties.hasProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR)) {
			logger.trace("calibrating");
			try {
				return calibrate(image);
			} catch (Exception e) {
				logger.error("calibration failed {}", e.getClass());
			}
		} else {
			logger.trace("standard normalizing");
			if (ApplicationProperties.getPropertyAsBoolean(ApplicationProperties.NORMALIZE)) {
				return normalizeDouble(image);
			}
		}
		return image;
	}

	public BufferedImage calibrate(BufferedImage image) throws IOException {
		int width = image.getWidth();
		int height = image.getHeight();
		logger.trace("getting raster array");
		double[] rasterArray = image.getRaster().getPixels(0, 0, width, height, new double[width * height]);
		image = null;

		logger.trace("getting cassini calibration directory");
		String cassiniCalibrationDirectory = ApplicationProperties.getPropertyAsString("cassini.calibration.dir");

		BufferedImage imageNew = null;
		logger.trace("lut 8to12 calibration");
		Lut8to12BitCalibrator lutCalibrator = new Lut8to12BitCalibrator();
		boolean lut = lutCalibrator.calibrate(rasterArray, iioMetadata);

		logger.trace("bitweight calibration");
		BitweightCalibrator bwCalibrator = new BitweightCalibrator(cassiniCalibrationDirectory);
		boolean bw = bwCalibrator.calibrate(rasterArray, iioMetadata);

		logger.trace("debias calibration");
		DebiasCalibrator debiasCalibrator = new DebiasCalibrator();
		boolean de = debiasCalibrator.calibrate(rasterArray, iioMetadata);

		logger.trace("dust calibration");
		CassiniDustRingCalibrator cassiniCalibrator = new CassiniDustRingCalibrator(cassiniCalibrationDirectory);
		boolean ca = cassiniCalibrator.calibrate(rasterArray, iioMetadata);

		logger.trace("divide by flats calibration");
		DivideByFlatsCalibrator divideByFlatsCalibrator = new DivideByFlatsCalibrator(cassiniCalibrationDirectory);
		boolean dbf = divideByFlatsCalibrator.calibrate(rasterArray, iioMetadata);

		if (bw || de || ca || dbf) {
			logger.trace("{} has been CALIBRATED", inputFilePath);

			boolean needsNormalized = false;
			for (int i = 0; i < rasterArray.length; i++) {
				if (rasterArray[i] > 1.0d) {
					needsNormalized = true;
					break;
				}
			}

			if (needsNormalized) {
				double bitDivisor = 1;
				if (lut) {
					bitDivisor = 1 << 12;
				} else {
					bitDivisor = 1 << 16;
				}

				for (int i = 0; i < rasterArray.length; i++) {
					rasterArray[i] = rasterArray[i] / bitDivisor;
				}
				logger.trace("{} has been NORMALIZED with bit depth of {}", new Object[] { inputFilePath, bitDivisor });
			}
			SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1, width,
					new int[1]);
			ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
					Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);

			DataBuffer dataBuffer = new DataBufferDouble(rasterArray, rasterArray.length);
			WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
			rasterArray = null;
			imageNew = new BufferedImage(colorModel, raster, false, null);
		} else {
			imageNew = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
			imageNew.getRaster().setPixels(0, 0, width, height, rasterArray);
		}

		return imageNew;
	}

	/**
	 * @return a path to the file with all necessary directories created
	 * @throws ParseException
	 */
	private String generateOutputFilePath(int w, int h) throws ParseException {
		String sep = "/";
		String outputtedFilePath = writeDirectory + sep;
		if (seqCount == null) {
			outputtedFilePath += metadataProcessor.getFileStructure() + sep + metadataProcessor.getFileName();
		} else {

			// BUILD DIRECTORY
			outputtedFilePath += metadata.getMission().replace("/", "-") + "_" + metadata.getTarget() + "_"
					+ metadata.getFilterOne();

			// not all missions have a seondary filter for the cameras
			if (metadata.getFilterTwo() != null)
				outputtedFilePath += "-" + metadata.getFilterTwo();

			outputtedFilePath += sep;

			// BUILD FILENAME
			String sequence = seqCount.toString();
			sequence = "0000000000".substring(sequence.length()) + sequence;
			outputtedFilePath += metadata.getMission().replace("/", "-") + "_" + metadata.getTarget() + "_"
					+ metadata.getFilterOne();

			// not all missions have a seondary filter for the cameras
			if (metadata.getFilterTwo() != null)
				outputtedFilePath += "-" + metadata.getFilterTwo();

			outputtedFilePath += "_" + sequence;
		}
		outputtedFilePath += "w" + w + "-h" + h + "." + format;
		try {
			Files.createParentDirs(new File(outputtedFilePath));
		} catch (IOException e) {
			logger.error("problem creating directory for {}", outputtedFilePath);
			throw new RuntimeException("problem creating directory for " + outputtedFilePath, e);
		}

		return outputtedFilePath;
	}

	/**
	 * write the image
	 * 
	 * @param fileName
	 * @throws IOException
	 */
	public void writeImage(BufferedImage image, String outputtedFilePath) throws IOException {
		File output = new File(outputtedFilePath);

		// get the writer
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
		if (!writers.hasNext()) {
			logger.error("we could not find a writer. make no changes and do not attempt to convert the image");
			throw new IllegalStateException("could not find a writer");
		}

		ImageWriter writer = writers.next();

		// delete the old file and create a new output stream to it
		ImageOutputStream stream = null;
		try {
			output.delete();
			logger.trace("writing the converted image to {}", outputtedFilePath);
			buildLocalDirectoryStructure(outputtedFilePath);
			output.createNewFile();
			stream = ImageIO.createImageOutputStream(output);

			// setting the byte order to be the more standard PC format
			stream.setByteOrder(ByteOrder.LITTLE_ENDIAN);
			doWrite(image, writer, stream);
		} catch (IOException e) {
			logger.error("image writing failed", e);
			throw e;
		} finally {
			stream.flush();
			writer.dispose();
			stream.close();
		}
	}

	/**
	 * build a local directory structure
	 * 
	 * @param link
	 */
	private void buildLocalDirectoryStructure(String link) {
		String[] fileStructure = link.split("/");
		String dir = "";
		for (String nextDir : fileStructure) {
			if (link.endsWith(nextDir))
				break;

			dir += nextDir + "/";
			File tempFile = new File(dir);
			if (!tempFile.canRead()) {
				logger.trace("cannot read directory {}, creating it", tempFile.getAbsolutePath());
				tempFile.mkdir();
				logger.trace("created directory {}", tempFile.getAbsolutePath());
			}
		}
	}

	/**
	 * Writes image to output stream using given image writer.
	 */
	private boolean doWrite(RenderedImage im, ImageWriter writer, ImageOutputStream output) throws IOException {
		if (writer == null) {
			return false;
		}
		writer.setOutput(output);
		ImageWriteParam imageWriteParam = new TIFFImageWriteParam(null);
		TIFFCompressor compressor = new TIFFLZWCompressor(0);
		((TIFFImageWriteParam) imageWriteParam).setCompressionMode(imageWriteParam.MODE_EXPLICIT);
		((TIFFImageWriteParam) imageWriteParam).setTIFFCompressor(compressor);
		((TIFFImageWriteParam) imageWriteParam).setCompressionType(compressor.getCompressionType());
		writer.write(iioMetadata, new IIOImage(im, null, iioMetadata), imageWriteParam);
		return true;
	}

	/**
	 * stretch the histogram of the image
	 */
	public void stretchHistogram(BufferedImage image) {
		Double maxValue = Math.pow(2.0d, (double) image.getColorModel().getPixelSize());
		float scaleFactor = (float) (maxValue / (((float) image.getHeight()) * ((float) image.getWidth()))) + 1f;
		RescaleOp rescaleOp = new RescaleOp(scaleFactor, 0, null);
		rescaleOp.filter(image, image);
	}

	/**
	 * read a file and convert it into an image, acquiring the metadata along
	 * the way
	 * 
	 * @param file
	 * @throws IOException
	 */
	public void readImage(File file) throws IOException {
		Iterator<ImageReader> readers = (Iterator<ImageReader>) ImageIO.getImageReaders(ImageIO
				.createImageInputStream(file));
		if (!readers.hasNext()) {
			logger.error("no image reader was found for {}", inputFilePath);
			throw new RuntimeException("no image reader was found for " + inputFilePath);
		}

		ImageReader reader = null;
		try {
			reader = readers.next();
			reader.setInput(ImageIO.createImageInputStream(file));
			inputImage = reader.read(0);
			iioMetadata = reader.getImageMetadata(0);
		} catch (IOException e) {
			throw e;
		} finally {
			reader.dispose();
		}
	}

	/**
	 * @return the metadata object. this should only be called after convert
	 */
	public IIOMetadata getIIOMetaData() {
		return iioMetadata;
	}

	public void sleepTask() throws InterruptedException {
		int timeToSubtract = 990;
		// see if the user passed in a percentage of system utilization value
		if (ApplicationProperties.hasProperty(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION)) {
			int percentage = Integer.parseInt(ApplicationProperties
					.getPropertyAsString(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION));
			if (percentage == 100) {
				timeToSubtract = 0;
			} else {
				double finalPercent = percentage / 100d;
				double firstPercentage = 1000d * finalPercent;
				double secondPercentage = 100d * finalPercent;
				if ((firstPercentage + secondPercentage) < 1000)
					timeToSubtract = (int) (firstPercentage + secondPercentage);
				else
					timeToSubtract = 995;
			}
		}
		// if the user did not specify 100% system utilization, calculate sleep time to match
		if (timeToSubtract != 0) {
			int millisToSleep = (int) (1000 - (timeToSubtract));
			Thread.sleep(millisToSleep);
		}
	}

	public List<String> getOutputtedFilePaths() {
		return outputtedFilePaths;
	}

}
