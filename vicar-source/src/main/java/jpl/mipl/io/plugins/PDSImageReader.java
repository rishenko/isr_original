/*
 * @(#)PDSImageReader.java	1.0 00/08/30
 *
 * Steve Levoe
 * Jet Propulsion Laboratory
 * Multimission Image Processing Laboratory
 * 12-2000 ImageIO EA2 version
 * 12-1-03 new version which chooses between Native and regular 
 * PDSInputFile
 */

package jpl.mipl.io.plugins;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;

import jpl.mipl.io.streams.DataInputStreamWrapper;
import jpl.mipl.io.vicar.PDSInputFile;
import jpl.mipl.io.vicar.PDSNativeInputFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.SeekableStream;

/**
 import java.awt.image.DataBufferByte;
 import java.awt.image.DataBufferInt;
 import java.awt.image.DataBufferUShort;
 import java.awt.image.IndexColorModel;
 import java.awt.image.MultiPixelPackedSampleModel;
 import java.awt.image.PixelInterleavedSampleModel;
 import java.io.File;
 **/
// EA2
// import javax.media.imageio.metadata.ImageMetadata; // EA1
// import javax.media.imageio.metadata.StreamMetadata; // EA1
// import javax.media.imageio.stream.FileImageInputStream; // EA1
/**
 import com.sun.imageio.plugins.common.InputStreamAdapter; // EA2
 import com.sun.imageio.plugins.common.SubImageInputStream; // EA2

 // added to example 
 import java.io.InputStream;
 **/
// import VicarIO stuff
// VicarInputFile  SystemLabel
// needed for createColorModel()
// SeekableStream is in jai - we must remove dependancy on jai
// maybe switch to the ImageInputStream

/* static final VicarType[] types = {
 static final int BYTE = 0;
 static final int HALF = 1;
 static final int FULL = 2;
 static final int REAL = 3;
 static final int DOUB = 4;
 static final int COMP = 5;
 }; */

/**
 * This class is an <code>ImageReader</code> for reading image files in the
 * Vicar format.
 * 
 * @version 0.6
 */
public class PDSImageReader extends JPLImageReader {

	private static final Logger logger = LoggerFactory.getLogger(PDSImageReader.class);

	private boolean imageTypeRetryFailed = false;
	/**
	 * 
	 * VicarIO specific variables
	 * 
	 * used to get file info
	 */
	// private VicarInputFile vif; // this may become PDSInputFile or
	// RawInputFile
	private PDSInputFile pif;
	// we need a system label object for all the reader
	// routines

	String filename = null; // can used by the native OAL reader code

	private SeekableStream seekableStream;
	// vicarIO currently uses SeekableStream
	// may transition to ImageInputStream ?????

	// private FileImageInputStream fileStream;
	private ImageInputStream stream;
	private DataInputStreamWrapper inputStreamWrapper;
	DataInputStream pixelStream = null;
	BufferedReader bufferedReader = null;

	FileImageInputStream fileStream = null;

	BufferedImage theImage = null;

	private boolean haveReadHeader = false;

	boolean gotHeader = false;
	boolean gotMetadata = false;

	PDSMetadata pdsMetadata = new PDSMetadata();
	ImageReadParam lastParam = null;

	Document document = null;

	/**
	 * The header is kept as array of Strings, one for each token A comment is
	 * defined as a single token. All tokens are preserved, in the order they
	 * appear in the stream
	 */
	// TODO kmcabee: Is this needed? It's not referenced except for being set
	private List header = new ArrayList();

	// private VicarType vicarType;
	private int type; // Redundant, for convenience
	private int bitDepth; // Redundant, for convenience
	private boolean isBinary; // Redundant, for convenience
	private ImageTypeSpecifier imageType = null; // Redundant, for convenience
	private int width;
	private int height;
	private int maxGray;
	private long streamPos;

	/**
	 * Constructor taking an ImageReaderSpi required by ImageReaderSpi.
	 */
	public PDSImageReader(ImageReaderSpi mySpi) {
		super(mySpi);
		logger.debug("constructor");
	}

	public void setInput(Object input, boolean isStreamable, boolean ignoreMetadata) {

		super.setInput(input, isStreamable, ignoreMetadata);
		setInputInternal(input);
		// this.setInput(input, isStreamable);
	}

	public void setInput(Object input, boolean isStreamable) {
		logger.debug("setInput");
		super.setInput(input, isStreamable, false); // true for ignoreMetadata??
		setInputInternal(input);

	}

	public void setInput(Object input) {
		super.setInput(input, false, false);
		setInputInternal(input);
	}

	/**
	 * Enforce that the input must be an <code>ImageInputStream</code>
	 */
	public void setInputInternal(Object input) {
		/**
		 * we will need to see if the VicarIO needs to be changed to use
		 * ImageInputStream
		 */
		logger.debug("input {}", input);
		if (input instanceof ImageInputStream) {
			logger.debug("input is instanceof ImageInputStream");
			this.stream = (ImageInputStream) input;

			// the ISIS reader wants BufferedReader or ImageInputStream
			// anything which has a readLine() method
			// this.bufferedReader = new BufferedReader(new
			// InputStreamReader(input)) ;
			// VicarIO wants an InputStream
			// ImageInputStream extends DataInput
			this.inputStreamWrapper = new DataInputStreamWrapper((DataInput) input);
		} else if (input instanceof FileImageInputStream) {
			logger.debug("input is instanceof FileImageInputStream");
			this.stream = (ImageInputStream) input;
			// the ISIS reader wants BufferedReader or ImageInputStream
			// anything which has a readLine() method
			// this.bufferedReader = new BufferedReader(new
			// InputStreamReader(input)) ;
			// VicarIO wants an InputStream
			// ImageInputStream extends DataInput
			this.inputStreamWrapper = new DataInputStreamWrapper((DataInput) input);
		} else if (input instanceof String) {
			logger.debug("input is instanceof String");
			FileImageInputStream fileStream;
			try {
				fileStream = new FileImageInputStream(new File((String) input));
				// this filename will be used by the native OAL libraries
				filename = (String) input;
				// do I need to close this stream??
			} catch (FileNotFoundException fnfe) {
				logger.error("file not found", fnfe);
				fileStream = null;
				filename = null;
			} catch (IOException ioe) {
				logger.error("io error ", ioe);
				fileStream = null;
				filename = null;
			}
			stream = fileStream;
		} else {

			logger.debug("input is NOT instanceof ImageInputStream ---------- using SeekableStream");
			// this is the input type that the vicarIO lib currently expects
			this.seekableStream = (SeekableStream) input;
			// throw new IllegalArgumentException();
		}
	}

	/**
	 * This method returns a RenderedImage. This is useful in at least 2
	 * situations. 1) The data for a RenderedImage is not grabbed until asked
	 * for by an application. A tiled image will only grab the tiles needed. In
	 * a file copy the whole image wouldn't need to be loaded into memory. It
	 * could be copied tile by tile. 2) The image has more than 3 bands. It
	 * can't be returned as a BufferedImage since no ColorModel exists for > 3
	 * bands. The user can extract bands for display using ImageOps.
	 * 
	 * added Steve Levoe 2-2003
	 */
	public RenderedImage readAsRenderedImage(int imageIndex, ImageReadParam param) throws IIOException {
		if (imageIndex != 0) {
			logger.error("no imageIndex");
			throw new IndexOutOfBoundsException();
		}
		if (haveReadHeader == false) {
			readHeader();
		}
		// printParam(param);
		// look at the param, decide what to do.
		// for now ignore it
		// boolean parameterizedRead = false;

		// create a VicarRenderedImage using the input stream and the
		// SystemLabel obtained by readHeader()
		VicarRenderedImage image = null;
		if (imageIndex != 0) {
			String errorMsg = "Illegal page requested from a Vicar image.";
			logger.error(errorMsg);
			throw new IIOException(errorMsg);
		}

		try {
			image = new VicarRenderedImage(pif, param);
		} catch (Exception e) {
			logger.error("error creating VicarRenderedImage", e);
		}

		logger.debug("vif {}, image {}", new Object[] { pif, image });
		return image;
	}

	/**
	 * Reads the entire header, storing all header data, including comments,
	 * into a list of tokens. Each comment, to the end of the line where it
	 * occurs, is considered a single token.
	 */
	private void readHeader() throws IIOException {

		logger.debug("PDSImageReader.readHeader");
		logger.debug("input={}, filename={}, stream={}, stream class name={}", new Object[] { input, filename, stream,
				stream.getClass().getName() });

		if (stream == null && seekableStream == null && inputStreamWrapper == null && filename == null) {
			String errorMsg = "Input stream not set.";
			logger.debug(errorMsg);
			throw new IllegalStateException(errorMsg);
		}

		try {
			/*
			 * check if PDSNativeInputFile check if Native OAL library can be
			 * loaded Then proceed should be a flag passed in to disable or
			 * force attempt to try Native
			 */

			if (filename != null) {
				PDSNativeInputFile pnif = new PDSNativeInputFile();
				logger.debug("try Native");
				// returns true if the Native library
				// can be found
				if (pnif.loadOalLib()) {
					logger.debug("PDSImageReader pnif.loadOalLib() returned TRUE");

					pif = pnif;
					// native overloaded methods SHOULD be called
				}

				// vif.open( stream); // vif should be able to deal with
				// ImageInputStream
				try {
					pif.open(filename);
				} catch (java.lang.UnsatisfiedLinkError ule) {
					logger.error("java.lang.UnsatisfiedLinkError", ule);
					logger.error("using pure java ");

					// create a stream from the file and read with pure java
					pif = new PDSInputFile();
					FileInputStream fis = new FileInputStream(new File(filename));
					logger.debug("using pure java fis {}", fis);
					pif.open(fis);

				}
			} else if (stream != null) {
				logger.debug("stream {} *@#$%^&*", stream.getClass().getName());
				pif = new PDSInputFile();
				pif.open(stream);
			} else if (inputStreamWrapper != null) {

				logger.debug("inputStreamWrapper {}", inputStreamWrapper.getClass().getName());

				// vif.open(inputStreamWrapper);
				pif = new PDSInputFile();
				pif.open(inputStreamWrapper);
				// public void open(InputStream is) throws IOException
				// public synchronized void open(InputStream is, boolean
				// sequential_only) throws IOException
			} else if (seekableStream != null) {

				logger.debug("seekableStream {}", seekableStream.getClass().getName());

				// vif.open(seekableStream);
				pif = new PDSInputFile();
				pif.open(seekableStream);
			}

			logger.debug("pif.getSystemLabel()");
			setSystemLabel(pif.getSystemLabel());

			String format = getSystemLabel().getFormat();
			String org = getSystemLabel().getOrg();
			int nb = getSystemLabel().getNB();

			if (format.equals("USHORT") && org.equals("BSQ") && nb == 1) {
				int biType = BufferedImage.TYPE_USHORT_GRAY;
				imageType = ImageTypeSpecifier.createFromBufferedImageType(biType);
				logger.debug("imageType {}", imageType);
			} else if (format.equals("BYTE") && org.equals("BSQ") && nb == 1) {
				int biType = BufferedImage.TYPE_BYTE_GRAY;
				imageType = ImageTypeSpecifier.createFromBufferedImageType(biType);
				logger.debug("imageType {}", imageType);
			} else {

				// this handles "HALF"
				SampleModel sampleModel = pif.createSampleModel(width, height);
				ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

				logger.debug("after pif.createSampleModel() ");
				logger.debug("sampleModel {}", sampleModel);
				logger.debug("colorModel {}", colorModel);

				try {
					if (imageType == null && imageTypeRetryFailed == false) {
						imageType = new ImageTypeSpecifier(colorModel, sampleModel);
					}
				} catch (IllegalArgumentException iae) {

					logger.debug("ImageTypeSpecifier error", iae);
					imageType = null;
				}

				/**
				 * // RenderedImage ri if (imageType == null &&
				 * imageTypeRetryFailed == false) { logger.debug(
				 * "imageType == null try using ImageTypeSpecifier from RenderedImage"
				 * ); RenderedImage ri = readAsRenderedImage(0, null); try {
				 * imageType = new ImageTypeSpecifier(ri) ; } catch
				 * (IllegalArgumentException iae) { logger.debug(
				 * "PDSImageReader.readHeader() ImageTypeSpecifier from RenderedImage"
				 * ); logger.debug("IllegalArgumentException "+iae); imageType =
				 * null; } if (imageType == null) { logger.debug(
				 * "****************************************************");
				 * logger.debug("*");
				 * logger.debug("* imageType == null RETRY failed ");
				 * logger.debug("*"); logger.debug(
				 * "****************************************************");
				 * imageTypeRetryFailed = true; } }
				 **/
			}

			logger.debug("PDSImageReader.readHeader() after pif.open() !@#$%^&*()+");
			logger.debug("System label: {}", getSystemLabel());
			logger.debug("PDSFile opened OK");

			// input.close(); // we keep the stream around so we can read in the
			// data
		}
		/**
		 * catch (IOException ex) {
		 * logger.debug("IOException Error reading header:"+ex.getMessage());
		 * ex.printStackTrace(); return; }
		 **/

		catch (Exception ex) {
			logger.error("Exception Error reading header", ex);
			ex.printStackTrace();
			return;
		}

		document = pif.getPDSDocument(); // should use accessor instead
		if (document != null) {
			logger.debug("PDSImageReader new PDSMetadata with document");
			pdsMetadata = new PDSMetadata(document);
			gotMetadata = true;
		} else {
			logger.debug("no document avaiable from pif - PDSImageReader NO PDSMetadata");
			return;
		}

		// PDSLabelToDOM(BufferedReader input, PrintWriter output);
		// creaste a BufferedReader from whatever input type we have

		/***
		 * BufferedReader input = null; if (stream instanceof ImageInputStream)
		 * { PDSLabelToDOM pdsLabel2Dom = new PDSLabelToDOM((ImageInputStream)
		 * stream, null); document = pdsLabel2Dom.getDocument();
		 * logger.debug("PDSImageReader new PDSMetadata with document");
		 * logger.debug("+++++++++++++++++++++++++++++++++++++++++++++++");
		 * pdsMetadata = new PDSMetadata(document); gotMetadata = true; //
		 * return ; } else {
		 * logger.debug("Improper input type, can't read the header "+input);
		 * logger.debug("PDSImageReader NO PDSMetadata ************"); return; }
		 * 
		 * logger.debug("+++++++++++++++++++++++++++++++++++++++++++++++");
		 ***/
		haveReadHeader = true;
	}

	/**
	 * Just get the VicarLabel Object from the read. Put the VicarLabel into the
	 * VicarMetadata Object held by "this" (the reader). The metadata trees will
	 * be generated when they are requested from the VicarMetadata class.
	 **/
	private void readMetadata() throws IIOException {
		if (gotMetadata) {
			return;
		}

		if (haveReadHeader == false) {
			readHeader();
		}
	}

	public String getFormatName() throws IIOException {
		logger.debug("PDSImageReader.getFormatName");
		return "pds";
	}

	public int getNumImages() throws IIOException {
		logger.debug("PDSImageReader.getNumImages");
		return 1; // Vicar always have just 1 ???
		// at least that's all we support now
	}

	public int getWidth(int imageIndex) throws IIOException {
		logger.debug("PDSImageReader.getWidth({})", imageIndex);
		if (imageIndex != 0) {
			throw new IndexOutOfBoundsException();
		}
		if (haveReadHeader == false) {
			readHeader();
		}
		// return width;
		logger.debug("width=" + getSystemLabel().getNS());
		return getSystemLabel().getNS();
	}

	public int getHeight(int imageIndex) throws IIOException {
		logger.debug("PDSImageReader.getHeight({})", imageIndex);
		if (imageIndex != 0) {
			throw new IndexOutOfBoundsException();
		}
		if (haveReadHeader == false) {
			readHeader();
		}
		// return height;
		logger.debug("height=" + getSystemLabel().getNL());
		return getSystemLabel().getNL();
	}

	// I think these are not useful
	public ImageTypeSpecifier getRawImageType(int imageIndex) throws IIOException {
		logger.debug("PDSImageReader.getRawImageType");
		if (imageIndex != 0) {
			throw new IndexOutOfBoundsException();
		}
		if (haveReadHeader == false) {
			readHeader();
			// imageType is set in ReadHeader
		}

		// figure out what this means in the vicar context
		return imageType;
	}

	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IIOException {
		logger.debug("PDSImageReader.getImageTypes");
		if (imageIndex != 0) {
			throw new IndexOutOfBoundsException();
		}
		if (haveReadHeader == false) {
			readHeader();
		}
		ArrayList<ImageTypeSpecifier> list = new ArrayList<ImageTypeSpecifier>();
		list.add(imageType);
		return list.iterator();
	}

	public int getNumImages(boolean allowSearch) throws IIOException {
		logger.debug("PDSImageReader.getNumImages");
		if (stream == null) {
			throw new IllegalStateException("No input source set!");
		}
		return 1;
	}

	/**
	 * Uses the default implementation of ImageReadParam.
	 */
	public ImageReadParam getDefaultReadParam() {
		logger.debug("PDSImageReader.getDefaultReadParam");
		return new ImageReadParam();
	}

	/**
	 * Since there is only ever 1 image, there is no clear distinction between
	 * image metadata and stream metadata, so just use image metadata and always
	 * return null for stream metadata.
	 */
	public IIOMetadata getStreamMetadata() throws IIOException {
		logger.debug("PDSImageReader.getStreamMetedata");
		return null;
	}

	public IIOMetadata getImageMetadata(int imageIndex) throws IIOException {
		logger.debug("imageIndex {}", imageIndex);
		if (imageIndex != 0) {
			throw new IndexOutOfBoundsException("imageIndex != 0!");
		}
		readMetadata(); // make sure vicarMetadata has valid data in it
		return pdsMetadata;
	}

	public void printParam(ImageReadParam param) {
		if (param == null) {
			logger.debug("param = null");
		} else {
			logger.debug("param {}", param);
			Rectangle sourceRegion = param.getSourceRegion();
			if (sourceRegion != null) {
				logger.debug("sourceRegion {},{} {}x{}", new Object[] { sourceRegion.x, sourceRegion.y,
						sourceRegion.width, sourceRegion.height });
			} else {
				logger.debug("sourceRegion is null");
			}
			logger.debug("param.sourceXSubsampling ", param.getSourceXSubsampling());
			logger.debug("param.sourceYSubsampling ", param.getSourceYSubsampling());
			logger.debug("param.subsamplingXOffset ", param.getSubsamplingXOffset());
			logger.debug("param.subsamplingYOffset ", param.getSubsamplingYOffset());
		}
		logger.debug("------------------------------------------");
	}

	/**
	 * This implementation performs a simple read, leaving any ImageReadParam
	 * subsampling to be performed by a postprocess, which has not yet been
	 * implemented. There are currently problems with the color code that appear
	 * to be bugs in the Java2D image creation and display chain. Only bitmap
	 * and grayscale images can be read. May 11, 2000 REV.
	 */
	public BufferedImage read(int imageIndex, ImageReadParam param)

	throws IIOException {
		logger.debug("imageindex {}", imageIndex);

		if (imageIndex != 0) {
			throw new IndexOutOfBoundsException();
		}
		if (haveReadHeader == false) {
			readHeader();
		}

		printParam(param);
		boolean parameterizedRead = false;

		logger.debug("after readHeader()");
		/**
		 * create a BufferedImage for the entire image tiling stuff will come
		 * later
		 **/

		// get these values from ?????
		int width = getSystemLabel().getNS();
		int height = getSystemLabel().getNL();
		int imageWidth = width;
		int imageHeight = height;
		int startX = 0; // starting x to begin reading FROM the file
		int startY = 0; // starting y to begin reading FROM the file
		int x_off = 0; // x offset into sample model to place data (origin)
		int y_off = 0; // y offset into sample model to place data (origin)

		logger.debug("image is {}x{}", new Object[] { width, height });

		logger.debug("imageType {}", imageType);
		BufferedImage theImage = null;
		SampleModel sampleModel = null;
		ColorModel colorModel = null;

		if (param != null) {
			Rectangle sourceRegion = param.getSourceRegion();
			if (sourceRegion != null) {
				width = sourceRegion.width;
				height = sourceRegion.height;
				// origin = new Point(sourceRegion.x,sourceRegion.y);
				startX = sourceRegion.x;
				startY = sourceRegion.y;
				parameterizedRead = true;
			}
		}

		if (parameterizedRead == false && imageType != null) {
			{
				logger.debug("imageType.createBufferedImage ");
				logger.debug("imageType {}", imageType);
			}
			// let imageTypeSpecifier create the buffered iamge for us
			theImage = imageType.createBufferedImage(width, height);
			sampleModel = theImage.getSampleModel();
			colorModel = theImage.getColorModel();
		} else {

			// get the SampleModel ColorModel Raster and data buffer from
			// vicarIO

			logger.debug("---------------------------------------------");
			logger.debug("pif.createSampleModel({},{})", new Object[] { width, height });

			sampleModel = pif.createSampleModel(width, height);
			logger.debug("sampleModel {}", sampleModel);
		}

		// public BufferedImage createBufferedImage(int width, int height) {
		WritableRaster raster = Raster.createWritableRaster(sampleModel, new Point(0, 0));
		// WritableRaster raster = Raster.createWritableRaster(sampleModel,
		// origin);

		// get the number of bands
		int bands = sampleModel.getNumBands();
		int dataType = sampleModel.getDataType();

		if (bands <= 3) {

			if (dataType == DataBuffer.TYPE_SHORT) {
				// what do we do with SHORT ?? the convenience method can't
				// create a colorModel
				// to override the choice of the ColorModel
				if (bands == 3) {
					int[] bits = { 16, 16, 16 };

					ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
					colorModel = new ComponentColorModel(colorSpace, bits, false, false, Transparency.TRANSLUCENT,
							DataBuffer.TYPE_SHORT);
				} else if (bands == 1) {
					int[] bits = { 16 };

					ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
					colorModel = new ComponentColorModel(colorSpace, bits, false, false, Transparency.TRANSLUCENT,
							DataBuffer.TYPE_SHORT);
				}

			} else {
				// colorModel =
				// ImageCodec.createComponentColorModel(sampleModel);

				// this is a dependancy on JAI - do this by han instead of using
				// a Convenience method
				// go find PlanarImage source ??
				colorModel = PlanarImage.createColorModel(sampleModel);
			}

			logger.debug("bands={} colorModel={}", new Object[] { bands, colorModel });
			// can't create a colorModel for something with a weird

			if (colorModel == null) {
				logger.error("ERROR  PDSImageReader.read() colorModel is null. Can't create BufferedImage");
				throw new IllegalStateException(
						"ERROR  PDSImageReader.read() colorModel is null. Can't create BufferedImage");
			} else {
				theImage = new java.awt.image.BufferedImage(colorModel, raster, colorModel.isAlphaPremultiplied(),
						new Hashtable<String, Object>());
			}
		} else { // bands > 3 can't create a color model
			// get a BufferedImage so we can still return something

			logger.debug("bands={} colorModel={}", new Object[] { bands, colorModel });

			// create a child raster using only 3 bands
			int parentX = raster.getMinX();
			int parentY = raster.getMinY();
			int w = raster.getWidth();
			int h = raster.getHeight();
			int childMinX = parentX;
			int childMinY = parentY;
			int[] bandList = { 0, 1, 2 };

			// the child should SHARE the parent's raster
			// we will read data into the parent raster
			// the BuffereImgae will be from the child so a ColorModel can be
			// created
			WritableRaster childRaster = raster.createWritableChild(parentX, parentY, w, h, childMinX, childMinY,
					bandList);

			// is the sampleModel valid ??
			// SampleModel childSM = new SampleModel();
			logger.debug("sampleModel {}", sampleModel);

			colorModel = ImageCodec.createComponentColorModel(sampleModel);
			logger.debug("colorModel {}", colorModel);
			if (colorModel == null) {
				dataType = sampleModel.getDataType();
				w = sampleModel.getWidth();
				h = sampleModel.getHeight();
				int b = 3;

				BandedSampleModel fakeSampleModel = new BandedSampleModel(dataType, w, h, b);
				// logger.debug("childSM "+ childSM);

				// create a bogus colorModel just to get by
				// colorModel =
				// ImageCodec.createComponentColorModel(fakeSampleModel);
				colorModel = PlanarImage.createColorModel(fakeSampleModel);
				logger.debug("colorModel (fake) {}", colorModel);

				// childRaster
				theImage = new java.awt.image.BufferedImage(colorModel, childRaster, colorModel.isAlphaPremultiplied(),
						new Hashtable<String, Object>());

				logger.debug("theImage {}", theImage);

			}

		}

		DataBuffer buf = theImage.getRaster().getDataBuffer();

		// this tile happens to be the whole image
		logger.debug("PDSImageReader.read() >>> pif.readTile >>>>>>>");
		try {
			if (startX != 0 || startY != 0 || width < imageWidth || height < imageHeight) {
				// read a subarea of the image
				logger.debug("readTile {},{} {}x{} from {}x{}", new Object[] { startX, startY, width, height,
						imageWidth, imageHeight });
				pif.readTile(startX, startY, width, height, x_off, y_off, sampleModel, buf);
			} else { // standard read of the entire tile
				logger.debug("readTile BASIC");
				pif.readTile(0, 0, sampleModel, buf);
			}
		} catch (IOException e) {
			String errorMsg = "IOException occured while reading PDS image file";
			logger.error(errorMsg, e);
			throw new RuntimeException(errorMsg);
		}

		logger.debug("PDSImageReader.read() completed");
		logger.debug("sampleModel={}", sampleModel);
		logger.debug("colorModel={}", colorModel);

		return theImage;
	}

	/**
	 * PDS images do not have thumbnails.
	 */
	public int getNumThumbnails(int imageIndex) {
		logger.debug("PDSImageReader.getNumThumbnails");
		return 0;
	}

	public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IIOException {
		logger.error("bad index");
		throw new IndexOutOfBoundsException("Bad thumbnail index!");
	}

	public void reset() {
		super.reset();
		haveReadHeader = false;
		header.clear();
	}

	public void dispose() {
		reset();
		haveReadHeader = false;
		header = null;
	}

}
