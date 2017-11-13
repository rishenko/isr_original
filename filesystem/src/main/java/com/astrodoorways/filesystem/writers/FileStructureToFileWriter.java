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

	public FileStructureToFileWriter(File file, boolean append, String... acceptedExtensions) {
		if (acceptedExtensions == null || acceptedExtensions.length == 0) {
			String message = "there must be at least one acceptable extension";
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		writer = new LinesToFileWriter(file, append);
		this.acceptedExtensions = new HashSet<>(Arrays.asList(acceptedExtensions));
	}

	public FileStructureToFileWriter(String writeDir, String fileName, boolean append, String... acceptedExtensions) {
		if (acceptedExtensions == null || acceptedExtensions.length == 0) {
			String message = "there must be at least one acceptable extension";
			logger.error(message);
			throw new IllegalArgumentException(message);
		}
		writer = new LinesToFileWriter(writeDir, fileName, append);
		this.acceptedExtensions = new HashSet<>(Arrays.asList(acceptedExtensions));
	}

	/**
	 * recursively move through the file structure found in file, writing out
	 * the paths
	 * 
	 * @param file
	 */
	public void writeFileStructure(File file) {
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
}
