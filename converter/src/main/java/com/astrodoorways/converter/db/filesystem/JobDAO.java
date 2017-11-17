package com.astrodoorways.converter.db.filesystem;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("jobDAO")
public interface JobDAO extends JpaRepository<Job, Long> {}