package com.astrodoorways.db.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.hibernate.ScrollableResults;

public interface FileStructureToDatabaseWriter {

	public static final String SYSTEM_PERCENT_UTILIZATION = "system.percent.utilization";

	/**
	 * recursively move through the file structure found in file, writing out
	 * the paths
	 * 
	 * @param file
	 * @param writer
	 * @throws IOException 
	 */
	void writeFileStructure(File file) throws IOException;

	Collection<String> getCollectionOfFiles();

	ScrollableResults getFileInfos();

	void clear();

	FileInfoDAO getFileInfoDAO();

	void setFileStructureDAO(FileInfoDAO fileStructureDAO);

	Job getJob();

	void setJob(Job job);

}