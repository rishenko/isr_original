package nom.tam.fits.io.plugins;

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
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.io.plugins.metadata.FitsMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitsImageReader extends ImageReader {

	Logger logger = LoggerFactory.getLogger(FitsImageReader.class);

	private boolean havePreparedFits;
	private ImageInputStream stream;
	private Object imageData;
	private FitsMetadata metadata;
	private int width;
	private int height;
	private int bits;
	private int colorType;

	protected FitsImageReader(ImageReaderSpi originatingProvider) {
		super(originatingProvider);
	}

	@Override
	public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
		super.setInput(input, seekForwardOnly, ignoreMetadata);
		if (input == null) {
			this.stream = null;
			return;
		}
		if (input instanceof ImageInputStream) {
			this.stream = (ImageInputStream) input;
		} else {
			throw new IllegalArgumentException("bad input");
		}
	}

	@Override
	public int getHeight(int arg0) throws IOException {
		try {
			prepareFits();
		} catch (FitsException | IOException e) {
			logger.error("could not prepare FITs file for processing", e);
			throw new IOException("could not prepare FITs file for processing", e);
		}
		return height;
	}

	@Override
	public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
		try {
			if (metadata == null) {
				Fits fits = new Fits(new FitsImageInputStreamWrapper(stream));
				Header header = fits.readFirstImageHeader();
				if (header != null)
					metadata = new FitsMetadata(header);
				else
					logger.error("no image was found for this stream");
				stream.reset();
			}
		} catch (FitsException | IOException e) {
			logger.error("could not prepare FITs file for processing", e);
			throw new IOException("could not prepare FITs file for processing", e);
		}
		return metadata;
	}

	@Override
	public IIOMetadata getStreamMetadata() throws IOException {
		try {
			if (metadata == null) {
				Fits fits = new Fits(new FitsImageInputStreamWrapper(stream));
				Header header = fits.readFirstImageHeader();
				if (header != null)
					metadata = new FitsMetadata(header);
				else
					logger.error("no image was found for this stream");
				stream.reset();
			}
		} catch (FitsException | IOException e) {
			logger.error("could not prepare FITs file for processing", e);
			throw new IOException("could not prepare FITs file for processing", e);
		}
		return metadata;
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) {
		java.util.List<ImageTypeSpecifier> l = new ArrayList<ImageTypeSpecifier>();
		ImageTypeSpecifier imageType = null;
		int datatype = DataBuffer.TYPE_BYTE;

		switch (colorType) {
		case 0:
			imageType = ImageTypeSpecifier.createGrayscale(bits, datatype, false);
			break;

		case 1:
			ColorSpace rgb = ColorSpace.getInstance(ColorSpace.CS_sRGB);
			int[] bandOffsets = new int[3];
			bandOffsets[0] = 0;
			bandOffsets[1] = 1;
			bandOffsets[2] = 2;
			imageType = ImageTypeSpecifier.createInterleaved(rgb, bandOffsets, datatype, false, false);
			break;
		}

		l.add(imageType);
		return l.iterator();
	}

	@Override
	public int getNumImages(boolean arg0) throws IOException {
		return 1;
	}

	@Override
	public int getWidth(int imageIndex) throws IOException {
		try {
			prepareFits();
		} catch (FitsException | IOException e) {
			throw new IllegalStateException("could not prepare FITs file for processing", e);
		}
		return width;
	}

	@Override
	public BufferedImage read(int imageIndex, ImageReadParam arg1) throws IOException {
		int imageType = 0;
		BufferedImage image = null;

		int width = getWidth(0);
		int height = getHeight(0);

		if (getImageData() instanceof byte[][]) {
			imageType = BufferedImage.TYPE_BYTE_GRAY;

		} else if (getImageData() instanceof short[][]) {
			logger.trace("image is unsigned short");
			short[][] imageDataArray = (short[][]) getImageData();
			double[] outgoingImageArray = new double[imageDataArray.length * imageDataArray[0].length];
			logger.trace("converting image from fits to raster");
			for (int x = 0; x < getWidth(0); x++) {
				for (int y = 0; y < getHeight(0); y++) {
					short val = imageDataArray[x][y];
					val = val < 0 ? 0 : val;
					outgoingImageArray[y + x * width] = (double) val;
				}
			}

			DataBuffer dataBuffer = new DataBufferDouble(outgoingImageArray, outgoingImageArray.length);
			SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1, width,
					new int[1]);
			ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
					Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
			WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
			image = new BufferedImage(colorModel, raster, false, null);

		} else if (getImageData() instanceof float[][]) {
			logger.trace("image is floating point");
			float[][] imageDataArray = (float[][]) getImageData();
			double[] outgoingImageArray = new double[imageDataArray.length * imageDataArray[0].length];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					float val = imageDataArray[x][y];
					val = val < 0 ? 0 : val;
					outgoingImageArray[y + x * width] = (double) val;
				}
			}

			DataBuffer dataBuffer = new DataBufferDouble(outgoingImageArray, outgoingImageArray.length);
			SampleModel sampleModel = new PixelInterleavedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1, width,
					new int[1]);
			ColorModel colorModel = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), false, false,
					Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
			WritableRaster raster = Raster.createWritableRaster(sampleModel, dataBuffer, new Point(0, 0));
			image = new BufferedImage(colorModel, raster, false, null);
		}

		logger.trace("finished converting image from fits to raster");
		return image;
	}

	public void prepareFits() throws FitsException, IOException {
		if (!havePreparedFits) {
			havePreparedFits = true;
			Fits fits = new Fits(new FitsImageInputStreamWrapper(stream));
			ImageHDU imageHDU = null;
			logger.trace("reading in fits file");
			for (BasicHDU hdu : fits.read()) {
				logger.trace("reading in hdu");
				if (hdu instanceof ImageHDU) {
					logger.trace("reading in image hdu");
					imageHDU = (ImageHDU) hdu;
					if (imageHDU.getHeader().getLongValue("NAXIS") != 0L
							|| imageHDU.getHeader().getLongValue("NAXIS1") != 0L) {
						logger.trace("proper image hdu");
						break;
					}
				}
			}
			logger.trace("finished reading in fits file");
			if (imageHDU == null) {
				throw new IOException("This FITs file does not contain image data.");
			}
			Header header = imageHDU.getHeader();
			imageData = imageHDU.getData().getKernel();
			metadata = new FitsMetadata(header);
			bits = header.getIntValue("BITPIX");
			colorType = header.getIntValue("NAXIS") == 2 ? 0 : 1;
			height = (int) header.getLongValue("NAXIS1");
			width = (int) header.getLongValue("NAXIS2");
		}
	}

	private Object getImageData() {
		if (imageData == null) {
			try {
				prepareFits();
			} catch (FitsException | IOException e) {
				throw new IllegalStateException("could not prepare FITs file for processing", e);
			}
		}
		return imageData;
	}

}
