package com.astrodoorways.filesystem.writers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.io.Files;

/**
 * 
 * Generate a file where lines can be written to
 * 
 * @author kmcabee
 */
public class LinesToFileWriter {
	private FileChannel pathFileWriter;
	private File fileToWrite;
	private boolean append;

	private static final Logger logger = LoggerFactory.getLogger(LinesToFileWriter.class);

	/**
	 * Constructor using a specific file reference to write to.
	 * 
	 * @param fileToWrite
	 * 
	 * @exception IllegalArgumentException
	 *                will be thrown if fileToWrite is null or cannot be written
	 *                to
	 */
	public LinesToFileWriter(File fileToWrite, boolean append) {
		this.append = append;
		prepareFileForWriting(fileToWrite);
	}

	/**
	 * constructor that requires both the read and write directory file names
	 * not be empty
	 * 
	 * @param readDirectory
	 * @param writeDir
	 * 
	 * @exception IllegalArgumentException
	 *                will be thrown if the file name or write directories are
	 *                null.
	 * @exception IllegalStateException
	 *                thrown if the write directory cannot be written to
	 */
	public LinesToFileWriter(String writeDir, String fileName, boolean append) {
		// validate and set the file name property
		if (fileName == null)
			throw new IllegalArgumentException("the path generator must have a file name");

		// validate and set the directory to write to
		if (Strings.isNullOrEmpty(writeDir))
			throw new IllegalArgumentException("The writeDirectory can't be null.");
		if (!writeDir.endsWith("/"))
			writeDir += "/";

		this.append = append;

		logger.trace("writeDirectory: {} file: {}", new Object[] { writeDir, fileName });
		prepareFileForWriting(new File(writeDir + fileName));
	}

	/**
	 * validate and prepare the file for writing
	 * 
	 * @param file
	 */
	private void prepareFileForWriting(File file) {
		// validate the incoming file
		if (file == null) {
			logger.error("fileToWrite cannot be null");
			throw new IllegalArgumentException("fileToWrite cannot be null");
		}

		// if the file doesn't exist, try to create
		try {
			if (!file.exists()) {
				Files.createParentDirs(file);
				file.createNewFile();
			}
		} catch (IOException e) {
			logger.error("could not create the file to write to", e);
			throw new RuntimeException(e);
		}

		// check to see if you can write to the file
		if (!file.canWrite()) {
			String path = file.getAbsolutePath();
			logger.error("you cannot write to {}", path);
			throw new IllegalStateException("cannot write to " + path);
		}

		try {
			this.fileToWrite = file;
			pathFileWriter = new FileOutputStream(fileToWrite, append).getChannel();
		} catch (IOException e) {
			logger.error("could not write to the path file", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * perform the actual grunt work of processing the file structure specified
	 * by the read directory into the file specified by
	 * getPathFileAbsolutePath().
	 */
	public synchronized void writeLine(String line) {
		// close the writer
		try {
			// if the file is of the proper type, write out its path
			logger.debug("adding to file {} the line {}", new Object[] { fileToWrite.getAbsolutePath(), line });
			line += "\n";
			pathFileWriter.write(ByteBuffer.wrap(line.getBytes()));
		} catch (IOException e) {
			logger.error("could not write a line to the file", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * close the file being written to
	 */
	public void close() {
		try {
			pathFileWriter.close();
		} catch (IOException e) {
			logger.error("could not close the file", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return the path of the file containing the list of files to be processed
	 */
	public String getFileAbsolutePath() {
		String fileName = fileToWrite.getAbsolutePath();
		logger.debug("path file name: {}", fileName);
		return fileName;
	}

}
