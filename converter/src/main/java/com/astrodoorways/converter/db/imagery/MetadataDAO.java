package com.astrodoorways.converter.db.imagery;

import com.astrodoorways.converter.db.filesystem.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("metadataDAO")
public interface MetadataDAO extends PagingAndSortingRepository<Metadata, Long> {

	List<Metadata> findByFileInfoJob(Job job);

	int countByFileInfoJob(Job job);

	Page<Metadata> findByFileInfoJob(Job job, Pageable pageable);
}