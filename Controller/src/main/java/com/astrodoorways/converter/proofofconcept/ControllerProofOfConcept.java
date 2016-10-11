package com.astrodoorways.converter.proofofconcept;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.RescaleOp;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;

import jpl.mipl.io.plugins.JPLImageReader;
import jpl.mipl.io.plugins.VicarRenderedImage;
import jpl.mipl.io.vicar.SystemLabel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControllerProofOfConcept {

	RenderedImage renderedImage = null;
	BufferedImage bufferedImage = null;

	boolean debug = false;
	// boolean debug = true; // false;

	boolean getAsRenderedImage = true;
	boolean formatToByte = false;
	String inputFileName = "";
	String imageFormatName = "";

	IIOImage iioImage = null;

	private static final Logger logger = LoggerFactory.getLogger(ControllerProofOfConcept.class);

	public static void main2(String... argv) {
		String inputDir = "/Users/kmcabee/Desktop/vicarInput";
		String outputDir = "/Users/kmcabee/Desktop/vicarOutput/";
		String type = "tiff";
		String thumbType = "jpeg";
		logger.debug("test");
		try {
			for (File file : new File(inputDir).listFiles()) {
				if (!file.getName().endsWith(".IMG") && !file.getName().endsWith(".img"))
					continue;

				logger.debug("Processing file: " + file.getName());
				Iterator<ImageReader> readers = (Iterator<ImageReader>) ImageIO.getImageReaders(ImageIO
						.createImageInputStream(file));
				JPLImageReader reader = (JPLImageReader) readers.next();
				reader.setInput(ImageIO.createImageInputStream(file));
				BufferedImage image = reader.read(0);
				logger.debug("Image bit size {}", image.getColorModel().getPixelSize());
				SystemLabel label = reader.getSystemLabel();
				// BufferedImage contrastedImage =
				// buildContrastRescaledImage(image);

				String fileNameNoExtension = file.getName().substring(0, file.getName().length() - 4);
				ImageIO.write(image, type, new File(outputDir + fileNameNoExtension + "." + type));
				ImageIO.write(image, thumbType, new File(outputDir + fileNameNoExtension + "." + thumbType));
				// ImageIO.write(contrastedImage, type, new File(outputDir +
				// fileNameNoExtension + "-CONTRAST." + type));

				// contrast
				// RescaleOp rescale = new RescaleOp(30f, 15, null);
				// rescale.filter(image, image);
				// buildHistogram(outputDir, fileNameNoExtension, image);
				// ImageIO.write(image, thumbType, new File(outputDir +
				// fileNameNoExtension + "." + thumbType));
			}
		} catch (Exception e) {
			logger.error("error processing the file", e);
		}
	}

	public BufferedImage getThumbSizedVersion(BufferedImage image) {
		int highestValue = image.getHeight() > image.getWidth() ? image.getHeight() : image.getWidth();
		double ratio = 150 / highestValue;
		AffineTransform transform = AffineTransform.getScaleInstance(ratio, ratio);
		AffineTransformOp transformOp = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
		return transformOp.filter(image, null);
	}

	public static Map<Integer, Integer> buildHistogramMap(BufferedImage image) {
		Raster raster = image.getRaster();
		int height = image.getHeight();
		int width = image.getWidth();
		Map<Integer, Integer> histogramMap = new TreeMap<Integer, Integer>();
		for (int band = 0; band < raster.getNumBands(); band++) {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					int sampleValue = raster.getSample(x, y, band);
					if (histogramMap.containsKey(sampleValue)) {
						histogramMap.put(sampleValue, histogramMap.get(sampleValue) + 1);
					} else {
						histogramMap.put(sampleValue, 0);
					}
				}
			}
		}
		return histogramMap;
	}

	/**
	 * @param currentHistogram
	 * @param bitLevel
	 * @param height
	 * @param width
	 * @return Map containing the original value and the new value
	 */
	public static Map<Integer, Integer> buildEqualizedHistogram(Map<Integer, Integer> currentHistogram,
			Integer bitLevel, Integer height, Integer width) {
		Double maxValue = Math.pow(2.0d, (double) bitLevel);
		double scaleFactor = maxValue / (((double) height) * ((double) width));
		Map<Integer, Integer> equalizedHistorgramMap = new HashMap<Integer, Integer>();
		int sum = 0;
		for (Entry<Integer, Integer> entry : currentHistogram.entrySet()) {
			sum += entry.getValue();
			equalizedHistorgramMap.put(entry.getKey(), (int) (sum * scaleFactor));
		}

		return equalizedHistorgramMap;
	}

	public static BufferedImage buildContrastRescaledImage(BufferedImage image) {
		Double maxValue = Math.pow(2.0d, (double) image.getColorModel().getPixelSize());
		float scaleFactor = (float) (maxValue / (((float) image.getHeight()) * ((float) image.getWidth()))) + 1f;
		RescaleOp rescaleOp = new RescaleOp(scaleFactor, 0, null);
		return rescaleOp.filter(image, null);
	}

	public static BufferedImage processImageUsingEqualizedHistogram(Map<Integer, Integer> equalizedHistogram,
			BufferedImage imageToEqualize) {
		int width = imageToEqualize.getWidth();
		int height = imageToEqualize.getHeight();
		Raster originalRaster = Raster.createWritableRaster(imageToEqualize.getSampleModel(), null);
		BufferedImage imageToReturn = new BufferedImage(width, height, imageToEqualize.getType());
		WritableRaster writableRaster = Raster.createWritableRaster(imageToReturn.getSampleModel(), null);

		for (int band = 0; band < originalRaster.getNumBands(); band++) {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					int sampleValue = originalRaster.getSample(x, y, band);
					if (equalizedHistogram.containsKey(sampleValue)) {
						writableRaster.setPixel(x, y, new int[] { equalizedHistogram.get(sampleValue) });
					} else {
						writableRaster.setPixel(x, y, new int[] { 0 });
					}
				}
			}
		}
		return imageToReturn;
	}

	public static void buildHistogram(String outputDir, String file, BufferedImage image) {
		Raster raster = image.getRaster();
		int height = image.getHeight();
		int width = image.getWidth();
		Map<Integer, Integer> histogramMap = new TreeMap<Integer, Integer>();
		for (int band = 0; band < raster.getNumBands(); band++) {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					int sampleValue = raster.getSample(x, y, band);
					if (histogramMap.containsKey(sampleValue)) {
						histogramMap.put(sampleValue, histogramMap.get(sampleValue) + 1);
					} else {
						histogramMap.put(sampleValue, 0);
					}
				}
			}
		}

		int histWidth = 250;
		int histHeight = 100;

		BufferedImage histogramImage = new BufferedImage(histWidth, histHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2D = histogramImage.createGraphics();

		Polygon background = new Polygon(new int[] { 0, histWidth, histWidth, 0 }, new int[] { 0, 0, histHeight,
				histHeight }, 4);
		Area bckgrndArea = new Area(background);

		Color bckgrndColor = new Color(0, 0, 0);
		Color foregrndColor = new Color(255, 255, 255);
		g2D.setColor(bckgrndColor);
		g2D.fill(bckgrndArea);

		float stepX = (float) (histWidth) / (float) histogramMap.size();
		float stepY = (float) (histHeight) / (float) 65536;

		Polygon poly = new Polygon();
		poly.addPoint(2, histHeight - 2);
		for (Entry<Integer, Integer> entry : histogramMap.entrySet()) {
			{
				int x = (int) ((float) entry.getKey() * stepX);
				int y = (int) ((float) entry.getValue() * stepY);

				poly.addPoint(x + 2, (histHeight - y - 2));
			}
		}
		poly.addPoint(histWidth - 2, histHeight - 2);
		g2D.setColor(foregrndColor);
		g2D.fill(poly);
		try {
			ImageIO.write(histogramImage, "png", new File(outputDir + file + ".png"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public IIOImage fullRead(String fileName) throws IOException {

		String inputFileName = fileName;

		// we will need a File, URL and stream version eventually

		ImageInputStream iis = null;
		IIOMetadata im = null;
		IIOMetadata sm = null;
		ImageReader reader = null;
		String readerClassName = "";
		String readerFormat = "";

		int numImages = 0;
		boolean debug = true;
		if (debug)
			System.out.println("ImageUtil open: " + fileName);
		try {
			iis = ImageIO.createImageInputStream(new File(fileName));

			if (iis == null) {
				System.out.println("Unable to get a stream!");
				// return null ; // error return
				// throw an exception !!! instead
				throw new IOException("ImageUtils.fullRead() Unable to get a stream!");
			}

			Iterator iter = ImageIO.getImageReaders(iis);

			while (iter.hasNext()) {
				reader = (ImageReader) iter.next();
				if (debug)
					System.out.println("Using " + reader.getClass().getName() + " to read.");
				readerClassName = reader.getClass().getName();
				// get the format we are reading
				readerFormat = reader.getFormatName();
				break;
			}

			if (reader == null) {
				System.err.println("Unable to find a reader!");
				// System.exit(1); // error return
				// return null;
				throw new IOException("ImageUtils.fullRead() Unable to find a reader!");
			}

			String imageFormatName = readerFormat;
			reader.setInput(iis, true);

			numImages = 1;
			// numImages = reader.getNumImages(true);
			if (debug) {
				System.out.println("\nThe file contains " + numImages + " image" + (numImages == 1 ? "" : "s") + ".");
				System.out.println();
			}

			sm = reader.getStreamMetadata();

			/**/} catch (IOException ioe) {
			System.out.println("I/O exception");
			// System.exit(1); // error return
			throw new IOException("ImageUtils.fullRead() exception reading stream metadata!");
		} /**/

		if (sm == null) {
			if (debug)
				System.out.println("The file contains no stream metadata.");
		} else {
			if (debug)
				System.out.println("has Stream metadata");
			// String nativeFormatName = sm.getNativeMetadataFormatName();
			// displayMetadata(sm.getAsTree(nativeFormatName));
		}

		// flag to decide if we get as BufferedImage or RenderedImage

		/*
		 * * for now user must set this explictly
		 */
		/*
		 * if (readerFormat.equalsIgnoreCase("vicar") ||
		 * readerFormat.equalsIgnoreCase("pds") ||
		 * readerFormat.equalsIgnoreCase("isis")) { getAsRenderedImage = true; }
		 * else { getAsRenderedImage = false; }
		 */

		if (getAsRenderedImage) {
			if (debug)
				System.out.println("get as RenderedImage *************** " + readerFormat);
			try {
				ImageReadParam param = reader.getDefaultReadParam();
				// add something to it
				renderedImage = reader.readAsRenderedImage(0, param);
				if (renderedImage instanceof VicarRenderedImage) {
					VicarRenderedImage vri = (VicarRenderedImage) renderedImage;
					vri.setTileWidth(vri.getWidth());
				}
			} catch (IOException ioe) {
				System.out.println("I/O exception !");
				// System.exit(1); // error return
				throw new IOException("ImageUtils.fullRead() exception getting rendered image!");
			}
			// for now try to create the bufferedImage using the RenderedImage

			// bufferedImage =
		} else {

			try {
				bufferedImage = reader.read(0);
			} catch (IOException ioe) {
				System.out.println("I/O exception !");
				// System.exit(1); // error return
				throw new IOException("ImageUtils.fullRead() exception reading buffered Image!");

			}
		}

		// this forces the reader to read in and store the metadata
		try {
			im = reader.getImageMetadata(0);
		} catch (IOException ioe) {
			System.out.println("I/O exception obtaining Image Metadata!");
			// this exception isn't alasys a problem. it is not forwarded
			// the metatadta will just be null
			// System.exit(0);
		}

		if (debug) {
			if (im == null) {
				System.out.println("\nThe file has no Image metadata.");
			} else {
				System.out.println("\nThe file contains Image metadata.");
			}
		}

		if (bufferedImage != null) {
			if (debug)
				printImageInfo(bufferedImage, "fullRead.bufferedImage");

			// 2nd argument is a List of thumbnails
			iioImage = new IIOImage(bufferedImage, null, im);
		} else if (renderedImage != null) {
			if (debug)
				printImageInfo(renderedImage, "fullRead.renderedImage");

			// 2nd argument is a List of thumbnails
			iioImage = new IIOImage(renderedImage, null, im);
		}
		try {
			iis.close();
		} catch (IOException cioe) {
			System.err.println(" Error closing IIS");
		}
		return iioImage;
		/***
		 * for (int i = 0; i < numImages; i++) {
		 * System.out.println("\n---------- Image #" + i + " ----------");
		 * System.out.println();
		 * 
		 * try { int width = reader.getWidth(i); System.out.println("width = " +
		 * width);
		 * 
		 * int height = reader.getHeight(i); System.out.println("height = " +
		 * height);
		 * 
		 * int numThumbnails = reader.getNumThumbnails(i);
		 * System.out.println("numThumbnails = " + numThumbnails);
		 * 
		 * for (int j = 0; i < numThumbnails; j++) {
		 * System.out.println("  width = " + reader.getThumbnailWidth(i, j) +
		 * ", height = " + reader.getThumbnailHeight(i, j)); }
		 * 
		 * // File ff = new File(f); // BufferedImage bufferedImage =
		 * ImageIO.read(f); bufferedImage = reader.read(0); } catch (IOException
		 * ioe) { System.out.println("I/O exception !"); System.exit(0); }
		 * 
		 * 
		 * if (bufferedImage == null) { System.out.println(inputFileName +
		 * " - couldn't read!"); // return; }
		 * 
		 * 
		 * System.out.println("\n ImageToDOM"); ImageToDOM i2dom = new
		 * ImageToDOM ( bufferedImage ); Document d = i2dom.getDocument();
		 * displayMetadata((Node) d); makeFrameDomEcho4((Node) d, bufferedImage
		 * );
		 * 
		 * System.out.println("<**************************************>");
		 * 
		 * System.out.println("\n ImageToPDS_DOM"); ImageToPDS_DOM i2PDSdom =
		 * new ImageToPDS_DOM ( bufferedImage ); Document d1 =
		 * i2PDSdom.getDocument(); displayMetadata((Node) d1);
		 * makeFrameDomEcho4((Node) d1, bufferedImage );
		 * 
		 * System.out.println("<**************************************>");
		 * 
		 * 
		 * // this forces the reader to read in and store the metadata try { im
		 * = reader.getImageMetadata(i); } catch (IOException ioe) {
		 * System.out.println("I/O exception obtaining Image Metadata!"); //
		 * System.exit(0); }
		 * 
		 * if (im == null) {
		 * System.out.println("\nThe file has no Image metadata."); } else {
		 * System.out.println("\nThe file contains Image metadata."); }
		 * 
		 * else { System.out.println("\nImage metadata:"); String
		 * nativeFormatName = im.getNativeMetadataFormatName();
		 * 
		 * Node imNode = im.getAsTree(nativeFormatName); // this could be loaded
		 * into the DomEcho4
		 * 
		 * System.out.println("ImageDumper.displayMetadata() >>>>>>");
		 * displayMetadata(imNode);
		 * System.out.println("<<<<<< ImageDumper.displayMetadata()"); //
		 * makeFrameDomEcho4(d); // makeFrameDomEcho4(imNode); IIOImage iioImage
		 * = new IIOImage(bufferedImage, null, im);
		 * System.out.println("<**************************************>");
		 * String fname = f.getName(); System.out.println(" "); // pass image
		 * AND stream metadata to the DomEcho // we pass it the image so it has
		 * the image to wriiten out to // a file makeFrameDomEcho4(iioImage, sm,
		 * fname); // makeFrameDomEcho4(imNode, bufferedImage);
		 * 
		 * }
		 * 
		 * }
		 * 
		 * // 2nd argument is a List of thumbnails iioImage = new
		 * IIOImage(bufferedImage, null, im); return iioImage;
		 ***/

	}

	/**
	 * prints useful debug information on a the image read in by fullRead()
	 * 
	 */
	public void printImageInfo() {

		if (renderedImage != null) {
			printImageInfo(renderedImage, " stored RenderedImage ");
		} else if (bufferedImage != null) {
			printImageInfo(bufferedImage, " stored BufferedImage ");
		}
	}

	/**
	 * prints useful debug information on a RenderedImage. The description is a
	 * String included in the print.
	 * 
	 * @param im
	 *            the image to display info about
	 * @param description
	 *            a String to be included in the printout
	 */
	public void printImageInfo(RenderedImage im, String description) {

		if (im == null)
			return;

		SampleModel sm = im.getSampleModel();
		ColorModel cm = im.getColorModel();
		int width = im.getWidth();
		int height = im.getHeight();

		int dataType = sm.getDataType();

		System.out.println("RenderedImage " + description + "  -------------------");
		int bands = sm.getNumBands();
		int[] sampleSize = sm.getSampleSize(); // sampleSize[0] equals b0size
		int b0size = sm.getSampleSize(0);
		int elements = sm.getNumDataElements();

		System.out.println("DataBuffer.TYPE_BYTE = " + DataBuffer.TYPE_BYTE);
		System.out.println("DataBuffer.TYPE_SHORT = " + DataBuffer.TYPE_SHORT);
		System.out.println("DataBuffer.TYPE_USHORT = " + DataBuffer.TYPE_USHORT);
		System.out.println("DataBuffer.TYPE_INT = " + DataBuffer.TYPE_INT);
		System.out.println("DataBuffer.TYPE_FLOAT = " + DataBuffer.TYPE_FLOAT);
		System.out.println("DataBuffer.TYPE_DOUBLE = " + DataBuffer.TYPE_DOUBLE);
		System.out.println("dataType " + dataType);
		System.out.println("height=" + height + "  width=" + width + "  bands=" + bands);
		System.out.println("dataElements=" + elements + "  b0size=" + b0size);
		for (int i = 0; i < sampleSize.length; i++) {
			System.out.println(" sampleSize[" + i + "]=" + sampleSize[i]);
		}

	}

	public RenderedImage processFormat(RenderedImage image, int newDataType, double rescaleMin, double rescaleMax) {

		// DataBuffer.TYPE_BYTE
		RenderedImage sourceImage = image;

		ComponentSampleModel sampleModel = (ComponentSampleModel) image.getSampleModel();
		int oldDataType = sampleModel.getDataType();
		if (oldDataType == newDataType)
			return image;

		int numbands = sampleModel.getNumBands();

		// check if (oldDataType == newDataType) return image;
		if (debug)
			System.out.println("processFormat " + numbands + " bands   " + oldDataType + " -> " + newDataType);

		// make a new SampleModel for the new image data type
		// get all the stuff we need to know from the old sampleModel
		int pStride = sampleModel.getPixelStride();
		int slStride = sampleModel.getScanlineStride();
		int[] bandOffsets = sampleModel.getBandOffsets();
		if (debug)
			System.out.println(" *** pStride=" + pStride + "  slStride=" + slStride + "  bandOffsets=" + bandOffsets);
		// int w = sampleModel.getWidth();
		// int h = sampleModel.getHeight();
		// ---------------------------------------------------------

		double max = 0;
		double min = 0;
		double oldCeiling, newCeiling;
		ParameterBlock PB;
		RenderedImage temp = image;

		double scale[][];
		if (rescaleMin == 0.0 && rescaleMax == 0.0) {
			// rescale the pixel values of the image based on the image extrema

			PB = new ParameterBlock();
			PB.addSource(temp).add(null).add(10).add(10);
			RenderedImage extrema = JAI.create("extrema", PB);

			// scale all pixels by: v1= m * v0 +b (assuming one band per pixel)
			scale = (double[][]) extrema.getProperty("extrema");
			// double ceiling=Byte.MAX_VALUE*2; // treat as unsigned
			oldCeiling = getMaxForDataType(oldDataType);
			newCeiling = getMaxForDataType(newDataType);

			// double ceiling=Short.MAX_VALUE*2;
			max = 0;
			min = oldCeiling;
			for (int i = 0; i < scale[0].length; i++) {
				if (debug)
					System.out.println(i + ") scale[0][i] " + scale[0][i] + "   scale[1][i] " + scale[1][i]);
				if (scale[1][i] > 0) {
					max = Math.max(max, scale[1][i]);
				} else {
					max = scale[1][i];
				}
				min = Math.min(min, scale[0][i]);
			}
			if (debug)
				System.out.println("processFormat extrema ceiling=" + oldCeiling + "  min=" + min + "  max=" + max);
			// round max up to the nearest power of 2.
			// max=Math.pow(2.0,Math.round(Math.log(max)/Math.log(2)));
			// min=0;
		} else {
			// use what the user provided on the command line
			min = rescaleMin;
			max = rescaleMax;
			newCeiling = max;
		}

		// this will be for BYTE output
		double constant[] = new double[] { 1.0 };
		double offset[] = new double[] { 0.0 };

		double delta = 0.0;
		if (min < 0 && max == 0) {
			delta = Math.abs(min);
		} else {
			delta = max - min;
		}

		constant[0] = newCeiling / delta;
		offset[0] = min * constant[0] * -1.0; // offset is added only for
												// unsigned ??
		// offset[0] = min * -1.0; // offset is added only for unsigned ??

		if (debug) {
			System.out.println("processFormat constant=" + constant[0] + "  offset=" + offset[0] + "  delta=" + delta);

			double min1 = (min * constant[0]) + offset[0];
			double max1 = (max * constant[0]) + offset[0];
			System.out.println("processFormat  min=" + min + "  min1=" + min1 + "  max=" + max + "  max1=" + max1);
		}

		PB = new ParameterBlock();
		// PB.addSource(temp).add(new double[]{ceiling/(max-min)}).add(new
		// double[]{ceiling*min/(min-max)});
		PB.addSource(temp).add(constant).add(offset);
		temp = JAI.create("rescale", PB);

		if (debug) {
			// do extrema again after the rescale

			PB = new ParameterBlock();
			PB.addSource(temp).add(null).add(10).add(10);
			RenderedImage extrema = JAI.create("extrema", PB);

			// scale all pixels by: v1= m * v0 +b (assuming one band per pixel)
			scale = (double[][]) extrema.getProperty("extrema");
			// ceiling=Short.MAX_VALUE*2;
			max = 1;
			min = newCeiling;
			for (int i = 0; i < scale[0].length; i++) {
				System.out.println(i + ")new  scale[0][i] " + scale[0][i] + "   scale[1][i] " + scale[1][i]);
				max = Math.max(max, scale[1][i]);
				min = Math.min(min, scale[0][i]);
			}
			System.out.println("processFormat new extrema  min=" + min + "  max=" + max);
		}

		image = temp;

		ParameterBlock pb = new ParameterBlock();
		pb.addSource(image);
		pb.add(newDataType);

		// RenderedImage formatedImage = JAI.create("format", pb, hints);
		RenderedImage formatedImage = JAI.create("format", pb);
		return (formatedImage);
		// 0 Flip_vertical
		// 1 Flip_horizontal
		// RenderedImage flippedImage = JAI.create("transpose", formatedImage,
		// 0);
		// return (flippedImage);
	}

	/**
	 * convenience for using format/rescale operators.
	 * 
	 * @param dataType
	 * @return double. The naximum value for the dataType
	 */
	public double getMaxForDataType(int dataType) {

		double max = 0.0;
		if (dataType == DataBuffer.TYPE_BYTE) {
			max = Byte.MAX_VALUE * 2;// used as unsigned
		} else if (dataType == DataBuffer.TYPE_SHORT) {
			max = Short.MAX_VALUE;
		} else if (dataType == DataBuffer.TYPE_USHORT) {
			max = Short.MAX_VALUE * 2;
		} else if (dataType == DataBuffer.TYPE_INT) {
			max = Integer.MAX_VALUE; // or 0.0 ?? // assume unsigned ???
		} else if (dataType == DataBuffer.TYPE_FLOAT) {
			max = Float.MAX_VALUE;
		} else if (dataType == DataBuffer.TYPE_DOUBLE) {
			max = Double.MAX_VALUE;
		}

		return max;
	}
}
