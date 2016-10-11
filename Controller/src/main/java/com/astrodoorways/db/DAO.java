package com.astrodoorways.db;

import org.hibernate.Session;

public interface DAO {

	Session getSession();

	void flush();

	void clear();

	void evict(Object obj);

}