package com.astrodoorways.converter.jexif.pool;

import java.lang.ProcessBuilder.Redirect;

import org.apache.commons.pool.PoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessPoolableFactory implements PoolableObjectFactory<Process> {

	private static Logger logger = LoggerFactory.getLogger(ProcessPoolableFactory.class);

	@Override
	public Process makeObject() throws Exception {
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
	public void destroyObject(Process obj) throws Exception {
		logger.debug("stopping an instance of Process");
		obj.destroy();
	}

	@Override
	public boolean validateObject(Process obj) {
		return true;
	}

	@Override
	public void activateObject(Process obj) throws Exception {
		// do nothing
	}

	@Override
	public void passivateObject(Process obj) throws Exception {
		// do nothing
	}
}
