package com.astrodoorways.converter.db.filesystem;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
public class Job {
	private Long id;
	private String name;
	private Date date;
	private List<FileInfo> files = new ArrayList<FileInfo>();
	private List<JobParam> params = new ArrayList<JobParam>();

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "job_dto_seq_gen")
	@SequenceGenerator(name = "job_dto_seq_gen", sequenceName = "JOB_SEQ")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Column
	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "job")
	public List<FileInfo> getFiles() {
		return files;
	}

	public void setFiles(List<FileInfo> files) {
		this.files = files;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "job")
	public List<JobParam> getParams() {
		return params;
	}

	public void setParams(List<JobParam> params) {
		this.params = params;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Job)) {
			return false;
		}

		Job toCheck = (Job) obj;
		boolean isEqual = true;
		isEqual = isEqual && basicEqualityTest(name, toCheck.getName());
		isEqual = isEqual && basicEqualityTest(date, toCheck.getDate());
		isEqual = isEqual && basicEqualityTest(id, toCheck.getId());

		return isEqual;
	}

	public int hashCode() {
		int hashCode = 1;
		if (name != null) {
			hashCode *= name.hashCode();
		}
		if (date != null) {
			hashCode *= date.hashCode();
		}
		if (id != null) {
			hashCode *= id.hashCode();
		}
		return hashCode;
	}

	public boolean basicEqualityTest(Object obj, Object obj2) {
		boolean equal = true;
		if (obj != null && obj2 != null) {
			equal = obj.equals(obj2);
		} else if ((obj != null && obj2 == null) || (obj == null && obj2 != null)) {
			equal = false;
		}
		return equal;
	}
}
