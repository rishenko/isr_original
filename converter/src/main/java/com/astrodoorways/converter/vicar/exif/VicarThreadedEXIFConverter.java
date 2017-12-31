package com.astrodoorways.converter.vicar.exif;

import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.vicar.VicarConstants;
import com.icafe4j.image.meta.Metadata;
import com.icafe4j.image.meta.exif.Exif;
import com.icafe4j.image.meta.exif.ExifTag;
import com.icafe4j.image.meta.image.Comments;
import com.icafe4j.image.meta.tiff.TiffExif;
import com.icafe4j.image.tiff.FieldType;
import com.icafe4j.image.tiff.TiffTag;
import jpl.mipl.io.plugins.VicarMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.imageio.metadata.IIOMetadata;

/**
 * A converter that takes a metadata object for a VICAR image file and sets it
 * as the EXIF data on the converted file.
 * 
 * @author kmcabee
 * 
 */
public class VicarThreadedEXIFConverter {

	private static final Logger logger = LoggerFactory.getLogger(VicarThreadedEXIFConverter.class);

	/**
	 * 
	 * @param inputFilePath
	 *            the file to convert with exif data
	 * @param metadata
	 *            the metadata that will be used in the conversion
	 * @throws IOException
	 */

	public void convert(String inputFilePath, IIOMetadata metadata) throws IOException{
		MetadataProcessor metadataProcessor = new MetadataProcessor();
		Map<String, String> valueMap = metadataProcessor.process(metadata);
		logger.trace("value map {}", new Object[] { valueMap });

		File inputFile = new File(inputFilePath);

		// Temporary output file
		File outputFile = new File(inputFile.getParent() + File.separator + "tmp_" + inputFile.getName());
		try (FileInputStream fin = new FileInputStream(inputFile);
			FileOutputStream fout = new FileOutputStream(outputFile)) {

			List<Metadata> metaList = new ArrayList<>();

			// Set the tags
			Exif exif = new TiffExif();
			exif.addExifField(ExifTag.EXPOSURE_TIME, FieldType.RATIONAL, new int[] {Double.valueOf(valueMap.get(VicarConstants.EXPOSURE)).intValue(), 1});
			exif.addExifField(ExifTag.DATE_TIME_ORIGINAL, FieldType.ASCII, metadataProcessor.convertVicarDateToExifFormat(valueMap.get(VicarConstants.TIME)));
			exif.addExifField(ExifTag.LENS_Make, FieldType.ASCII, valueMap.get(VicarConstants.CAMERA));
			exif.addExifField(ExifTag.LENS_MODEL, FieldType.ASCII, valueMap.get(VicarConstants.TARGET));
			exif.addImageField(TiffTag.MODEL, FieldType.ASCII, valueMap.get(VicarConstants.FILTER));
			exif.addImageField(TiffTag.MAKE, FieldType.ASCII, valueMap.get(VicarConstants.MISSION));
			Comments comment = new Comments();
			comment.addComment(((VicarMetadata) metadata).getVicarLabel().toString());

			metaList.add(exif);
			metaList.add(comment);

			// Write the metadata out to the temp file
			Metadata.insertMetadata(metaList, fin, fout);

		} catch (Exception e) {
			logger.error("Error adding metadata for ", inputFile.getAbsolutePath(), e);
		}

		// Override the original file with the temp file. This needs to happen after the file input & output streams are closed
		// To prevent issues on Windows
		Files.move(outputFile.toPath(), inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		Files.deleteIfExists(Paths.get(inputFilePath + "_original"));
	}
}
