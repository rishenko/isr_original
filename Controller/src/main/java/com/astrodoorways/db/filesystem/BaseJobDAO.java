package com.astrodoorways.db.filesystem;

import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.astrodoorways.db.AbstractDao;

@Component
@Scope("prototype")
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class BaseJobDAO extends AbstractDao implements JobDAO {

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.filesystem.JobDAO#saveJob(com.astrodoorways.db.filesystem.Job)
	 */
	@Override
	public void saveJob(Job job) {
		getSession().saveOrUpdate(job);
		getSession().flush();
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.filesystem.JobDAO#getJob(java.lang.Long)
	 */
	@Override
	public Job getJob(Long id) {
		Job job = (Job) getSession().load(Job.class, id);
		return job;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.filesystem.JobDAO#getAllJobs()
	 */
	@Override
	public List<Job> getAllJobs() {
		List<Job> jobs = getSession().createCriteria(Job.class).list();
		return jobs;
	}
}
