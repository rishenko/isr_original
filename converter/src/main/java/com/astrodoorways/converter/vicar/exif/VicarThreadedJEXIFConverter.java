package com.astrodoorways.converter.vicar.exif;

import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.vicar.VicarConstants;
import com.thebuzzmedia.exiftool.ExifTool;
import com.thebuzzmedia.exiftool.Tag;
import com.thebuzzmedia.exiftool.core.StandardTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.metadata.IIOMetadata;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A converter that takes a metadata object for a VICAR image file and sets it
 * as the EXIF data on the converted file.
 * 
 * @author kmcabee
 * 
 */
public class VicarThreadedJEXIFConverter {

	private static final Logger logger = LoggerFactory.getLogger(VicarThreadedJEXIFConverter.class);

	private final ExifTool tool;

	public VicarThreadedJEXIFConverter(ExifTool tool) {
		this.tool = tool;
	}

	/**
	 * 
	 * @param inputFilePath
	 *            the file to convert with exif data
	 * @param metadata
	 *            the metadata that will be used in the conversion
	 * @throws IOException
	 */
	public void convert(String inputFilePath, IIOMetadata metadata) throws IOException {
		MetadataProcessor metadataProcessor = new MetadataProcessor();
		Map<String, String> valueMap = metadataProcessor.process(metadata);
		logger.trace("value map {}", new Object[] { valueMap });

		Map<Tag, String> exifData = new HashMap<>();
		exifData.put(StandardTag.EXPOSURE_TIME, valueMap.get(VicarConstants.EXPOSURE));
		exifData.put(StandardTag.DATE_TIME_ORIGINAL, valueMap.get(VicarConstants.TIME));
		exifData.put(StandardTag.LENS_ID, valueMap.get(VicarConstants.CAMERA));
		exifData.put(StandardTag.MODEL, valueMap.get(VicarConstants.FILTER));
		exifData.put(StandardTag.MAKE, valueMap.get(VicarConstants.MISSION));
		exifData.put(StandardTag.COMMENT, valueMap.get(VicarConstants.TARGET));

		tool.setImageMeta(new File(inputFilePath), exifData);
		logger.trace("finished setting tags");

		File origCopy = new File(inputFilePath + "_original");
		origCopy.delete();
	}
}
