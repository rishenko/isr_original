package nom.tam.fits.io.plugins;

import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageReader;
import javax.imageio.spi.ImageReaderSpi;
import javax.imageio.stream.ImageInputStream;

public class FitsImageReaderSpi extends ImageReaderSpi {

	static final String vendorName = "NASA";
	static final String version = "3";
	static final String readerClassName = "nom.tam.fits.io.plugins.FitsImageReader";
	static final String[] names = { "fits" };
	static final String[] suffixes = { "fits" };
	static final String[] MIMETypes = { "image/fits" };
	static final String[] writerSpiNames = {};

	// Metadata formats, more information below
	static final boolean supportsStandardStreamMetadataFormat = false;
	static final String nativeStreamMetadataFormatName = null;
	static final String nativeStreamMetadataFormatClassName = null;
	static final String[] extraStreamMetadataFormatNames = null;
	static final String[] extraStreamMetadataFormatClassNames = null;
	static final boolean supportsStandardImageMetadataFormat = false;
	static final String nativeImageMetadataFormatName = "nom.tam.fits.io.metadata.FitsMetadata_1.0";
	static final String nativeImageMetadataFormatClassName = "nom.tam.fits.io.metadata.FitsMetadata";
	static final String[] extraImageMetadataFormatNames = null;
	static final String[] extraImageMetadataFormatClassNames = null;

	public FitsImageReaderSpi() {
		super(vendorName, version, names,
				suffixes,
				MIMETypes,
				readerClassName,
				STANDARD_INPUT_TYPE, // Accept ImageInputStreams
				writerSpiNames, supportsStandardStreamMetadataFormat, nativeStreamMetadataFormatName,
				nativeStreamMetadataFormatClassName, extraStreamMetadataFormatNames,
				extraStreamMetadataFormatClassNames, supportsStandardImageMetadataFormat,
				nativeImageMetadataFormatName, nativeImageMetadataFormatClassName, extraImageMetadataFormatNames,
				extraImageMetadataFormatClassNames);
	}

	@Override
	public boolean canDecodeInput(Object input) throws IOException {
		if (!(input instanceof ImageInputStream)) {
			return false;
		}

		ImageInputStream stream = (ImageInputStream) input;
		byte[] b = new byte[6];
		try {
			stream.mark();
			stream.readFully(b);
			stream.reset();
		} catch (IOException e) {
			return false;
		}

		return (b[0] == (byte) 'S' && b[1] == (byte) 'I' && b[2] == (byte) 'M' && b[3] == (byte) 'P'
				&& b[4] == (byte) 'L' && b[5] == (byte) 'E');
	}

	@Override
	public ImageReader createReaderInstance(Object arg0) throws IOException {
		return new FitsImageReader(this);
	}

	@Override
	public String getDescription(Locale arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
