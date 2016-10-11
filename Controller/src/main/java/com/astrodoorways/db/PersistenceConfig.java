package com.astrodoorways.db;

import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate3.HibernateTransactionManager;
import org.springframework.orm.hibernate3.annotation.AnnotationSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.TransactionManagementConfigurer;

@Configuration
@EnableTransactionManagement
@PropertySource("classpath:persistence-h2.properties")
@ComponentScan("com.astrodoorways")
public class PersistenceConfig implements TransactionManagementConfigurer {

	Logger logger = LoggerFactory.getLogger(PersistenceConfig.class);

	@Autowired
	private Environment env;

	@Bean
	public AnnotationSessionFactoryBean sessionFactory() {
		AnnotationSessionFactoryBean sessionFactory = new AnnotationSessionFactoryBean();
		sessionFactory.setDataSource(dataSource());
		sessionFactory.setPackagesToScan(new String[] { "com.astrodoorways" });
		sessionFactory.setHibernateProperties(hibernateProperties());
		logger.debug("returning sessionfactory");
		return sessionFactory;
	}

	@Bean
	public DataSource dataSource() {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(env.getProperty("jdbc.driverClassName"));
		dataSource.setUrl(env.getProperty("jdbc.url"));
		return dataSource;
	}

	@Bean
	public HibernateTransactionManager txManager() {
		HibernateTransactionManager txManager = new HibernateTransactionManager();
		txManager.setSessionFactory(sessionFactory().getObject());

		return txManager;
	}

	@Bean
	public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
		return new PersistenceExceptionTranslationPostProcessor();
	}

	@Override
	public HibernateTransactionManager annotationDrivenTransactionManager() {
		return txManager();
	}

	Properties hibernateProperties() {
		return new Properties() {
			{
				setProperty("hibernate.hbm2ddl.auto", env.getProperty("hbm2ddl.auto"));
				setProperty("hibernate.dialect", env.getProperty("dialect"));
				setProperty("connection.provider_class", env.getProperty("connection.provider_class"));
				setProperty("bonecp.partitionCount", env.getProperty("bonecp.partitionCount"));
				setProperty("bonecp.maxConnectionsPerPartition", env.getProperty("bonecp.maxConnectionsPerPartition"));
				setProperty("bonecp.minConnectionsPerPartition", env.getProperty("bonecp.minConnectionsPerPartition"));
				setProperty("bonecp.acquireIncrement", env.getProperty("bonecp.acquireIncrement"));
				setProperty("connection.pool_size", env.getProperty("connection.pool_size"));
				setProperty("cache.provider_class", env.getProperty("cache.provider_class"));
				setProperty("hibernate.show_sql", env.getProperty("show_sql"));
			}
		};
	}
}
