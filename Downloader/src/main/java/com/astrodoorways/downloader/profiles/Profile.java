package com.astrodoorways.downloader.profiles;

import com.astrodoorways.filesystem.writers.LinesToFileWriter;

public interface Profile {

	void process();

	String getURL();

	String getRemoteServer();

	LinesToFileWriter getPathGenerator();

	String getLinksFileName();

	String getWriteDirectory();

}