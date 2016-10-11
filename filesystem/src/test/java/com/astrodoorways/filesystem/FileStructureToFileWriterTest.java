package com.astrodoorways.filesystem;

import java.io.File;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.astrodoorways.filesystem.writers.FileStructureToFileWriter;

@RunWith(JUnit4.class)
public class FileStructureToFileWriterTest {

	public static String BASE_PATH = "src/test/resources/com/astrodoorways/filesystem/";
	public static String WRITE_PATH = BASE_PATH + "write/";
	public static String READ_PATH = BASE_PATH + "read/";
	public static String FILE_NAME = "writeLinesToFileWriter.txt";

	@After
	public void deleteFile() {
		new File(WRITE_PATH + FILE_NAME).delete();
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorFileFailTest() {
		new FileStructureToFileWriter(null, false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorFileFailArgTest() {
		FileStructureToFileWriter writer = new FileStructureToFileWriter(new File(WRITE_PATH + FILE_NAME), false);
		writer.close();
	}

	@Test
	public void copyFileStructureTest() {
		String[] acceptedExtensions = new String[] { "txt", "pdf" };
		FileStructureToFileWriter writer = new FileStructureToFileWriter(new File(WRITE_PATH + FILE_NAME), false,
				acceptedExtensions);
		writer.writeFileStructure(new File(READ_PATH));
		writer.close();
	}
}
