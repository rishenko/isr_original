package com.astrodoorways.filesystem;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.astrodoorways.filesystem.writers.LinesToFileWriter;

@RunWith(JUnit4.class)
public class LinesToFileWriterTest {

	public static String WRITE_PATH = "src/test/resources/com/astrodoorways/filesystem/write/";
	public static String FILE_NAME = "writeLinesToFileWriter.txt";

	@After
	public void deleteFile() {
		new File(WRITE_PATH + FILE_NAME).delete();
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorFileFailTest() {
		new LinesToFileWriter(null, false);
	}

	@Test
	public void constructorFileSuccessTest() {
		new LinesToFileWriter(new File(WRITE_PATH + FILE_NAME), false);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorPathFailTest() {
		new LinesToFileWriter(null, null, false);
	}

	@Test
	public void constructorPathSuccessTest() {
		new LinesToFileWriter(WRITE_PATH, FILE_NAME, false);
	}

	@Test
	public void absolutePathTest() {
		LinesToFileWriter writer = new LinesToFileWriter(WRITE_PATH, FILE_NAME, false);
		writer.writeLine("stuff");
		String acceptedPath = new File(WRITE_PATH + FILE_NAME).getAbsolutePath();
		assertTrue(acceptedPath.equals(writer.getFileAbsolutePath()));
	}

	@Test
	public void writeLineTest() {
		LinesToFileWriter writer = new LinesToFileWriter(WRITE_PATH, FILE_NAME, false);
		writer.writeLine("a line of text");
	}
}
