package com.astrodoorways.converter.jexif.pool;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.pool.PoolableObjectFactory;
import org.fest.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.pw.jexif.JExifTool;
import be.pw.jexif.internal.constants.ExecutionConstant;

public class JExifToolPoolableFactory implements PoolableObjectFactory<JExifTool> {

	private final String argsFileDirectory;

	private static Logger logger = LoggerFactory.getLogger(JExifToolPoolableFactory.class);

	public JExifToolPoolableFactory(String argsFileDirectory) throws FileNotFoundException, IOException {
		if (Strings.isEmpty(argsFileDirectory)) {
			throw new IllegalArgumentException("the directory housing the exiftool args file cannot be null");
		}

		Properties p = new Properties();
		p.load(new FileInputStream(new File("./jexiftool.properties")));

		System.setProperty(ExecutionConstant.EXIFTOOLPATH, p.getProperty(ExecutionConstant.EXIFTOOLPATH));
		System.setProperty(ExecutionConstant.EXIFTOOLDEADLOCK, p.getProperty(ExecutionConstant.EXIFTOOLDEADLOCK));

		if (!argsFileDirectory.endsWith("/")) {
			argsFileDirectory += "/";
		}
		this.argsFileDirectory = argsFileDirectory;
	}

	@Override
	public JExifTool makeObject() throws Exception {
		logger.debug("return a new instance of JExifTool");
		File file = null;
		do {
			file = new File(argsFileDirectory + UUID.randomUUID());
		} while (file.exists());
		return new JExifTool(file.getAbsolutePath());
	}

	@Override
	public void destroyObject(JExifTool obj) throws Exception {
		logger.debug("stopping an instance of JExifTool");
		obj.stop();
	}

	@Override
	public boolean validateObject(JExifTool obj) {
		return true;
	}

	@Override
	public void activateObject(JExifTool obj) throws Exception {
		// do nothing
	}

	@Override
	public void passivateObject(JExifTool obj) throws Exception {
		// do nothing
	}
}
