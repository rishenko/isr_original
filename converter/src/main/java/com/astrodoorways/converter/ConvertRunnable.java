package com.astrodoorways.converter;

import be.pw.jexif.JExifTool;
import com.astrodoorways.converter.converters.VicarImageConverter;
import com.astrodoorways.converter.vicar.exif.VicarThreadedJEXIFConverter;
import com.astrodoorways.db.filesystem.FileInfo;
import com.astrodoorways.db.filesystem.FileInfoDAO;
import com.astrodoorways.db.imagery.Metadata;
import org.apache.commons.pool2.ObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EmptyStackException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Scope("prototype")
public class ConvertRunnable implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(ConvertRunnable.class);

	@Autowired
	private FileInfoDAO fileInfoDAO;

	private Metadata metadata;
	private String filePath;
	private Integer seqCount;
	private String writeDirectory;
	private ObjectPool<JExifTool> jexifToolPool;
	private AtomicInteger counter;
	private int maxValue;
	private String type;

	private static final long maxFileSizeForExif = 8 * 1024 * 1024 * 100; // byte * kilobyte * megabyte * #megabytes
	private static final List<String> ACCEPTED_EXTENSIONS = Arrays.asList("IMG", "TIFF", "tiff", "FIT", "FITS", "img",
			"fits", "fit");

	public ConvertRunnable() {
	}

	public ConvertRunnable(Metadata metadata, String writeDirectory, ObjectPool<JExifTool> jexifToolPool, String type,
			AtomicInteger counter, int maxValue) {
		this.metadata = metadata;
		this.writeDirectory = writeDirectory;
		this.jexifToolPool = jexifToolPool;
		this.counter = counter;
		this.maxValue = maxValue;
		this.type = type;
		filePath = metadata.getFileInfo().getFilePath();
	}

	public ConvertRunnable(Metadata metadata, Integer seqCount, String writeDirectory,
			ObjectPool<JExifTool> jexifToolPool, String type, AtomicInteger counter, int maxValue) {
		this.metadata = metadata;
		this.writeDirectory = writeDirectory;
		this.jexifToolPool = jexifToolPool;
		this.counter = counter;
		this.maxValue = maxValue;
		this.type = type;
		this.seqCount = seqCount;
		filePath = metadata.getFileInfo().getFilePath();
	}

	@Override
	public void run() {
		if (!ACCEPTED_EXTENSIONS.contains(metadata.getFileInfo().getExtension())) {
			return;
		}
		JExifTool tool = null;
		try {
			logger.trace("about to convert image {} to type {}", metadata.getFileInfo(), type);
			VicarImageConverter converter = new VicarImageConverter(metadata, seqCount, writeDirectory, type, counter);
			converter.convert();

			logger.trace("converted image {}", converter.getOutputtedFilePaths().toArray());
			// exif is an expensive operation, only use on smaller files, add override somehow
			if (new File(getOutputFilePath()).length() < maxFileSizeForExif) {
				tool = jexifToolPool.borrowObject();
				VicarThreadedJEXIFConverter exifConverter = new VicarThreadedJEXIFConverter(tool);
				for (String path : converter.getOutputtedFilePaths()) {
					exifConverter.convert(path, converter.getIIOMetaData());
				}
			}

			updateMetadata(converter.getOutputtedFilePaths());
			postProcessImage();
		} catch (IllegalStateException e) {
			String msg = e.getMessage();
			if (msg.contains("image already exists")) {
				logger.debug("{} - {}", msg, completionMessage());
				return;
			} else {
				logger.error("unknown state exception error", e);
			}
		} catch (ParseException e) {
			logger.error("error  parsing the date", e);
		} catch (IOException e) {
			logger.error("there was a problem converting the file " + filePath, e);
		} catch (Exception e) {
			try {
				logger.error("an unknown error {}", e);
			} catch (EmptyStackException ee) {
				logger.error("empty stack issue with {}", e.getClass());
			}
		} finally {
			if (tool != null) {
				try {
					jexifToolPool.returnObject(tool);
				} catch (Exception e) {
					logger.error("error returning the jexif tool to the pool", e);
				}
			}
		}
	}

	private void updateMetadata(List<String> outputtedFilePaths) {
		FileInfo fileInfo = metadata.getFileInfo();
		String outputPathList = "";
		for (String path : outputtedFilePaths) {
			outputPathList += path + ":::";
		}
		if (outputPathList.length() > 2048)
			outputPathList = outputPathList.substring(0, 2040) + "...";

		fileInfo.setOutputFileName(outputPathList);
		fileInfoDAO.save(fileInfo);
	}

	private void postProcessImage() {
		if (filePath.endsWith(".fits")) {
			String fzFilePath = filePath.substring(0, filePath.length() - 4) + "fz";
			File fzFile = new File(fzFilePath);
			if (fzFile.exists()) {
				new File(filePath).delete();
			}
			fzFile.renameTo(new File(filePath));
		}
	}

	private String completionMessage() {
		int counterVal = counter.incrementAndGet();
		int percentComplete = (int) (((double) counterVal / (double) maxValue) * 100.0);
		return String.format("converter is %d%% complete with %d out of %d processed", percentComplete, counterVal,
				maxValue);
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public String getOutputFilePath() throws Exception {
		if (filePath == null) {
			filePath = metadata.getTarget() + "_" + metadata.getMission() + "_" + metadata.getFilterOne() + "-"
					+ metadata.getFilterTwo();
			if (seqCount != null) {
				filePath += "_" + seqCount;
			} else {
				filePath += convertVicarDateToTimestamp(metadata.getTime());
			}
			filePath += "." + type;
		}
		return filePath;

	}

	public final SimpleDateFormat VICAR_FORMATTER = new SimpleDateFormat("yyyy-DDD'T'HH:mm:ss.SSS'Z'");
	public final SimpleDateFormat TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyyMMddhhmmssSSS");

	public String convertVicarDateToTimestamp(Date date) throws ParseException {
		String finalDate = TIMESTAMP_FORMATTER.format(date);
		return finalDate;
	}

	public Integer getSeqCount() {
		return seqCount;
	}

	public void setSeqCount(Integer seqCount) {
		this.seqCount = seqCount;
	}

	public String getWriteDirectory() {
		return writeDirectory;
	}

	public ObjectPool<JExifTool> getJexifToolPool() {
		return jexifToolPool;
	}

	public AtomicInteger getCounter() {
		return counter;
	}

	public int getMaxValue() {
		return maxValue;
	}

	public String getType() {
		return type;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public void setWriteDirectory(String writeDirectory) {
		this.writeDirectory = writeDirectory;
	}

	public void setJexifToolPool(ObjectPool<JExifTool> jexifToolPool) {
		this.jexifToolPool = jexifToolPool;
	}

	public void setCounter(AtomicInteger counter) {
		this.counter = counter;
	}

	public void setMaxValue(int maxValue) {
		this.maxValue = maxValue;
	}

	public void setType(String type) {
		this.type = type;
	}
}
