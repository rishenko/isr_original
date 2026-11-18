package com.astrodoorways.converter.vicar.exif;

import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.vicar.VicarConstants;
import jpl.mipl.io.plugins.VicarMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.RenderedImage;
import java.util.Map;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.ExifParentTIFFTagSet;
import javax.imageio.plugins.tiff.ExifTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.plugins.tiff.TIFFTagSet;

/**
 * Builds TIFF image metadata carrying the EXIF values for a converted VICAR image, so that the
 * JDK's TIFF writer can write the tags together with the pixels.
 *
 * This replaces VicarThreadedEXIFConverter, which re-wrote every finished TIFF through the icafe
 * library (no longer downloadable). The values are computed exactly as that class computed them.
 * One deliberate difference: when the values cannot be computed (FITS, PDS, and TIFF inputs, whose
 * metadata is not VicarMetadata), the old code replaced the converted image with an empty file;
 * now the image is written without EXIF tags.
 */
public final class TiffExifMetadata {

	private static final Logger logger = LoggerFactory.getLogger(TiffExifMetadata.class);

	/** EXIF 2.3 tags that the JDK's ExifTIFFTagSet (EXIF 2.2) does not define. */
	public static final int TAG_LENS_MAKE = 42035;
	public static final int TAG_LENS_MODEL = 42036;

	private TiffExifMetadata() {
	}

	/** The values written as EXIF, in the form VicarThreadedEXIFConverter produced them. */
	public static final class Values {
		public final long exposureSeconds;
		public final String dateTimeOriginal;
		public final String lensMake;
		public final String lensModel;
		public final String model;
		public final String make;
		public final String comment;

		Values(long exposureSeconds, String dateTimeOriginal, String lensMake, String lensModel, String model,
				String make, String comment) {
			this.exposureSeconds = exposureSeconds;
			this.dateTimeOriginal = dateTimeOriginal;
			this.lensMake = lensMake;
			this.lensModel = lensModel;
			this.model = model;
			this.make = make;
			this.comment = comment;
		}
	}

	/**
	 * Compute the EXIF values for an image.
	 *
	 * @throws Exception when the metadata cannot supply them (same failures as the old converter)
	 */
	public static Values computeValues(IIOMetadata metadata) throws Exception {
		MetadataProcessor metadataProcessor = new MetadataProcessor();
		Map<String, String> valueMap = metadataProcessor.process(metadata);
		long exposure = Double.valueOf(valueMap.get(VicarConstants.EXPOSURE)).intValue();
		String date = metadataProcessor.convertVicarDateToExifFormat(valueMap.get(VicarConstants.TIME));
		String comment = ((VicarMetadata) metadata).getVicarLabel().toString();
		return new Values(exposure, date, valueMap.get(VicarConstants.CAMERA), valueMap.get(VicarConstants.TARGET),
				valueMap.get(VicarConstants.FILTER), valueMap.get(VicarConstants.MISSION), comment);
	}

	/**
	 * @return TIFF image metadata with the EXIF tags, or null (writer defaults) when the values
	 *         cannot be computed for this image
	 */
	public static IIOMetadata build(ImageWriter writer, RenderedImage image, ImageWriteParam param,
			IIOMetadata sourceMetadata) {
		Values values;
		try {
			values = computeValues(sourceMetadata);
		} catch (Exception e) {
			logger.warn("no EXIF tags written; could not compute values from {}: {}",
					sourceMetadata == null ? null : sourceMetadata.getClass().getSimpleName(), e.toString());
			return null;
		}

		try {
			IIOMetadata defaults = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), param);
			TIFFDirectory root = TIFFDirectory.createFromMetadata(defaults);

			addAscii(root, BaselineTIFFTagSet.getInstance().getTag(BaselineTIFFTagSet.TAG_MAKE), values.make);
			addAscii(root, BaselineTIFFTagSet.getInstance().getTag(BaselineTIFFTagSet.TAG_MODEL), values.model);
			addAscii(root, BaselineTIFFTagSet.getInstance().getTag(BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION),
					values.comment);

			TIFFTag exifPointer = ExifParentTIFFTagSet.getInstance().getTag(ExifParentTIFFTagSet.TAG_EXIF_IFD_POINTER);
			TIFFDirectory exif = new TIFFDirectory(new TIFFTagSet[] { ExifTIFFTagSet.getInstance() }, exifPointer);
			exif.addTIFFField(new TIFFField(ExifTIFFTagSet.getInstance().getTag(ExifTIFFTagSet.TAG_EXPOSURE_TIME),
					TIFFTag.TIFF_RATIONAL, 1, new long[][] { { values.exposureSeconds, 1 } }));
			addAscii(exif, ExifTIFFTagSet.getInstance().getTag(ExifTIFFTagSet.TAG_DATE_TIME_ORIGINAL),
					values.dateTimeOriginal);
			addAscii(exif, new TIFFTag("LensMake", TAG_LENS_MAKE, 1 << TIFFTag.TIFF_ASCII), values.lensMake);
			addAscii(exif, new TIFFTag("LensModel", TAG_LENS_MODEL, 1 << TIFFTag.TIFF_ASCII), values.lensModel);
			// the offset is a placeholder; the writer computes the real one
			root.addTIFFField(new TIFFField(exifPointer, TIFFTag.TIFF_LONG, 1L, exif));

			return root.getAsMetadata();
		} catch (IIOInvalidTreeException e) {
			logger.warn("no EXIF tags written; could not build TIFF metadata", e);
			return null;
		}
	}

	private static void addAscii(TIFFDirectory directory, TIFFTag tag, String value) {
		if (value == null) {
			return;
		}
		directory.addTIFFField(new TIFFField(tag, TIFFTag.TIFF_ASCII, 1, new String[] { value }));
	}
}
