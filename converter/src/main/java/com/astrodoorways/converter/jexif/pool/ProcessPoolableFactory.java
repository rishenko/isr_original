package com.astrodoorways.converter.jexif.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ProcessBuilder.Redirect;

public class ProcessPoolableFactory extends BasePooledObjectFactory<Process> {

	private static Logger logger = LoggerFactory.getLogger(ProcessPoolableFactory.class);

	@Override
	public Process create() throws Exception {
		ProcessBuilder builder = new ProcessBuilder();
		builder.redirectError(Redirect.INHERIT);
		// I am being lazy, but really the InputStream is where
		// you can get any output of the PHP Process. This setting
		// will make it output to the current processes console.
		builder.redirectOutput(Redirect.INHERIT);
		builder.redirectInput(Redirect.PIPE);
		builder.command("ls");
		return builder.start();
	}

	@Override
	public PooledObject<Process> wrap(Process process) {
		return new DefaultPooledObject<>(process);
	}
}
