package com.astrodoorways.db.filesystem;

import org.hibernate.ScrollableResults;

import com.astrodoorways.db.DAO;

public interface FileInfoDAO extends DAO {

	void saveFilePath(FileInfo fileStructureDto);

	ScrollableResults searchByExample(FileInfo dto);

	int retrieveFileInfoCountByJob(Job job);

}