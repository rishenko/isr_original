package com.astrodoorways.db.filesystem;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import com.astrodoorways.db.imagery.Metadata;

@Entity(name = "file_structure")
public class FileInfo {
	private Long id;
	private String fileName;
	private String preprocessedFileName;
	private String outputFileName;
	private String directory;
	private String extension;
	private Job job;
	private List<Metadata> metadata = new ArrayList<Metadata>();

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "file_struct_dto_seq_gen")
	@SequenceGenerator(name = "file_struct_dto_seq_gen", sequenceName = "FILESTRUCT_DTO_SEQ")
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	@Column(name = "output_file_name", length = 2048)
	public String getOutputFileName() {
		return outputFileName;
	}

	public void setOutputFileName(String outputFileName) {
		this.outputFileName = outputFileName;
	}

	@Column(name = "preprocessed_file_name")
	public String getPreprocessedFileName() {
		return preprocessedFileName;
	}

	public void setPreprocessedFileName(String preprocessedFileName) {
		this.preprocessedFileName = preprocessedFileName;
	}

	@Column(name = "file_name")
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Column(name = "directory")
	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	@Column(name = "extension")
	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	@ManyToOne
	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public void setMetadata(List<Metadata> metadata) {
		this.metadata = metadata;
	}

	@Transient
	public String getFilePath() {
		return getDirectory() + "/" + getFileName();
	}

	public String toString() { return getFileName(); }
}
