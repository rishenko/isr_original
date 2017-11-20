package com.astrodoorways.converter.db.imagery;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.astrodoorways.converter.db.filesystem.FileInfo;

@Entity(name = "metadata")
public class Metadata {
	private Long id;
	private String filterOne;
	private String filterTwo;
	private Double exposure;
	private String camera;
	private String target;
	private String mission;
	private Date time;

	private FileInfo fileInfo;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "metadata_dto_seq_gen")
	@SequenceGenerator(name = "metadata_dto_seq_gen", sequenceName = "METADATA_SEQ")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "filter_one")
	public String getFilterOne() {
		return filterOne;
	}

	public void setFilterOne(String filterOne) {
		this.filterOne = filterOne;
	}

	@Column(name = "filter_two")
	public String getFilterTwo() {
		return filterTwo;
	}

	public void setFilterTwo(String filterTwo) {
		this.filterTwo = filterTwo;
	}

	public Double getExposure() {
		return exposure;
	}

	public void setExposure(Double exposure) {
		this.exposure = exposure;
	}

	public String getCamera() {
		return camera;
	}

	public void setCamera(String camera) {
		this.camera = camera;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getMission() {
		return mission;
	}

	public void setMission(String mission) {
		this.mission = mission;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	//	@Transient
	public FileInfo getFileInfo() {
		return fileInfo;
	}

	public void setFileInfo(FileInfo fileInfo) {
		this.fileInfo = fileInfo;
	}

	public String toString() {
		return mission + "_" + target + "_" + filterOne + "_" + filterTwo + "_" + time + "_" + fileInfo;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Metadata)) {
			return false;
		}

		Metadata toCheck = (Metadata) obj;
		boolean isEqual = basicEqualityTest(mission, toCheck.getMission());
		isEqual = isEqual && basicEqualityTest(target, toCheck.getTarget());
		isEqual = isEqual && basicEqualityTest(filterOne, toCheck.getFilterOne());
		isEqual = isEqual && basicEqualityTest(filterTwo, toCheck.getFilterTwo());
		isEqual = isEqual && basicEqualityTest(time, toCheck.getTime());

		return isEqual;
	}

	public int hashCode() {
		int hashCode = 1;
		if (mission != null) {
			hashCode *= mission.hashCode();
		}
		if (target != null) {
			hashCode *= target.hashCode();
		}
		if (filterOne != null) {
			hashCode *= filterOne.hashCode();
		}
		if (filterTwo != null) {
			hashCode *= filterTwo.hashCode();
		}
		if (time != null) {
			hashCode *= time.hashCode();
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
