package com.astrodoorways.filesystem.writers;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public interface FileStructureWriter {

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

}