package com.astrodoorways.db.imagery;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.Collections;
import org.hibernate.transform.Transformers;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.astrodoorways.db.AbstractDao;
import com.astrodoorways.db.filesystem.Job;
import org.springframework.util.CollectionUtils;

@Component
@Scope("prototype")
@Transactional
public class BaseMetadataDAO extends AbstractDao implements MetadataDAO {

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.imagery.MetadataDAO#saveMetadata(com.astrodoorways.db.imagery.Metadata)
	 */
	@Override
	public void saveMetadata(Metadata metadata) {

		getSession().saveOrUpdate(metadata);
		getSession().flush();

	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.imagery.MetadataDAO#retrieveMetadata(com.astrodoorways.db.filesystem.Job, java.util.List, java.util.List, java.util.List)
	 */
	@Override
	public ScrollableResults retrieveMetadata(Job job, List<String> targets, List<String> filters, List<String> orderBy) {

		Criteria criteria = getSession().createCriteria(Metadata.class);

		criteria.createCriteria("fileInfo").add(Restrictions.eq("job.id", job.getId()));
		if (!CollectionUtils.isEmpty(filters)) {
			criteria.add(Restrictions.in("filterOne", filters));
		}
		if (!CollectionUtils.isEmpty(targets)) {
			criteria.add(Restrictions.in("target", targets));
		}
		if (!CollectionUtils.isEmpty(orderBy)) {
			for (String order : orderBy) {
				criteria.addOrder(Order.asc(order));
			}
		}

		return criteria.scroll(ScrollMode.FORWARD_ONLY);
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.imagery.MetadataDAO#retrieveDistinctMetadataGroups(com.astrodoorways.db.filesystem.Job, java.util.List, java.util.List, java.util.List)
	 */
	@Override
	public Collection<Metadata> retrieveDistinctMetadataGroups(Job job, List<String> targets, List<String> filters,
			List<String> orderBy) {

		String sql = "select distinct mission \"mission\", target \"target\", filter_one \"filterOne\", filter_two \"filterTwo\" from metadata  m inner join file_structure fs on m.fileinfo_id=fs.id where target in ?, fs.job_id=?";
		Collection<Metadata> results = getSession().createSQLQuery(sql)
				.setResultTransformer(Transformers.aliasToBean(Metadata.class)).setParameter(0, job.getId()).list();

		return results;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.imagery.MetadataDAO#retrieveMetadataByExample(com.astrodoorways.db.imagery.Metadata, java.lang.String)
	 */
	@Override
	public Collection<Metadata> retrieveMetadataByExample(Metadata example, String... orderBy) {

		Criteria criteria = getSession().createCriteria(Metadata.class);
		criteria.add(Example.create(example));
		for (String order : orderBy) {
			criteria.addOrder(Order.asc(order));
		}
		Collection<Metadata> results = criteria.list();

		return results;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.imagery.MetadataDAO#retrieveMetadataCountByJob(com.astrodoorways.db.filesystem.Job)
	 */
	@Override
	public int retrieveMetadataCountByJob(Job job) {
		String sql = "select count(m.id) from metadata m inner join file_structure fs on m.fileinfo_id=fs.id where fs.job_id=?";
		Query query = getSession().createSQLQuery(sql).setParameter(0, job.getId());
		return ((BigInteger) query.list().get(0)).intValue();
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.imagery.MetadataDAO#retrieveMetadataByJob(com.astrodoorways.db.filesystem.Job)
	 */
	@Override
	public Collection<Metadata> retrieveMetadataByJob(Job job) {

		Collection<Metadata> results = getSession().createCriteria(Metadata.class)
				.add(Restrictions.eq("job.id", job.getId())).setFetchSize(1000).list();

		return results;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.imagery.MetadataDAO#distinctGroupings(com.astrodoorways.db.filesystem.Job, java.util.List, java.util.List)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Collection<Metadata> distinctGroupings(Job job, List<String> targets, List<String> filters) {

		String sql = "select distinct mission, target, filterOne, filterTwo from metadata join fileInfo as fileInfo where fileInfo.job = :job";
		if (!CollectionUtils.isEmpty(targets)) {
			sql += " targets in (:targets)";
		}

		if (!CollectionUtils.isEmpty(filters)) {
			sql += " filter in (:filters)";
		}

		Query query = getSession().createQuery(sql);
		query.setParameter(":job", job);
		if (!CollectionUtils.isEmpty(targets)) {
			query.setParameterList(":targets", targets);
		}

		if (!CollectionUtils.isEmpty(filters)) {
			query.setParameter(":filters", filters);
		}

		Collection<Metadata> results = query.setResultTransformer(Transformers.aliasToBean(Metadata.class)).list();

		return results;
	}
}
