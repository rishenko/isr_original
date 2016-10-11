package com.astrodoorways.filesystem.writers;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Recurse a file structure and write out all files with acceptable extensions.
 * 
 * @author kmcabee
 * 
 */
public class FileStructureToFileWriter {

	Logger logger = LoggerFactory.getLogger(FileStructureToFileWriter.class);

	private final LinesToFileWriter writer;

	private Set<String> acceptedExtensions = new HashSet<String>();

	public static final String SYSTEM_PERCENT_UTILIZATION = "system.percent.utilization";

	public FileStructureToFileWriter(File file, boolean append, String... acceptedExtensions) {
		if (acceptedExtensions == null || acceptedExtensions.length == 0) {
			String message = "there must be at least one acceptable extension";
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		writer = new LinesToFileWriter(file, append);
		this.acceptedExtensions = new HashSet<String>(Arrays.asList(acceptedExtensions));
	}

	public FileStructureToFileWriter(String writeDir, String fileName, boolean append, String... acceptedExtensions) {
		if (acceptedExtensions == null || acceptedExtensions.length == 0) {
			String message = "there must be at least one acceptable extension";
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		writer = new LinesToFileWriter(writeDir, fileName, append);
		this.acceptedExtensions = new HashSet<String>(Arrays.asList(acceptedExtensions));
	}

	/**
	 * recursively move through the file structure found in file, writing out
	 * the paths
	 * 
	 * @param file
	 * @param writer
	 */
	public void writeFileStructure(File file) {
		try {
			sleepTask();
		} catch (InterruptedException e) {
			logger.error("could not put the file structure to file writer to sleep", e);
		}
		// if file is a directory, recursively call writeFileStructure on its
		// children
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				writeFileStructure(child);
			}
		} else {
			// if the file is of the proper type, write out its path
			String fileName = file.getName();
			int fileNameLength = fileName.length();
			int positionLastPeriod = fileName.lastIndexOf(".");
			String extension = file.getName().substring(positionLastPeriod + 1, fileNameLength);
			if (acceptedExtensions.contains(extension)) {
				writer.writeLine(file.getAbsolutePath());
			}
		}
	}

	/**
	 * close the writer. should be called when finished
	 */
	public void close() {
		writer.close();
	}

	/**
	 * @return path to written file
	 */
	public String getFileAbsolutePath() {
		return writer.getFileAbsolutePath();
	}

	public void sleepTask() throws InterruptedException {
		int timeToSubtract = 250;
		// see if the user passed in a percentage of system utilization value
		if (System.getProperties().containsKey(SYSTEM_PERCENT_UTILIZATION)) {
			int percentage = Integer.parseInt(System.getProperties().getProperty(SYSTEM_PERCENT_UTILIZATION));
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
}
