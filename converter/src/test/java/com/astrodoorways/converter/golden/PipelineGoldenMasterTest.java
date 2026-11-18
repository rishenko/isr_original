package com.astrodoorways.converter.golden;

import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.Converter;
import com.astrodoorways.converter.RunConverter;
import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.db.imagery.MetadataDAO;
import org.junit.After;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import javax.imageio.ImageIO;

import static org.junit.Assert.assertEquals;

/**
 * Golden-master tests for the whole conversion pipeline: scan a folder, store FileInfo rows,
 * extract metadata on the metadata executor, then convert on the converter executor.
 *
 * Each test copies a sample folder to a temp directory first, because the pipeline renames
 * input files. The context is rebuilt before every test because ConverterImpl shuts down the
 * shared executors at the end of a run.
 *
 * Expected values in src/test/resources/golden/pipeline come from the original 2018 code.
 * Regenerate with -Dgolden.generate=true (see ImageGoldenMasterTest).
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = RunConverter.class)
// BEFORE each method: a context cached by another test class may already have shut-down executors,
// and ConverterImpl silently drops tasks submitted to them.
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PipelineGoldenMasterTest {

	private static final File GOLDEN_DIR = new File("src/test/resources/golden/pipeline");

	@Autowired
	private Converter converter;

	@Autowired
	private MetadataDAO metadataDAO;

	@org.junit.Before
	public void resetGlobalState() {
		// other test classes in the same JVM may leave these behind
		GoldenRecorder.resetSequencer();
		System.clearProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR);
		System.clearProperty(ApplicationProperties.SEQUENCE);
	}

	@After
	public void clearProperties() {
		System.clearProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR);
		System.clearProperty(ApplicationProperties.SEQUENCE);
	}

	@Test
	public void cassiniNeedsCalibrationUncalibrated() throws Exception {
		run("cassini-needsCalib-plain", "read/data/cassini/needsCalib");
	}

	@Test
	public void cassiniNeedsCalibrationCalibrated() throws Exception {
		Assume.assumeTrue("Cassini calibration files not available", ImageGoldenMasterTest.calibrationDir() != null);
		ApplicationProperties.setProperty(ApplicationProperties.CASSINI_CALIBRATION_DIR,
				new File(ImageGoldenMasterTest.calibrationDir()).getCanonicalPath());
		run("cassini-needsCalib-calibrated", "read/data/cassini/needsCalib");
	}

	@Test
	public void cassiniFolderWithLabelsAndOtherImages() throws Exception {
		run("cassini-1526038904", "read/data/1526038904_1526111192");
	}

	@Test
	public void cassiniAlreadyCalibratedWithSequenceName() throws Exception {
		ApplicationProperties.setProperty(ApplicationProperties.SEQUENCE, "2012_CASS_TEST");
		run("cassini-calibrated-sequence", "read/data/cassini/calibrated");
	}

	/** Three OPEN CL1-CL2 images and one SATURN image: numbers follow image time within each group. */
	@Test
	public void cassiniSequenceIsChronological() throws Exception {
		ApplicationProperties.setProperty(ApplicationProperties.SEQUENCE, "SEQ_TEST");
		run("cassini-1526038904-sequence", "read/data/1526038904_1526111192");
	}

	@Test
	public void huygens() throws Exception {
		run("huygens", "read/huygens");
	}

	@Test
	public void rosettaNac() throws Exception {
		run("rosetta-nac", "read/data/rosetta-nac");
	}

	private void run(String name, String sampleDir) throws Exception {
		assertEquals("golden files were generated in UTC; run tests with -Duser.timezone=UTC", "UTC",
				java.util.TimeZone.getDefault().getID());

		File readDir = Files.createTempDirectory("isr-pipeline-read").toFile();
		File writeDir = Files.createTempDirectory("isr-pipeline-write").toFile();
		try {
			copyDirectory(new File(ImageGoldenMasterTest.TEST_DIRS, sampleDir), readDir);
			GoldenRecorder recorder = new GoldenRecorder()
					.normalizePath(writeDir.getCanonicalPath(), "<WRITE>")
					.normalizePath(writeDir.getAbsolutePath(), "<WRITE>")
					.normalizePath(readDir.getCanonicalPath(), "<READ>")
					.normalizePath(readDir.getAbsolutePath(), "<READ>");

			converter.setReadDirectory(readDir.getAbsolutePath());
			converter.setWriteDirectory(writeDir.getAbsolutePath());
			try {
				converter.beginConversion();
				recorder.put("pipeline.completed", true);
			} catch (Exception e) {
				recorder.putException("pipeline", e);
			}

			// metadata rows, in a stable order
			List<Metadata> rows = new ArrayList<>(metadataDAO.findAll());
			Collections.sort(rows, new Comparator<Metadata>() {
				@Override
				public int compare(Metadata a, Metadata b) {
					return a.getFileInfo().getFileName().compareTo(b.getFileInfo().getFileName());
				}
			});
			recorder.put("db.metadata.count", rows.size());
			for (Metadata row : rows) {
				String key = "db.metadata." + row.getFileInfo().getFileName();
				recorder.put(key + ".camera", row.getCamera());
				recorder.put(key + ".exposure", row.getExposure());
				recorder.put(key + ".filterOne", row.getFilterOne());
				recorder.put(key + ".filterTwo", row.getFilterTwo());
				recorder.put(key + ".mission", row.getMission());
				recorder.put(key + ".target", row.getTarget());
				recorder.putEpochMillis(key + ".time", row.getTime() == null ? null : row.getTime().getTime());
				recorder.put(key + ".outputFileName", row.getFileInfo().getOutputFileName());
			}

			// everything written, with pixel hashes
			TreeSet<String> files = new TreeSet<>();
			ImageGoldenMasterTest.listFiles(writeDir, writeDir, files);
			recorder.put("output.files", files);
			for (String file : files) {
				try {
					BufferedImage image = ImageIO.read(new File(writeDir, file));
					recorder.putImage("output." + file, image);
				} catch (Exception e) {
					recorder.putException("output." + file, e);
				}
			}

			File golden = new File(GOLDEN_DIR, name + ".txt");
			if (ImageGoldenMasterTest.GENERATE) {
				recorder.write(golden);
				return;
			}
			ImageGoldenMasterTest.assertMatches(golden, recorder.getValues());
		} finally {
			ImageGoldenMasterTest.deleteRecursively(readDir);
			ImageGoldenMasterTest.deleteRecursively(writeDir);
		}
	}

	private static void copyDirectory(File from, File to) throws IOException {
		File[] children = from.listFiles();
		if (children == null) {
			return;
		}
		for (File child : children) {
			File target = new File(to, child.getName());
			if (child.isDirectory()) {
				target.mkdirs();
				copyDirectory(child, target);
			} else {
				Files.copy(child.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
}
