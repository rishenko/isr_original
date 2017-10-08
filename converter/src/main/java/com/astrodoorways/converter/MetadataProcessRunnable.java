package com.astrodoorways.converter;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.metadata.IIOMetadata;

import org.slf4j.Logger;

import com.astrodoorways.db.filesystem.FileInfo;
import com.astrodoorways.db.filesystem.Job;

public interface MetadataProcessRunnable extends Runnable {

	void run();

	void readImage(File file) throws IOException;

	FileInfo getFileInfo();

	void setFileInfo(FileInfo fileInfo);

	Job getJob();

	void setJob(Job job);

	IIOMetadata getMetadata();

	void setMetadata(IIOMetadata metadata);

	String getFilePath();

	void setFilePath(String filePath);

	AtomicInteger getCounter();

	void setCounter(AtomicInteger counter);

	int getMaxValue();

	void setMaxValue(int maxValue);

	Logger getLogger();

	void setLogger(Logger logger);

}