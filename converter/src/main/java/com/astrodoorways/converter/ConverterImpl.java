package com.astrodoorways.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import be.pw.jexif.JExifTool;

import com.astrodoorways.converter.jexif.pool.JExifToolPoolableFactory;
import com.astrodoorways.db.filesystem.FileInfo;
import com.astrodoorways.db.filesystem.FileInfoDAO;
import com.astrodoorways.db.filesystem.FileStructureToDatabaseWriter;
import com.astrodoorways.db.filesystem.Job;
import com.astrodoorways.db.filesystem.JobDAO;
import com.astrodoorways.db.imagery.Metadata;
import com.astrodoorways.db.imagery.MetadataDAO;
import com.google.common.base.Strings;

@Component("converter")
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
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public ConverterImpl() throws FileNotFoundException, IOException {
		Properties p = new Properties();
		p.load(new FileInputStream(new File("./jexiftool.properties")));
	}

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

		// prepare jexif, thread, and process pools
		Integer numProcesses = ApplicationProperties.getPropertyAsInteger(ApplicationProperties.MAX_NUM_PROCESSORS);
		if (numProcesses == null)
			numProcesses = (int) (Runtime.getRuntime().availableProcessors() * .75);

		// Parse the file structure for applicable files
		List<FileInfo> fileInfos = null;
		Long jobId = ApplicationProperties.getPropertyAsLong(ApplicationProperties.JOB_ID);
		if (jobId == null) {
			job = buildJob();
			fileStructureWriter.setJob(job);
			fileStructureWriter.writeFileStructure(new File(readDirectory));
			fileInfos = fileStructureWriter.getFileInfos();
			fileInfoDAO = fileStructureWriter.getFileInfoDAO();
		} else {
			job = jobDAO.findById(jobId).get();
			fileStructureWriter.setJob(job);
			fileInfos = fileStructureWriter.getFileInfos();
		}

		// process metadata if its not a job only run
		if (!ApplicationProperties.getPropertyAsBoolean(ApplicationProperties.PROCESS_JOB_ONLY)) {
			processMetadata(numProcesses, fileInfos);
		}

		// Only convert the files if there is no property for metadata only or if metadata only is set to false
		if (!ApplicationProperties.getPropertyAsBoolean(ApplicationProperties.METADATA_ONLY)) {
			convertFiles(numProcesses);
		}

		logger.debug("!!!! Converter thread pool has terminated and the application completed. !!!!!");
	}

	/**
	 * @return the built job
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private Job buildJob() {
		Job job = new Job();
		job.setDate(new Date());
		jobDAO.save(job);
		return job;
	}

	/**
	 * Process the metadata for the files represented by the collection of file info objects
	 * using the number of provided processes.
	 * 
	 * @param numProcesses
	 * @param fileInfos
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	private void processMetadata(int numProcesses, List<FileInfo> fileInfos) throws IOException,
			InterruptedException {
		AtomicInteger counter = new AtomicInteger();
		// Build the metadata for each of the files
		metadataExecutor.setCorePoolSize(numProcesses);
		int fileInfoCount = fileInfoDAO.countByJob(job);
		for (FileInfo fileInfo: fileInfos) {
			executorThrottleBasic(metadataExecutor);
			preProcessImage(fileInfo);
			// try to convert the image at least twice if there is a failure
			logger.debug("adding a task to process metadata for: {}", fileInfo);

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
		}

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
			String line = "";
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

	/**
	 * Convert images using a number or threads
	 * 
	 * @param numProcesses
	 * @throws Exception
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void convertFiles(int numProcesses) throws Exception {
		ObjectPool<JExifTool> jexifToolPool = new GenericObjectPool<JExifTool>(new JExifToolPoolableFactory(
				writeDirectory + "/jexiftool-args"), numProcesses + 4);
		AtomicInteger counter = new AtomicInteger();

		// Convert all of the files according to the metadata in the database
		counter = new AtomicInteger();
		int metadataCount = metadataDAO.countByFileInfoJob(job);
		List<String> targets = ApplicationProperties.getPropertyAsStringList(ApplicationProperties.TARGET_LIST);
		List<String> filters = ApplicationProperties.getPropertyAsStringList(ApplicationProperties.FILTER_LIST);

		logger.debug("targets: {} filters: {} metadataCount: {}", new Object[]{targets, filters, metadataCount});
		converterExecutor.setCorePoolSize(numProcesses);

		if (isSequence()) {
			int count = 0;
			// MAIN TOPIC: retrieve the metadata, grouped by mission, target, filter 1, filter 2
			// 1) Get a master set of objects representing the distinct groupings

			// 2) Query for all metadata by a single grouping
			for (Metadata metadata : new HashSet<Metadata>(metadataDAO.findByFileInfoJob(job))) {
				executorThrottleBasic(converterExecutor);
				logger.debug("adding a task to convert the following in a sequence: {}", metadata);
				ConvertRunnable runnable = context.getBean(ConvertRunnable.class);
				runnable.setMetadata(metadata);
				runnable.setSeqCount(count++);
				runnable.setWriteDirectory(writeDirectory);
				runnable.setJexifToolPool(jexifToolPool);
				runnable.setType("TIFF");
				runnable.setCounter(counter);
				runnable.setMaxValue(metadataCount);
				runnable.setFilePath(metadata.getFileInfo().getFilePath());
				converterExecutor.execute(runnable);
			}
			// not generating sequences, so build the file names according to metadata
		} else {
			for (Metadata metadata: metadataDAO.findByFileInfoJob(job)) {
				executorThrottleBasic(converterExecutor);
				logger.debug("adding a task to convert the following: {}", metadata);
				ConvertRunnable runnable = context.getBean(ConvertRunnable.class);
				runnable.setMetadata(metadata);
				runnable.setWriteDirectory(writeDirectory);
				runnable.setJexifToolPool(jexifToolPool);
				runnable.setType("tiff");
				runnable.setCounter(counter);
				runnable.setMaxValue(metadataCount);
				runnable.setFilePath(metadata.getFileInfo().getFilePath());
				converterExecutor.execute(runnable);
			}
		}

		// let the thread live while the converter processes
		converterExecutor.setWaitForTasksToCompleteOnShutdown(true);
		converterExecutor.shutdown();
		while (!converterExecutor.getThreadPoolExecutor().isTerminated()) {
		}

		jexifToolPool.close();
	}

	@Override
	public void executorThrottleBasic(ThreadPoolTaskExecutor executor) throws InterruptedException {
		executorThrottle(executor, 1000, 5);
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
			Thread.sleep(sleepSeconds * 1000);
		}
	}

	/**
	 * @return if the job should build a sequence of files or not
	 */
	private boolean isSequence() {
		String seqStr = ApplicationProperties.getPropertyAsString(ApplicationProperties.SEQUENCE);
		return seqStr != null && seqStr.trim().equals("");
	}

	@Override
	public void sleepTask() throws InterruptedException {
		int timeToSubtract = 250;
		// see if the user passed in a percentage of system utilization value
		Integer percentage = ApplicationProperties
				.getPropertyAsInteger(ApplicationProperties.SYSTEM_PERCENT_UTILIZATION);
		if (percentage != null) {
			if (percentage == 100) {
				timeToSubtract = 0;
			} else {
				double finalPercent = percentage / 100d;
				double firstPercentage = 1000d * finalPercent;
				double secondPercentage = 100d * finalPercent;
				if ((firstPercentage + secondPercentage) < 1000)
					timeToSubtract = (int) (firstPercentage + secondPercentage);
				else
					timeToSubtract = 995;
			}
			logger.trace("percentage for subtraction: {}", percentage);
		}
		logger.trace("time to subtract from 1000 milliseconds: {}", timeToSubtract);
		// if the user did not specify 100% system utilization, calculate sleep time to match
		if (timeToSubtract != 0) {
			int millisToSleep = (int) (1000 - (timeToSubtract));
			Thread.sleep(millisToSleep);
		}
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
	public String getReadDirectory() {
		return readDirectory;
	}

	@Override
	public void setReadDirectory(String readDirectory) {
		this.readDirectory = readDirectory;
	}

	@Override
	public String getWriteDirectory() {
		return writeDirectory;
	}

	@Override
	public void setWriteDirectory(String writeDirectory) {
		this.writeDirectory = writeDirectory;
	}
}
