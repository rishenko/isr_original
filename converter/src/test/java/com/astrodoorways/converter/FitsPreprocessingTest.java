package com.astrodoorways.converter;

import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.db.imagery.MetadataDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The FITS preprocessing step used to rename each source .fits file to .fz, run funpack, and
 * rename it back after conversion. Without funpack the job aborted with the source left renamed,
 * a failed rename deleted the source, and the converter read a path that no longer existed.
 *
 * These tests run the whole pipeline on two small FITS files and check that the sources are
 * byte-for-byte untouched, every image converts, and no intermediaries are left behind.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = RunConverter.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class FitsPreprocessingTest {

	private static final File FITS_DIR = new File("src/test/resources/test-dirs/read/data/fits");
	private static final List<String> SAMPLES = Arrays.asList("rosat_pspc_rdf2_3_im1.fits", "rosat_pspc_rdf2_3_mex.fits");

	@Autowired
	private Converter converter;

	@Autowired
	private MetadataDAO metadataDAO;

	private File readDir;
	private File writeDir;
	private File toolDir;

	@Before
	public void setUp() throws IOException {
		readDir = Files.createTempDirectory("isr-fits-read").toFile();
		writeDir = Files.createTempDirectory("isr-fits-write").toFile();
		toolDir = Files.createTempDirectory("isr-fits-tool").toFile();
	}

	@After
	public void tearDown() {
		System.clearProperty(ApplicationProperties.FUNPACK_PATH);
		System.clearProperty(ApplicationProperties.PREPROCESSED_PATH);
		deleteRecursively(readDir);
		deleteRecursively(writeDir);
		deleteRecursively(toolDir);
	}

	@Test
	public void missingFunpackConvertsTheOriginals() throws Exception {
		copySamples(".fits");
		ApplicationProperties.setProperty(ApplicationProperties.FUNPACK_PATH, new File(toolDir, "no-such-funpack").getPath());
		Map<String, String> before = hashes(readDir);

		runConverter();

		assertEquals("sources must be untouched", before, hashes(readDir));
		assertConverted(2);
		for (Metadata row : metadataDAO.findAll()) {
			assertNull(row.getFileInfo().getPreprocessedFileName());
		}
	}

	@Test
	public void funpackIntermediaryIsReadAndCleanedUp() throws Exception {
		copySamples(".fits");
		File log = new File(toolDir, "calls.log");
		ApplicationProperties.setProperty(ApplicationProperties.FUNPACK_PATH, fakeFunpack(log).getPath());
		Map<String, String> before = hashes(readDir);

		runConverter();

		assertEquals("sources must be untouched", before, hashes(readDir));
		assertConverted(2);
		List<String> calls = Files.readAllLines(log.toPath());
		assertEquals(2, calls.size());
		for (Metadata row : metadataDAO.findAll()) {
			String intermediary = row.getFileInfo().getPreprocessedFileName();
			assertNotNull(intermediary);
			assertFalse("temporary intermediary should be deleted: " + intermediary, new File(intermediary).exists());
			assertFalse("intermediary must not be in the read folder", intermediary.startsWith(readDir.getPath()));
		}
	}

	@Test
	public void fzFilesAreUnpackedIntoThePreprocessedFolder() throws Exception {
		copySamples(".fz");
		File preprocessed = new File(toolDir, "preprocessed");
		ApplicationProperties.setProperty(ApplicationProperties.PREPROCESSED_PATH, preprocessed.getPath());
		ApplicationProperties.setProperty(ApplicationProperties.FUNPACK_PATH, fakeFunpack(new File(toolDir, "calls.log")).getPath());
		Map<String, String> before = hashes(readDir);

		runConverter();

		assertEquals("sources must be untouched", before, hashes(readDir));
		assertConverted(2);
		// intermediaries in the requested folder are kept
		TreeSet<String> kept = new TreeSet<>(Arrays.asList(preprocessed.list()));
		assertEquals(new TreeSet<>(Arrays.asList("rosat_pspc_rdf2_3_im1.fits", "rosat_pspc_rdf2_3_mex.fits")), kept);
	}

	private void runConverter() throws Exception {
		converter.setReadDirectory(readDir.getAbsolutePath());
		converter.setWriteDirectory(writeDir.getAbsolutePath());
		converter.beginConversion();
	}

	private void assertConverted(int expected) {
		List<Metadata> rows = new ArrayList<>(metadataDAO.findAll());
		assertEquals(expected, rows.size());
		for (Metadata row : rows) {
			String output = row.getFileInfo().getOutputFileName();
			assertNotNull("no output for " + row.getFileInfo().getFileName(), output);
			for (String path : output.split(":::")) {
				assertTrue("missing output " + path, new File(path).isFile());
			}
		}
	}

	private void copySamples(String extension) throws IOException {
		for (String sample : SAMPLES) {
			String name = sample.substring(0, sample.lastIndexOf('.')) + extension;
			Files.copy(new File(FITS_DIR, sample).toPath(), new File(readDir, name).toPath(),
					StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/** A stand-in for funpack that copies the (uncompressed) input to the -O path and logs the call. */
	private File fakeFunpack(File log) throws IOException {
		File script = new File(toolDir, "funpack");
		String body = "#!/bin/sh\n"
				+ "echo \"$@\" >> '" + log.getPath() + "'\n"
				+ "[ \"$1\" = \"-O\" ] || exit 2\n"
				+ "cp \"$3\" \"$2\"\n";
		Files.write(script.toPath(), body.getBytes(StandardCharsets.UTF_8));
		assertTrue(script.setExecutable(true));
		return script;
	}

	private static Map<String, String> hashes(File dir) throws Exception {
		Map<String, String> result = new TreeMap<>();
		for (File file : dir.listFiles()) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			StringBuilder hex = new StringBuilder();
			for (byte b : digest.digest(Files.readAllBytes(file.toPath()))) {
				hex.append(String.format("%02x", b));
			}
			result.put(file.getName(), hex.toString());
		}
		return result;
	}

	private static void deleteRecursively(File file) {
		File[] children = file.listFiles();
		if (children != null) {
			for (File child : children) {
				deleteRecursively(child);
			}
		}
		file.delete();
	}
}
