package com.astrodoorways.downloader.profiles;

import com.astrodoorways.filesystem.writers.LinesToFileWriter;

public abstract class AbstractBaseProfile implements Profile {

	private static final long serialVersionUID = 7488024008835524586L;
	protected final String URL;
	private final LinesToFileWriter pathGenerator;
	private final String writeDirectory;

	public static final String PATH_FILE_NAME = "generatedListOfFiles.txt";

	public AbstractBaseProfile(String URL, String writeDirectory) {
		this(URL, writeDirectory, new LinesToFileWriter(writeDirectory, PATH_FILE_NAME, true));
	}

	public AbstractBaseProfile(String URL, String writeDirectory, LinesToFileWriter writer) {
		if (!URL.endsWith("/")) {
			URL += "/";
		}

		this.URL = URL;
		this.writeDirectory = writeDirectory;
		this.pathGenerator = writer;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.downloader.profiles.Profile#getURL()
	 */
	@Override
	public String getURL() {
		return URL;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.downloader.profiles.Profile#getRemoteServer()
	 */
	@Override
	public abstract String getRemoteServer();

	/* (non-Javadoc)
	 * @see com.astrodoorways.downloader.profiles.Profile#getPathGenerator()
	 */
	@Override
	public LinesToFileWriter getPathGenerator() {
		return pathGenerator;
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.downloader.profiles.Profile#getLinksFileName()
	 */
	@Override
	public String getLinksFileName() {
		return pathGenerator.getFileAbsolutePath();
	}

	/* (non-Javadoc)
	 * @see com.astrodoorways.downloader.profiles.Profile#getWriteDirectory()
	 */
	@Override
	public String getWriteDirectory() {
		return writeDirectory;
	}

}
