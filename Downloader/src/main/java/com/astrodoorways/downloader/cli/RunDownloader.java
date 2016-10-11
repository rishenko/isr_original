package com.astrodoorways.downloader.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astrodoorways.downloader.Downloader;
import com.astrodoorways.downloader.profiles.Profile;
import com.astrodoorways.downloader.profiles.pds.PDSArchiveProfile;
import com.astrodoorways.downloader.profiles.sdo.SDOProfile;
import com.astrodoorways.filesystem.writers.LinesToFileWriter;

public class RunDownloader {

	private static Logger logger = LoggerFactory.getLogger(RunDownloader.class);

	private static String writeDirectory = "";
	private static String remoteUrl = "";
	private static String profile = "";
	private static LinesToFileWriter pathCompleteWriter;
	private static LinesToFileWriter pathGeneratedWriter;

	public static final String DOWNLOADER_PROFILE = "downloader.profile";
	public static final String WRITE_DIRECTORY_PROPERTY = "files.directory.write";
	public static final String REMOTE_URL = "files.remote.url";
	public static final String ACCEPTED_EXTENSIONS = "files.remote.accepted.extensions";

	public static final String SYSTEM_PERCENT_UTILIZATION = "system.percent.utilization";
	public static final String MAX_NUM_PROCESSORS = "max.num.processors";

	private Properties properties;

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public static void main(String... args) {
		Properties props = processProperties("./RunDownloader.properties");
		if (!writeDirectory.endsWith("/")) {
			writeDirectory += "/";
		}

		Downloader downloader = new Downloader(writeDirectory, remoteUrl);
		pathCompleteWriter = new LinesToFileWriter(new File(writeDirectory + "generatedPathCheckList.txt"), true);
		pathGeneratedWriter = new LinesToFileWriter(new File(writeDirectory + "generatedPathList.txt"), true);

		buildProfileList(downloader, props);
		downloader.processProfiles();
	}

	private static void buildProfileList(Downloader downloader, Properties props) {
		if (profile.equals("archive")) {
			Profile profile = new PDSArchiveProfile(remoteUrl, writeDirectory, pathCompleteWriter, pathGeneratedWriter,
					"img", "IMG", "lbl", "LBL", "FITS", "fits", "FIT", "fit");
			downloader.getProfiles().add(profile);
		} else if (profile.equals("sdo")) {

			String wavelength = null;
			if (props.containsKey("sdo.wavelength")) {
				wavelength = props.getProperty("sdo.wavelength");
			}
			// "aia.lev1_euv_12s[2012-11-01T00:10:00Z/30h@1h][?QUALITY>=0?]"
			Profile profile = new SDOProfile(writeDirectory, props.getProperty("sdo.cadence"),
					props.getProperty("sdo.startDate"), props.getProperty("sdo.timeRange"),
					props.getProperty("sdo.frequency"), Integer.parseInt(props.getProperty("sdo.numIterations")),
					wavelength);
			downloader.getProfiles().add(profile);
		}
	}

	/**
	 * @param args
	 *            process program properties based on the passed in arguments
	 */
	public static Properties processProperties(String arg) {

		Properties prop = new Properties();
		File file = new File(arg);

		try {
			InputStream stream = new FileInputStream(file);
			prop.load(stream);
		} catch (IOException e) {
			logger.error("There was an error reading the properties file.", e);
			throw new RuntimeException("There was an error reading the properties file", e);
		}

		// parse properties file for values
		if (!prop.containsKey(WRITE_DIRECTORY_PROPERTY) || !prop.containsKey(REMOTE_URL)
				|| !prop.containsKey(DOWNLOADER_PROFILE)) {
			logger.debug("properties {}", prop);
			logger.error("The properties file must have both the {}, {}, and {} properties set.", new Object[] {
					WRITE_DIRECTORY_PROPERTY, REMOTE_URL, DOWNLOADER_PROFILE });
			throw new RuntimeException(
					"The properties file must have both the image.directory.read and image.directory.write properties set.");
		}

		profile = prop.getProperty(DOWNLOADER_PROFILE);
		writeDirectory = prop.getProperty(WRITE_DIRECTORY_PROPERTY);
		remoteUrl = prop.getProperty(REMOTE_URL);

		if (prop.containsKey(SYSTEM_PERCENT_UTILIZATION)) {
			System.setProperty(SYSTEM_PERCENT_UTILIZATION, prop.getProperty(SYSTEM_PERCENT_UTILIZATION));
		}

		if (prop.containsKey(MAX_NUM_PROCESSORS)) {
			System.setProperty(MAX_NUM_PROCESSORS, prop.getProperty(MAX_NUM_PROCESSORS));
		}

		return prop;
	}
}
