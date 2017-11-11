package com.astrodoorways.db.filesystem;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileInfoDAO extends CrudRepository<FileInfo,Long> {
	int countByJob(Job job);
	List<FileInfo> findByJob(Job job);
}