package com.astrodoorways.converter.vicar.exif;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.metadata.IIOMetadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.pw.jexif.JExifInfo;
import be.pw.jexif.JExifTool;
import be.pw.jexif.enums.tag.ExifIFD;
import be.pw.jexif.enums.tag.Tag;
import be.pw.jexif.exception.ExifError;
import be.pw.jexif.exception.JExifException;

import com.astrodoorways.converter.jexif.tag.OtherExifTags;
import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.vicar.VicarConstants;

/**
 * A converter that takes a metadata object for a VICAR image file and sets it
 * as the EXIF data on the converted file.
 * 
 * @author kmcabee
 * 
 */
public class VicarThreadedJEXIFConverter {

	private static final Logger logger = LoggerFactory.getLogger(VicarThreadedJEXIFConverter.class);

	private final JExifTool tool;

	public VicarThreadedJEXIFConverter(JExifTool tool) {
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
		JExifInfo info = tool.getInfo(new File(inputFilePath));
		MetadataProcessor metadataProcessor = new MetadataProcessor();
		Map<String, String> valueMap = metadataProcessor.process(metadata);
		logger.trace("value map {}", new Object[] { valueMap });

		try {
			Map<Tag, String> tagMap = new HashMap<Tag, String>();
			tagMap.put(ExifIFD.EXPOSURETIME, valueMap.get(VicarConstants.EXPOSURE));
			tagMap.put(ExifIFD.DATETIMEORIGINAL,
					metadataProcessor.convertVicarDateToExifFormat(valueMap.get(VicarConstants.TIME)));
			tagMap.put(OtherExifTags.LENS, valueMap.get(VicarConstants.CAMERA));
			tagMap.put(OtherExifTags.MODEL, valueMap.get(VicarConstants.FILTER));
			tagMap.put(OtherExifTags.MAKE, valueMap.get(VicarConstants.MISSION));
			tagMap.put(ExifIFD.USERCOMMENT, valueMap.get(VicarConstants.TARGET));
			info.setTagValue(tagMap);
			logger.trace("finished setting tags");
		} catch (ExifError e) {
			logger.error("exif processing error", e);
		} catch (JExifException e) {
			logger.error("exif processing error", e);
		} catch (ParseException e) {
			logger.error("parsing exception while processing exif data", e);
		}

		File origCopy = new File(inputFilePath + "_original");
		origCopy.delete();
	}
}
