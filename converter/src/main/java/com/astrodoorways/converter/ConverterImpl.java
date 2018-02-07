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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.Date;
import java.util.List;
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
		logger.info("Begin building list of files to convert from read directory.");
		job = buildAndPersistJob();
		fileStructureWriter.setJob(job);
		fileStructureWriter.writeFileStructure(new File(readDirectory));
		fileInfos = fileStructureWriter.getFileInfos();
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
		// Build the metadata for each of the files
		int pageSize = 50;
		int fileInfoCount = fileInfoDAO.countByJob(job);
		int maxPages = fileInfoCount / pageSize;
		for (int page = 1; page <= maxPages; page++) {
			PageRequest pageRequest = PageRequest.of(page, pageSize);
			Page<FileInfo> dataPage = fileInfoDAO.findByJob(job, pageRequest);
			for (FileInfo fileInfo: dataPage) {
				preProcessImage(fileInfo);
				logger.trace("adding a task to process metadata for: {}", fileInfo);

				MetadataProcessRunnable runnable = context.getBean(MetadataProcessRunnable.class);
				runnable.setJob(fileInfo.getJob());
				runnable.setFileInfo(fileInfo);
				runnable.setCounter(counter);
				runnable.setMaxValue(fileInfoCount);
				metadataExecutor.execute(runnable);
			}
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
		int metadataCount = metadataDAO.countByFileInfoJob(job);
		List<String> targets = ApplicationProperties.getPropertyAsStringList(ApplicationProperties.TARGET_LIST);
		List<String> filters = ApplicationProperties.getPropertyAsStringList(ApplicationProperties.FILTER_LIST);

		logger.debug("targets: {} filters: {} metadataCount: {}", targets, filters, metadataCount);

		int totalFiles = metadataDAO.countByFileInfoJob(job);
		int pageSize = 50;
		int maxPages = (totalFiles / pageSize) + 1;
		for (int page = 1; page <= maxPages; page++) {
			PageRequest pageRequest = PageRequest.of(page, pageSize);
			Page<Metadata> dataPage = metadataDAO.findByFileInfoJob(job, pageRequest);
			dataPage.forEach(metadata -> {
				logger.trace("adding sequence convert task: {}", metadata);
				ConvertRunnable runnable = context.getBean(ConvertRunnable.class);
				prepConvertRunnable(counter, metadataCount, metadata, runnable);
				converterExecutor.execute(runnable);
			});
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
	 * @return if the job should build a sequence of files or not
	 */
	private boolean isSequence() {
		String seqStr = ApplicationProperties.getPropertyAsString(ApplicationProperties.SEQUENCE);
		return seqStr != null && seqStr.trim().equals("");
	}

	/**
	 * build a local directory structure
	 * 
	 * @param link
	 */
	private void buildLocalDirectoryStructure(String link) {
		String dir = "";
		for (String nextDir : link.split("/")) {
			if (nextDir == null || nextDir.equals("")) {
				dir += "/";
				continue;
			}

			if (link.endsWith(nextDir)) {
				break;
			}

			dir += nextDir + "/";
			File tempFile = new File(dir);
			if (!tempFile.canRead()) {
				tempFile.mkdir();
			}
		}
	}

	@Override
	public void setReadDirectory(String readDirectory) {
		this.readDirectory = readDirectory;
	}

	@Override
	public void setWriteDirectory(String writeDirectory) {
		this.writeDirectory = writeDirectory;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void preProcessImage(FileInfo fileInfo) throws IOException, InterruptedException {
		String filePath = fileInfo.getDirectory() + "/" + fileInfo.getFileName();

		if (filePath.endsWith(".fits")) {
			// rename fits file to fz to try and unpack, just in case
			String newFilePath = filePath.substring(0, filePath.length() - 4) + "fz";
			if (!new File(filePath).renameTo(new File(newFilePath))) {
				new File(filePath).delete();
			}
			filePath = newFilePath;
		}

		if (filePath.endsWith(".fz")) {
			// handle the location of the intermediary, preprocessed file
			String newFilePath = ApplicationProperties.getPropertyAsString(ApplicationProperties.PREPROCESSED_PATH);
			String fileName = fileInfo.getFileName();
			if (newFilePath != null) {
				logger.trace("using intermediary structure");
				newFilePath += "/" + fileInfo.getDirectory().replace(this.readDirectory, "") + "/";
				buildLocalDirectoryStructure(newFilePath);
				newFilePath += fileName.substring(0, fileName.length() - 2) + "fits";
			} else {
				newFilePath = fileInfo.getDirectory() + "/" + fileName.substring(0, fileName.length() - 2) + "fits";
			}
			logger.trace("intermediary file path: {}", newFilePath);

			// prepare the decompression process
			String funpackPath = ApplicationProperties.getPropertyAsString(ApplicationProperties.FUNPACK_PATH);
			if (funpackPath == null)
				funpackPath = "funpack";
			ProcessBuilder builder = new ProcessBuilder(funpackPath, "-O", newFilePath, filePath);
			builder.redirectErrorStream(true);
			Process process = builder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				logger.debug("funpack output: {}", line);
			}
			if (process.waitFor() != 0)
				logger.error("funpack process returned an error");
			process.destroy();

			// if there was a preprocessed path, make sure to save the new path to the file info object
			if (ApplicationProperties.getPropertyAsString(ApplicationProperties.PREPROCESSED_PATH) != null) {
				fileInfo.setPreprocessedFileName(newFilePath);
				fileInfoDAO.save(fileInfo);
			}
		}
	}
}
