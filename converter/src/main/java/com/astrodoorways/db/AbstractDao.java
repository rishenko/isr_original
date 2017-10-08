package com.astrodoorways.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class AbstractDao implements DAO {

	@Autowired
	private SessionFactory sessionFactory;

	private Session session;

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.DAO#getSession()
	 */
	@Override
	public Session getSession() {
		if (session == null || !session.isOpen()) {
			session = sessionFactory.getCurrentSession();
		}
		return session;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.DAO#flush()
	 */
	@Override
	public void flush() {
		getSession().flush();
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.DAO#clear()
	 */
	@Override
	public void clear() {
		getSession().clear();
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.db.DAO#evict(java.lang.Object)
	 */
	@Override
	public void evict(Object obj) {
		getSession().evict(obj);
	}
}
