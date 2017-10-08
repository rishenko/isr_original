package com.astrodoorways.converter;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.astrodoorways.converter.metadata.handler.MetadataHandler;
import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.db.filesystem.FileInfo;
import com.astrodoorways.db.filesystem.FileInfoDAO;
import com.astrodoorways.db.filesystem.Job;
import com.astrodoorways.db.imagery.Metadata;
import com.astrodoorways.db.imagery.MetadataDAO;

@Component
@Scope("prototype")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class BaseMetadataProcessRunnable implements Runnable, MetadataProcessRunnable {

	@Autowired
	private MetadataDAO metadataDAO;

	@Autowired
	private FileInfoDAO fileInfoDAO;

	private FileInfo fileInfo;
	private String filePath;
	private Job job;
	private IIOMetadata metadata;
	private AtomicInteger counter;
	private MetadataProcessor metadataProcessor = new MetadataProcessor();
	private int maxValue;

	Logger logger = LoggerFactory.getLogger(BaseMetadataProcessRunnable.class);

	public BaseMetadataProcessRunnable() {
	}

	public BaseMetadataProcessRunnable(Job job, FileInfo fileInfo, AtomicInteger counter, int maxValue) {
		this.job = job;
		this.fileInfo = fileInfo;
		this.counter = counter;
		this.maxValue = maxValue;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#run()
	 */
	@Override
	public void run() {
		final File file;
		Session session = fileInfoDAO.getSession();
		//		FileInfo fileInfoNew = (FileInfo) session.get(FileInfo.class, fileInfo.getId());
		List<FileInfo> fileInfos = session.createCriteria(FileInfo.class).list();

		// if the file has an intermediary through preprocess, reference it
		if (fileInfo.getPreprocessedFileName() == null) {
			filePath = fileInfo.getDirectory() + "/" + fileInfo.getFileName();
			file = new File(filePath);
		} else {
			logger.debug("preprocessed file path: {}", fileInfo.getPreprocessedFileName());
			filePath = fileInfo.getPreprocessedFileName();
			file = new File(fileInfo.getPreprocessedFileName());
		}
		if (!file.canRead()) {
			logger.error("file cannot be found {}", filePath);
			throw new RuntimeException("file cannot be found " + filePath);
		}

		try {
			logger.debug("creating new file from filepath");
			final File fileFinal = new File(filePath);
			if (!fileFinal.canRead()) {
				logger.error("file cannot be found {}", filePath);
				throw new RuntimeException("file cannot be found " + filePath);
			}
			logger.debug("going to read the image");
			readImage(fileFinal);
		} catch (Exception e) {
			//			if (e.getStackTrace() != null && e.getStackTrace().length > 0)
			//				logger.error("there was an error processing this file: " + filePath, e);
			//			else
			logger.error("there was an error processing the file and the exception has no stack: "
					+ e.getClass().getCanonicalName());
			return;
		}

		createMetadata();
	}

	/**
	 * create the metadata object
	 */
	private void createMetadata() {
		Map<String, String> valueMap = metadataProcessor.process(metadata);
		Metadata metadata = new Metadata();
		metadata.setCamera(valueMap.get(MetadataHandler.CAMERA));
		metadata.setExposure(Double.valueOf(valueMap.get(MetadataHandler.EXPOSURE)));
		String[] filters = valueMap.get(MetadataHandler.FILTER).split("-");
		metadata.setFilterOne(filters[0]);
		if (filters.length > 1)
			metadata.setFilterTwo(filters[1]);
		metadata.setMission(valueMap.get(MetadataHandler.MISSION));
		metadata.setTarget(valueMap.get(MetadataHandler.TARGET));

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS'Z'");
		try {
			metadata.setTime(formatter.parse(valueMap.get(MetadataHandler.TIME)));
		} catch (ParseException e) {
			formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SS'Z'");
			try {
				metadata.setTime(formatter.parse(valueMap.get(MetadataHandler.TIME)));
			} catch (ParseException e1) {
				logger.error("Couldn't parse the date", e1);
			}
		}

		metadataDAO.saveMetadata(metadata);
		metadata.setFileInfo(fileInfo);
		metadataDAO.saveMetadata(metadata);
		logger.debug("Metadata: {} FileInfo: {}", metadata.getId(), fileInfo.getId());
		logger.debug("finished processing metadata for {} - {}", filePath, completionMessage());
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#readImage(java.io.File)
	 */
	@Override
	public void readImage(File file) throws IOException {
		logger.debug("creating input stream");
		ImageInputStream stream = ImageIO.createImageInputStream(file);
		logger.debug("getting image readers");
		Iterator<ImageReader> readers = (Iterator<ImageReader>) ImageIO.getImageReaders(stream);
		if (!readers.hasNext()) {
			logger.error("no image reader was found for {}", file.getAbsolutePath());
			stream.close();
			throw new RuntimeException("no image reader was found for " + file.getAbsolutePath());
		}

		logger.debug("getting the first image reader");
		ImageReader reader = readers.next();
		logger.debug("resetting the stream");
		stream.reset();
		try {
			logger.debug("creating input stream");
			stream = ImageIO.createImageInputStream(file);
			logger.debug("setting stream as input");
			reader.setInput(stream);
			logger.debug("getting image metadata from reader");
			metadata = reader.getImageMetadata(0);
			if (metadata == null) {
				logger.error("no metadata was found for {}", file);
				throw new RuntimeException("no metadata was found for " + file.getAbsolutePath());
			}
			logger.debug("acquired the image metadata from the reader");
		} catch (IOException e) {
			logger.error("Hit an IOException");
			throw e;
		} finally {
			logger.debug("disposing of reader");
			reader.dispose();
			try {
				logger.debug("closing the stream because it hates the world");
				stream.close();
			} catch (Exception e) {
				logger.debug("the stream was already closed");
			}
		}
	}

	private String completionMessage() {
		int counterVal = counter.incrementAndGet();
		int percentComplete = (int) (((double) counterVal / (double) maxValue) * 100.0);
		return String.format("converter is %d%% complete with %d out of %d processed", percentComplete, counterVal,
				maxValue);
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#getFileInfo()
	 */
	@Override
	public FileInfo getFileInfo() {
		return fileInfo;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#setFileInfo(com.astrodoorways.db.filesystem.FileInfo)
	 */
	@Override
	public void setFileInfo(FileInfo fileInfo) {
		this.fileInfo = fileInfo;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#getJob()
	 */
	@Override
	public Job getJob() {
		return job;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#setJob(com.astrodoorways.db.filesystem.Job)
	 */
	@Override
	public void setJob(Job job) {
		this.job = job;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#getMetadata()
	 */
	@Override
	public IIOMetadata getMetadata() {
		return metadata;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#setMetadata(javax.imageio.metadata.IIOMetadata)
	 */
	@Override
	public void setMetadata(IIOMetadata metadata) {
		this.metadata = metadata;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#getFilePath()
	 */
	@Override
	public String getFilePath() {
		return filePath;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#setFilePath(java.lang.String)
	 */
	@Override
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#getCounter()
	 */
	@Override
	public AtomicInteger getCounter() {
		return counter;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#setCounter(java.util.concurrent.atomic.AtomicInteger)
	 */
	@Override
	public void setCounter(AtomicInteger counter) {
		this.counter = counter;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#getMaxValue()
	 */
	@Override
	public int getMaxValue() {
		return maxValue;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#setMaxValue(int)
	 */
	@Override
	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#getLogger()
	 */
	@Override
	public Logger getLogger() {
		return logger;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.MetadataProcessRunnable#setLogger(org.slf4j.Logger)
	 */
	@Override
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

}
