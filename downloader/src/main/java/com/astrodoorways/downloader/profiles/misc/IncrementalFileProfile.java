package com.astrodoorways.downloader.profiles.misc;

import com.astrodoorways.downloader.profiles.AbstractBaseProfile;
import com.astrodoorways.filesystem.writers.LinesToFileWriter;

public class IncrementalFileProfile extends AbstractBaseProfile {

	private final Integer start;
	private final Integer finish;
	private final String fileFormat;

	public IncrementalFileProfile(Integer start, Integer finish, String fileFormat, String URL, String writeDirectory,
			LinesToFileWriter writer) {
		super(URL, writeDirectory, writer);
		this.start = start;
		this.finish = finish;
		this.fileFormat = fileFormat;
	}

	@Override
	public void process() {
		for (int inc = start; inc <= finish; inc++) {
			String fileName = String.format(fileFormat, inc);
			getPathGenerator().writeLine(String.format(fileName));
		}

	}

	@Override
	public String getRemoteServer() {
		return getURL();
	}

}
