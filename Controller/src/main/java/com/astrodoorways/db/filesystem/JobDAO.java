package com.astrodoorways.db.filesystem;

import java.util.List;

import com.astrodoorways.db.DAO;

public interface JobDAO extends DAO {

	void saveJob(Job job);

	Job getJob(Long id);

	List<Job> getAllJobs();

}