package com.astrodoorways.converter.db.filesystem;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;

@Entity(name = "job_param")
public class JobParam {
	private Long id;
	private Job job;
	private String name;
	private String value;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "job_param_seq_gen")
	@SequenceGenerator(name = "job_param_seq_gen", sequenceName = "JOB_PARAM_SEQ")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@ManyToOne
	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
