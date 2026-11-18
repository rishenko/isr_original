package com.astrodoorways.converter;

import com.astrodoorways.converter.db.filesystem.FileInfo;
import com.astrodoorways.converter.db.filesystem.FileInfoDAO;
import com.astrodoorways.converter.db.filesystem.Job;
import com.astrodoorways.converter.db.filesystem.JobDAO;
import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.db.imagery.MetadataDAO;
import com.astrodoorways.converter.metadata.handler.Sequencer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Resuming a job with job.process=&lt;id&gt; after an interruption. The job database used to be
 * in memory and dropped at exit, so there was never a job to resume.
 *
 * The first run converts four images. The test then fakes an interruption: one image's output is
 * left half-written and not recorded, and another image never got its metadata. A second run in a
 * new application context, against the same on-disk database, must redo exactly those two and
 * leave the others alone.
 */
public class ResumeJobTest {

	private static final File SAMPLE_DIR = new File("src/test/resources/test-dirs/read/data/1526038904_1526111192");

	private File readDir;
	private File writeDir;
	private File dbDir;

	@Before
	public void setUp() throws IOException {
		readDir = Files.createTempDirectory("isr-resume-read").toFile();
		writeDir = Files.createTempDirectory("isr-resume-write").toFile();
		dbDir = Files.createTempDirectory("isr-resume-db").toFile();
		for (File file : SAMPLE_DIR.listFiles()) {
			Files.copy(file.toPath(), new File(readDir, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		// sequence names show that a resumed job numbers every image the same way
		ApplicationProperties.setProperty(ApplicationProperties.SEQUENCE, "RESUME_TEST");
	}

	@After
	public void tearDown() {
		System.clearProperty(ApplicationProperties.SEQUENCE);
		System.clearProperty(ApplicationProperties.JOB_ID);
		deleteRecursively(readDir);
		deleteRecursively(writeDir);
		deleteRecursively(dbDir);
	}

	private ConfigurableApplicationContext startContext() {
		// command-line arguments take precedence over application-test.properties
		return new SpringApplicationBuilder(RunConverter.class)
				.profiles("test")
				.run("--spring.datasource.url=jdbc:h2:file:" + new File(dbDir, "converter").getAbsolutePath(),
						"--spring.jpa.hibernate.ddl-auto=update");
	}

	private static void convert(ConfigurableApplicationContext context, File readDir, File writeDir) throws Exception {
		Converter converter = context.getBean(Converter.class);
		converter.setReadDirectory(readDir.getAbsolutePath());
		converter.setWriteDirectory(writeDir.getAbsolutePath());
		converter.beginConversion();
	}

	@Test
	public void resumeRedoesOnlyUnfinishedImages() throws Exception {
		Long jobId;
		Map<String, String> firstRun;
		String halfWritten;
		String noMetadata;

		try (ConfigurableApplicationContext context = startContext()) {
			convert(context, readDir, writeDir);
			List<Job> jobs = new ArrayList<>();
			context.getBean(JobDAO.class).findAll().forEach(jobs::add);
			assertEquals(1, jobs.size());
			jobId = jobs.get(0).getId();

			firstRun = outputs(writeDir);
			assertEquals(4, firstRun.size());

			// fake the interruption
			MetadataDAO metadataDAO = context.getBean(MetadataDAO.class);
			FileInfoDAO fileInfoDAO = context.getBean(FileInfoDAO.class);
			Map<String, Metadata> byFile = new HashMap<>();
			for (Metadata metadata : metadataDAO.findAll()) {
				byFile.put(metadata.getFileInfo().getFileName(), metadata);
			}

			// 1) output started but not finished or recorded
			Metadata first = byFile.get("121811.img");
			halfWritten = outputPath(first);
			Files.write(new File(halfWritten).toPath(), "partial".getBytes(StandardCharsets.UTF_8));
			first.getFileInfo().setOutputFileName(null);
			fileInfoDAO.save(first.getFileInfo());

			// 2) interrupted before its metadata was saved
			Metadata second = byFile.get("121813.img");
			noMetadata = outputPath(second);
			assertTrue(new File(noMetadata).delete());
			FileInfo secondFile = second.getFileInfo();
			secondFile.setOutputFileName(null);
			fileInfoDAO.save(secondFile);
			metadataDAO.delete(second);
		}

		Map<String, Long> modifiedBefore = lastModified(writeDir);
		resetSequencer();
		Thread.sleep(1100); // so a rewritten file gets a later timestamp

		ApplicationProperties.setProperty(ApplicationProperties.JOB_ID, jobId.toString());
		try (ConfigurableApplicationContext context = startContext()) {
			convert(context, readDir, writeDir);

			List<Job> jobs = new ArrayList<>();
			context.getBean(JobDAO.class).findAll().forEach(jobs::add);
			assertEquals("resume must not start a new job", 1, jobs.size());
			List<FileInfo> files = context.getBean(FileInfoDAO.class).findByJob(jobs.get(0));
			assertEquals("resume must not rescan the read folder", 4, files.size());
			List<Metadata> rows = context.getBean(MetadataDAO.class).findAll();
			assertEquals("no duplicate metadata rows", 4, rows.size());
			for (Metadata row : rows) {
				assertNotNull("not converted: " + row.getFileInfo().getFileName(), row.getFileInfo().getOutputFileName());
			}
		}

		// same names and pixels as an uninterrupted run
		assertEquals(firstRun, outputs(writeDir));

		// only the two unfinished images were written again
		Map<String, Long> modifiedAfter = lastModified(writeDir);
		for (Map.Entry<String, Long> entry : modifiedAfter.entrySet()) {
			String path = new File(writeDir, entry.getKey()).getAbsolutePath();
			boolean redone = path.equals(new File(halfWritten).getAbsolutePath())
					|| path.equals(new File(noMetadata).getAbsolutePath());
			if (!redone) {
				assertEquals("should not be rewritten: " + entry.getKey(), modifiedBefore.get(entry.getKey()), entry.getValue());
			}
		}
	}

	private static String outputPath(Metadata metadata) {
		String recorded = metadata.getFileInfo().getOutputFileName();
		return recorded.substring(0, recorded.indexOf(":::"));
	}

	/** relative path to SHA-256 of every file under dir */
	private static Map<String, String> outputs(File dir) throws Exception {
		Map<String, String> result = new TreeMap<>();
		collect(dir, dir, result, true);
		return result;
	}

	private static Map<String, Long> lastModified(File dir) throws Exception {
		Map<String, String> paths = new TreeMap<>();
		collect(dir, dir, paths, false);
		Map<String, Long> result = new TreeMap<>();
		for (String path : paths.keySet()) {
			result.put(path, new File(dir, path).lastModified());
		}
		return result;
	}

	private static void collect(File root, File dir, Map<String, String> result, boolean hash) throws Exception {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				collect(root, file, result, hash);
			} else {
				String relative = root.toPath().relativize(file.toPath()).toString();
				result.put(relative, hash ? sha256(file) : "");
			}
		}
	}

	private static String sha256(File file) throws Exception {
		StringBuilder hex = new StringBuilder();
		for (byte b : MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()))) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}

	private static void resetSequencer() throws Exception {
		Field field = Sequencer.class.getDeclaredField("map");
		field.setAccessible(true);
		synchronized (field.get(null)) {
			((Map<?, ?>) field.get(null)).clear();
		}
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
