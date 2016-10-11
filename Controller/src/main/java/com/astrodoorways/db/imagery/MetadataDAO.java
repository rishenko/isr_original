package com.astrodoorways.db.imagery;

import java.util.Collection;
import java.util.List;

import org.hibernate.ScrollableResults;

import com.astrodoorways.db.DAO;
import com.astrodoorways.db.filesystem.Job;

public interface MetadataDAO extends DAO {

	void saveMetadata(Metadata metadata);

	ScrollableResults retrieveMetadata(Job job, List<String> targets, List<String> filters, List<String> orderBy);

	Collection<Metadata> retrieveDistinctMetadataGroups(Job job, List<String> targets, List<String> filters,
			List<String> orderBy);

	Collection<Metadata> retrieveMetadataByExample(Metadata example, String... orderBy);

	int retrieveMetadataCountByJob(Job job);

	Collection<Metadata> retrieveMetadataByJob(Job job);

	Collection<Metadata> distinctGroupings(Job job, List<String> targets, List<String> filters);

}