package com.astrodoorways.converter;

import com.astrodoorways.converter.db.filesystem.*;
import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.db.imagery.MetadataDAO;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

@Component("converter")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ConverterImpl implements Converter {

	private static final Logger logger = LoggerFactory.getLogger(ConverterImpl.class);

	@Autowired
	private JobDAO jobDAO;

	@Autowired
	private FileInfoDAO fileInfoDAO;

	@Autowired
	private FileStructureToDatabaseWriter fileStructureWriter;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MetadataDAO metadataDAO;

	@Autowired
	private ThreadPoolTaskExecutor metadataExecutor;

	@Autowired
	private ThreadPoolTaskExecutor converterExecutor;

	private String readDirectory;
	private String writeDirectory;
	private Job job;
	private File temporaryDirectory;
	private boolean resuming;

	/**
	 * Default no-arg constructor to allow for bean usage.
	 */
	public ConverterImpl() {}

	public ConverterImpl(String readDirectory, String writeDirectory) {
		this.readDirectory = readDirectory;
		this.writeDirectory = writeDirectory;
	}

	/**
	 * Three steps to the conversion process:
	 * 1) Get a list of files to be processed
	 * 2) Process the metadata
	 * 3) Process the actual files using the metadata and output them according to job parameters.
	 * 
	 * @throws Exception
	 */
	@Override
	public void beginConversion() throws Exception {
		if (Strings.isNullOrEmpty(readDirectory) || Strings.isNullOrEmpty(writeDirectory)) {
			logger.error("both readDirectory and writeDirectory must not be null or empty strings");
			throw new IllegalArgumentException(
					"both readDirectory and writeDirectory must not be null or empty strings");
		}

		// Parse the file structure for applicable files
		List<FileInfo> fileInfos;
		Long jobId = ApplicationProperties.getPropertyAsLong(ApplicationProperties.JOB_ID);
		logger.info("Begin building list of files to convert from read directory.");
		if (jobId == null) {
			job = buildAndPersistJob();
			resuming = false;
			logger.info("Started job {}. To resume it after an interruption, run again with {}={}.", job.getId(),
					ApplicationProperties.JOB_ID, job.getId());
			fileStructureWriter.setJob(job);
			fileStructureWriter.writeFileStructure(new File(readDirectory));
			fileInfos = fileStructureWriter.getFileInfos();
		} else {
			job = jobDAO.findById(jobId)
					.orElseThrow(() -> new IllegalArgumentException("no job with id " + jobId + " in the database"));
			resuming = true;
			fileStructureWriter.setJob(job);
			fileInfos = fileStructureWriter.getFileInfos();
			logger.info("Resuming job {}: {} files, {} already converted.", jobId, fileInfos.size(),
					fileInfos.stream().filter(fileInfo -> fileInfo.getOutputFileName() != null).count());
		}
		logger.info("Finished building list of files to convert from read directory.");
		logger.info("Begin processing metadata.");
		processMetadata(fileInfos);
		logger.info("Finished processing metadata.");
		logger.info("Begin converting data to images.");
		convertFiles();
		logger.info("Finished converting data to images.");

		logger.info("!!!! Conversion complete. You can kill the application. !!!!!");
	}

	/**
	 * @return the built job
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private Job buildAndPersistJob() {
		Job job = new Job();
		job.setDate(new Date());
		jobDAO.save(job);
		return job;
	}

	/**
	 * Process the metadata for the files represented by the collection of file info objects
	 * using the number of provided processes.
	 *
	 * @param fileInfos
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void processMetadata(List<FileInfo> fileInfos) throws IOException,
			InterruptedException {
		AtomicInteger counter = new AtomicInteger();
		// when resuming, skip files that are already converted or already have metadata
		Set<Long> withMetadata = new HashSet<>();
		for (Metadata metadata : metadataDAO.findByFileInfoJob(job)) {
			withMetadata.add(metadata.getFileInfo().getId());
		}
		List<FileInfo> todo = new ArrayList<>();
		for (FileInfo fileInfo : fileInfos) {
			if (fileInfo.getOutputFileName() == null) {
				todo.add(fileInfo);
			}
		}
		int fileInfoCount = (int) todo.stream().filter(fileInfo -> !withMetadata.contains(fileInfo.getId())).count();
		for (FileInfo fileInfo: todo) {
			executorThrottleBasic(metadataExecutor);
			// files still to convert need their funpack intermediary even if their metadata exists
			preProcessImage(fileInfo);
			if (withMetadata.contains(fileInfo.getId())) {
				continue;
			}
			// try to convert the image at least twice if there is a failure
			logger.trace("adding a task to process metadata for: {}", fileInfo);

			MetadataProcessRunnable runnable = context.getBean(MetadataProcessRunnable.class);
			runnable.setJob(fileInfo.getJob());
			runnable.setFileInfo(fileInfo);
			runnable.setCounter(counter);
			runnable.setMaxValue(fileInfoCount);
			metadataExecutor.execute(runnable);
		}

		// let the metadata processing tasks live while the converter processes
		metadataExecutor.setWaitForTasksToCompleteOnShutdown(true);
		metadataExecutor.shutdown();
		while (!metadataExecutor.getThreadPoolExecutor().isTerminated()) {
			sleep(1000);
		}

	}

	/**
	 * Convert images using a number or threads
	 *
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void convertFiles() throws Exception {
			// Convert all of the files according to the metadata in the database
		AtomicInteger counter = new AtomicInteger();
		int metadataCount = (int) metadataDAO.findByFileInfoJob(job).stream()
				.filter(metadata -> metadata.getFileInfo().getOutputFileName() == null).count();
		List<String> targets = ApplicationProperties.getPropertyAsStringList(ApplicationProperties.TARGET_LIST);
		List<String> filters = ApplicationProperties.getPropertyAsStringList(ApplicationProperties.FILTER_LIST);

		logger.debug("targets: {} filters: {} metadataCount: {}", targets, filters, metadataCount);

		List<Metadata> metadataList = metadataDAO.findByFileInfoJob(job);
		// numbered over the whole job, so a resumed job gives every image the same number as before
		Map<Metadata, Integer> sequenceNumbers = isSequence() ? assignSequenceNumbers(metadataList)
				: Collections.emptyMap();
		for (Metadata metadata : metadataList) {
			if (metadata.getFileInfo().getOutputFileName() != null) {
				continue; // converted before the job was interrupted
			}
			executorThrottleBasic(converterExecutor);
			logger.trace("adding convert task: {}", metadata);
			ConvertRunnable runnable = context.getBean(ConvertRunnable.class);
			prepConvertRunnable(counter, metadataCount, metadata, runnable);
			runnable.setSeqCount(sequenceNumbers.get(metadata));
			// an output left half-written by the interrupted run is replaced
			runnable.setReplaceExisting(resuming);
			converterExecutor.execute(runnable);
		}

		// let the thread live while the converter processes
		converterExecutor.setWaitForTasksToCompleteOnShutdown(true);
		converterExecutor.shutdown();
		while (!converterExecutor.getThreadPoolExecutor().isTerminated()) {
			sleep(1000);
		}
	}

	private void prepConvertRunnable(AtomicInteger counter, int metadataCount, Metadata metadata, ConvertRunnable runnable) {
		runnable.setMetadata(metadata);
		runnable.setWriteDirectory(writeDirectory);
		runnable.setType("TIFF");
		runnable.setCounter(counter);
		runnable.setMaxValue(metadataCount);
		runnable.setFilePath(metadata.getFileInfo().getFilePath());
	}

	@Override
	public void executorThrottleBasic(ThreadPoolTaskExecutor executor) throws InterruptedException {
		executorThrottle(executor, 1000, 1);
	}

	/**
	 * Throttle the use of an executor
	 * 
	 * @param executor
	 * @param count
	 * @param sleepSeconds
	 * @throws InterruptedException
	 */
	@Override
	public void executorThrottle(ThreadPoolTaskExecutor executor, int count, int sleepSeconds)
			throws InterruptedException {
		BlockingQueue queue = executor.getThreadPoolExecutor().getQueue();
		while (queue.size() > count) {
			sleep(sleepSeconds * 1000);
		}
	}

	/**
	 * @return if the job should build a sequence of files or not (a non-blank sequence name is set)
	 */
	private boolean isSequence() {
		String seqStr = ApplicationProperties.getPropertyAsString(ApplicationProperties.SEQUENCE);
		return seqStr != null && !seqStr.trim().isEmpty();
	}

	/**
	 * Number the images 1, 2, 3... within each target and filter combination, in order of image
	 * time, so a sequence plays back chronologically no matter which thread finishes first.
	 * Images without a time go last; ties are broken by file path so numbering is repeatable.
	 */
	static Map<Metadata, Integer> assignSequenceNumbers(List<Metadata> metadataList) {
		List<Metadata> sorted = new ArrayList<>(metadataList);
		sorted.sort(Comparator.comparing(Metadata::getTime, Comparator.nullsLast(Comparator.<Date>naturalOrder()))
				.thenComparing(metadata -> metadata.getFileInfo().getFilePath()));
		Map<String, Integer> counters = new HashMap<>();
		Map<Metadata, Integer> numbers = new IdentityHashMap<>();
		for (Metadata metadata : sorted) {
			String key = metadata.getTarget() + "|" + metadata.getFilterOne() + "|" + metadata.getFilterTwo();
			numbers.put(metadata, counters.merge(key, 1, Integer::sum));
		}
		return numbers;
	}

	@Override
	public void setReadDirectory(String readDirectory) {
		this.readDirectory = readDirectory;
	}

	@Override
	public void setWriteDirectory(String writeDirectory) {
		this.writeDirectory = writeDirectory;
	}

	/**
	 * Unpack fpack-compressed FITS files (.fz, and .fits files that may be tile-compressed) with
	 * funpack into an intermediary .fits file, and record its path on the FileInfo so both the
	 * metadata and conversion stages read it.
	 *
	 * The source file is never renamed or deleted. If funpack is missing or fails, the original
	 * file is used as is (for .fits inputs this is the same data, uncompressed).
	 *
	 * The intermediary goes to {@code preprocessed.file.dir} (mirroring the read directory's
	 * layout) when that is set, and otherwise to a temporary folder that the converter cleans up.
	 */
	void preProcessImage(FileInfo fileInfo) throws IOException, InterruptedException {
		String fileName = fileInfo.getFileName();
		String lowerName = fileName.toLowerCase();
		if (!lowerName.endsWith(".fits") && !lowerName.endsWith(".fz")) {
			return;
		}
		// already unpacked (e.g. a resumed job using preprocessed.file.dir)
		if (fileInfo.getPreprocessedFileName() != null && new File(fileInfo.getPreprocessedFileName()).canRead()) {
			return;
		}

		String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
		File intermediary;
		String preprocessedDir = ApplicationProperties.getPropertyAsString(ApplicationProperties.PREPROCESSED_PATH);
		if (preprocessedDir != null) {
			intermediary = new File(new File(preprocessedDir, relativeToReadDirectory(fileInfo.getDirectory())),
					baseName + ".fits");
		} else {
			intermediary = new File(temporaryDirectory(), fileInfo.getId() + "_" + baseName + ".fits");
		}
		if (intermediary.getCanonicalFile().equals(new File(fileInfo.getFilePath()).getCanonicalFile())) {
			logger.warn("not unpacking {}: the intermediary would overwrite the source", fileInfo.getFilePath());
			return;
		}
		intermediary.getParentFile().mkdirs();
		Files.deleteIfExists(intermediary.toPath());
		logger.trace("intermediary file path: {}", intermediary);

		String funpackPath = ApplicationProperties.getPropertyAsString(ApplicationProperties.FUNPACK_PATH);
		if (funpackPath == null)
			funpackPath = "funpack";
		ProcessBuilder builder = new ProcessBuilder(funpackPath, "-O", intermediary.getPath(), fileInfo.getFilePath());
		builder.redirectErrorStream(true);
		int exitCode;
		try {
			Process process = builder.start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					logger.debug("funpack output: {}", line);
				}
			}
			exitCode = process.waitFor();
		} catch (IOException e) {
			logger.warn("could not run funpack ({}); reading {} as is", e.getMessage(), fileInfo.getFilePath());
			clearPreprocessed(fileInfo);
			return;
		}
		if (exitCode != 0 || !intermediary.canRead()) {
			logger.warn("funpack returned {} for {}; reading it as is", exitCode, fileInfo.getFilePath());
			Files.deleteIfExists(intermediary.toPath());
			clearPreprocessed(fileInfo);
			return;
		}

		fileInfo.setPreprocessedFileName(intermediary.getPath());
		fileInfoDAO.save(fileInfo);
	}

	/**
	 * The folder's path below the read directory ("" for the read directory itself). Both sides are
	 * made canonical: the file scan stores canonical paths, which differ from the configured read
	 * directory when it goes through a symlink (e.g. /var vs /private/var on macOS).
	 */
	private String relativeToReadDirectory(String directory) throws IOException {
		java.nio.file.Path root = new File(readDirectory).getCanonicalFile().toPath();
		java.nio.file.Path dir = new File(directory).getCanonicalFile().toPath();
		return dir.startsWith(root) ? root.relativize(dir).toString() : "";
	}

	/** Forget an intermediary from an earlier run that no longer exists (e.g. on resume). */
	private void clearPreprocessed(FileInfo fileInfo) {
		if (fileInfo.getPreprocessedFileName() != null) {
			fileInfo.setPreprocessedFileName(null);
			fileInfoDAO.save(fileInfo);
		}
	}

	private File temporaryDirectory() throws IOException {
		if (temporaryDirectory == null) {
			temporaryDirectory = Files.createTempDirectory("isr-funpack").toFile();
			temporaryDirectory.deleteOnExit();
		}
		return temporaryDirectory;
	}
}
