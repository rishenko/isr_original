package com.astrodoorways.converter.db.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.astrodoorways.filesystem.writers.FileStructureToFileWriter;
import com.astrodoorways.filesystem.writers.FileStructureWriter;

@Component
@Scope("prototype")
@Transactional
public class BaseFileStructureToDatabaseWriter implements FileStructureWriter, FileStructureToDatabaseWriter {
	Logger logger = LoggerFactory.getLogger(FileStructureToFileWriter.class);

	private Set<String> acceptedExtensions = new HashSet<String>();

	@Autowired
	private FileInfoDAO fileInfoDAO;
	@Autowired
	private JobDAO jobDAO;

	private Job job;
	private int count = 0;

	public BaseFileStructureToDatabaseWriter() {
		String[] extensions = new String[] { "img", "fit", "fits", "fz" };
		this.acceptedExtensions = new HashSet<String>();
		for (String extension : extensions) {
			acceptedExtensions.add(extension.toLowerCase());
		}
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.db.filesystem.FileStructureToDatabaseWriter#writeFileStructure(java.io.File)
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void writeFileStructure(File file) throws IOException {
		if (!file.exists()) {
			logger.error("this file does not exist {}", file.getAbsolutePath());
			throw new IOException("this file does not exist " + file.getAbsolutePath());
		}
		// if file is a directory, recursively call writeFileStructure on its
		// children
		if (file.isDirectory()) {
			for (File child : file.listFiles()) {
				writeFileStructure(child);
			}
		} else {
			// if the file is of the proper type, write out its path
			String fileName = file.getName();
			int positionLastPeriod = fileName.lastIndexOf(".");
			String extension = fileName.substring(positionLastPeriod + 1, fileName.length());

			if (acceptedExtensions.contains(extension.toLowerCase())) {
				FileInfo fileInfo = new FileInfo();
				fileInfo.setFileName(fileName);
				fileInfo.setExtension(extension);
				fileInfo.setDirectory(file.getParentFile().getCanonicalPath());
				job.getFiles().add(fileInfo);
				fileInfo.setJob(job);
				getFileInfoDAO().save(fileInfo);
				logger.debug("{} file added to the database: {}", new Object[] { count++, fileInfo.getFilePath() });
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.db.filesystem.FileStructureToDatabaseWriter#getCollectionOfFiles()
	 */
	@Override
	public Collection<String> getCollectionOfFiles() {
		List<FileInfo> results = getFileInfoDAO().findByJob(job);
		List<String> fileNames = new ArrayList<String>();
		for (FileInfo result: results) {
			fileNames.add(result.getDirectory() + "/" + result.getFileName());
		}
		return fileNames;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.db.filesystem.FileStructureToDatabaseWriter#getFileInfos()
	 */
	@Override
	public List<FileInfo> getFileInfos() {
		return getFileInfoDAO().findByJob(job);
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.db.filesystem.FileStructureToDatabaseWriter#getFileInfoDAO()
	 */
	@Override
	public FileInfoDAO getFileInfoDAO() {
		return fileInfoDAO;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.db.filesystem.FileStructureToDatabaseWriter#setFileStructureDAO(com.astrodoorways.converter.db.filesystem.FileInfoDAO)
	 */
	@Override
	public void setFileStructureDAO(FileInfoDAO fileStructureDAO) {
		this.fileInfoDAO = fileStructureDAO;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.db.filesystem.FileStructureToDatabaseWriter#getJob()
	 */
	@Override
	public Job getJob() {
		return job;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.converter.db.filesystem.FileStructureToDatabaseWriter#setJob(com.astrodoorways.converter.db.filesystem.Job)
	 */
	@Override
	public void setJob(Job job) {
		this.job = job;
	}
}
