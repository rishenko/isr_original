package com.astrodoorways.db.filesystem;

import java.math.BigInteger;

import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.astrodoorways.db.AbstractDao;

@Component
@Scope("prototype")
@Transactional
public class BaseFileInfoDAO extends AbstractDao implements FileInfoDAO {

	Logger logger = LoggerFactory.getLogger(BaseFileInfoDAO.class);

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.filesystem.FileInfoDAO#saveFilePath(com.astrodoorways.db.filesystem.FileInfo)
	 */
	@Override
	public void saveFilePath(FileInfo fileStructureDto) {
		getSession().saveOrUpdate(fileStructureDto);
		getSession().flush();
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.filesystem.FileInfoDAO#searchByExample(com.astrodoorways.db.filesystem.FileInfo)
	 */
	@Override
	public ScrollableResults searchByExample(FileInfo dto) {
		return getSession().createCriteria(FileInfo.class).add(Restrictions.eq("job.id", dto.getJob().getId()))
				.scroll();
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.filesystem.FileInfoDAO#retrieveFileInfoCountByJob(com.astrodoorways.db.filesystem.Job)
	 */
	@Override
	public int retrieveFileInfoCountByJob(Job job) {
		String sql = "select count(id) from file_structure where job_id=?";
		Query query = getSession().createSQLQuery(sql).setParameter(0, job.getId());
		return ((BigInteger) query.list().get(0)).intValue();
	}
}
