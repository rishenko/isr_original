package com.astrodoorways.converter.jexif.pool;

import be.pw.jexif.JExifTool;
import be.pw.jexif.internal.constants.ExecutionConstant;
import com.google.common.base.Strings;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class JExifToolPoolableFactory extends BasePooledObjectFactory<JExifTool> {

	private static Logger logger = LoggerFactory.getLogger(JExifToolPoolableFactory.class);

	public JExifToolPoolableFactory(String argsFileDirectory) throws IOException {
		if (Strings.isNullOrEmpty(argsFileDirectory)) {
			throw new IllegalArgumentException("the directory housing the exiftool args file cannot be null");
		}

		Properties p = new Properties();
		p.load(new FileInputStream(new File("./jexiftool.properties")));

		System.setProperty(ExecutionConstant.EXIFTOOLPATH, p.getProperty(ExecutionConstant.EXIFTOOLPATH));
		System.setProperty(ExecutionConstant.EXIFTOOLDEADLOCK, p.getProperty(ExecutionConstant.EXIFTOOLDEADLOCK));

		if (!argsFileDirectory.endsWith("/")) {
			argsFileDirectory += "/";
		}
	}

	@Override
	public JExifTool create() throws Exception {
		logger.trace("creating new JExif object");
		return new JExifTool();
	}

	@Override
	public PooledObject<JExifTool> wrap(JExifTool jExifTool) {
		return new DefaultPooledObject<>(jExifTool);
	}
}
