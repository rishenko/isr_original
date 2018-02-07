package com.astrodoorways.converter.db.filesystem;

import com.astrodoorways.converter.db.imagery.Metadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileInfoDAO extends PagingAndSortingRepository<FileInfo,Long> {
	int countByJob(Job job);
	List<FileInfo> findByJob(Job job);

	Page<FileInfo> findByJob(Job job, Pageable pageable);
}