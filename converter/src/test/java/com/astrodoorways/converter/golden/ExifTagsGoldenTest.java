package com.astrodoorways.converter.golden;

import com.astrodoorways.converter.converters.VicarImageConverter;
import com.astrodoorways.converter.db.filesystem.FileInfo;
import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.vicar.exif.TiffExifMetadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.ExifParentTIFFTagSet;
import javax.imageio.plugins.tiff.ExifTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFImageReadParam;
import javax.imageio.stream.ImageInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Checks the EXIF tags actually written into each converted TIFF against the values the original
 * code computed (the exif.* lines of the plain-mode image goldens).
 *
 * The original code wrote these tags with icafe in a second pass. Where computing them failed
 * (FITS/PDS/TIFF inputs) it replaced the converted file with an empty one; the new code writes the
 * image without EXIF tags instead, which this test also checks.
 */
@RunWith(Parameterized.class)
public class ExifTagsGoldenTest {

	@Parameters(name = "{0}")
	public static Collection<Object[]> cases() {
		List<Object[]> params = new ArrayList<>();
		File[] goldens = new File(ImageGoldenMasterTest.GOLDEN_DIR, "plain").listFiles();
		for (File golden : goldens) {
			params.add(new Object[] { golden.getName() });
		}
		return params;
	}

	private final String goldenName;

	public ExifTagsGoldenTest(String goldenName) {
		this.goldenName = goldenName;
	}

	@Test
	public void writtenTagsMatchGolden() throws Exception {
		TreeMap<String, String> golden = GoldenRecorder
				.read(new File(ImageGoldenMasterTest.GOLDEN_DIR, "plain/" + goldenName));
		// only cases that produced an output file
		if (!"1".equals(golden.get("convert.outputCount"))) {
			return;
		}

		String casePath = goldenName.substring(0, goldenName.length() - ".txt".length());
		File input = findInput(casePath);
		File writeDir = Files.createTempDirectory("isr-exif").toFile();
		try {
			VicarImageConverter converter = new VicarImageConverter(buildMetadata(input), null,
					writeDir.getAbsolutePath(), "TIFF");
			converter.convert();
			File output = new File(converter.getOutputtedFilePaths().get(0));
			assertTrue("output must not be empty", output.length() > 0);

			TIFFDirectory root = readRootDirectory(output);
			TIFFField exifPointer = root.getTIFFField(ExifParentTIFFTagSet.TAG_EXIF_IFD_POINTER);

			if (golden.containsKey("exif.exception")) {
				assertNull("no EXIF expected when the values cannot be computed", exifPointer);
				return;
			}

			assertEquals(golden.get("exif.make"), ascii(root, BaselineTIFFTagSet.TAG_MAKE));
			assertEquals(golden.get("exif.model"), ascii(root, BaselineTIFFTagSet.TAG_MODEL));
			String description = ascii(root, BaselineTIFFTagSet.TAG_IMAGE_DESCRIPTION);
			assertEquals(golden.get("exif.comment.length"), String.valueOf(description.length()));
			assertEquals(golden.get("exif.comment.sha256"), GoldenRecorder.sha256(description));

			assertNotNull("EXIF sub-directory expected", exifPointer);
			TIFFDirectory exif = exifPointer.getDirectory();
			long[] exposure = exif.getTIFFField(ExifTIFFTagSet.TAG_EXPOSURE_TIME).getAsRational(0);
			assertEquals(golden.get("exif.exposureTime"), exposure[0] + "/" + exposure[1]);
			String date = ascii(exif, ExifTIFFTagSet.TAG_DATE_TIME_ORIGINAL);
			if (!"<NOW>".equals(golden.get("exif.dateTimeOriginal"))) {
				assertEquals(golden.get("exif.dateTimeOriginal"), date);
			}
			assertEquals(golden.get("exif.lensMake"), ascii(exif, TiffExifMetadata.TAG_LENS_MAKE));
			assertEquals(golden.get("exif.lensModel"), ascii(exif, TiffExifMetadata.TAG_LENS_MODEL));
		} finally {
			ImageGoldenMasterTest.deleteRecursively(writeDir);
		}
	}

	private static String ascii(TIFFDirectory directory, int tag) {
		TIFFField field = directory.getTIFFField(tag);
		return field == null ? null : field.getAsString(0);
	}

	private static TIFFDirectory readRootDirectory(File tiff) throws Exception {
		try (ImageInputStream stream = ImageIO.createImageInputStream(tiff)) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
			ImageReader reader = readers.next();
			reader.setInput(stream);
			// LensMake/LensModel are EXIF 2.3 tags the JDK tag sets don't define; read them anyway
			TIFFImageReadParam param = new TIFFImageReadParam();
			param.setReadUnknownTags(true);
			reader.read(0, param);
			TIFFDirectory directory = TIFFDirectory.createFromMetadata(reader.getImageMetadata(0));
			reader.dispose();
			return directory;
		}
	}

	/** Golden names are the case path with '/' replaced by '_'; find the matching sample file. */
	private static File findInput(String flattened) {
		List<File> all = new ArrayList<>();
		collect(ImageGoldenMasterTest.TEST_DIRS, all);
		for (File file : all) {
			String relative = ImageGoldenMasterTest.TEST_DIRS.toURI().relativize(file.toURI()).getPath();
			if (relative.replace('/', '_').equals(flattened)) {
				return file;
			}
		}
		throw new IllegalStateException("no sample file for " + flattened);
	}

	private static void collect(File dir, List<File> files) {
		File[] children = dir.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			if (child.isDirectory()) {
				collect(child, files);
			} else {
				files.add(child);
			}
		}
	}

	private static Metadata buildMetadata(File input) throws Exception {
		FileInfo fileInfo = new FileInfo();
		fileInfo.setFileName(input.getName());
		fileInfo.setExtension(input.getName().substring(input.getName().lastIndexOf('.') + 1));
		fileInfo.setDirectory(input.getParentFile().getCanonicalPath());
		Metadata metadata = new Metadata();
		metadata.setFileInfo(fileInfo);
		return metadata;
	}
}
