package com.astrodoorways;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.astrodoorways.downloader.Downloader;
import com.astrodoorways.downloader.cli.RunDownloader;
import com.astrodoorways.downloader.profiles.HtmlUnitProfile;
import com.astrodoorways.downloader.profiles.Profile;
import com.astrodoorways.downloader.profiles.misc.IncrementalFileProfile;
import com.astrodoorways.downloader.profiles.pds.PDSArchiveProfile;
import com.astrodoorways.downloader.profiles.pds.rings.PDSRingsArchiveProfile;
import com.astrodoorways.downloader.profiles.sdo.SDOProfile;
import com.astrodoorways.filesystem.writers.LinesToFileWriter;

@RunWith(JUnit4.class)
public class RunDownloaderTest {

	@Test
	public void downloaderCassiniRawArchiveTest() {
		String remoteUrl = "http://saturn.jpl.nasa.gov/multimedia/images/raw/casJPGFullS77/";
		String writeDirectory = "/Users/kmcabee/Desktop/downloaderCassiniRawRev183-0306/";
		System.setProperty(RunDownloader.MAX_NUM_PROCESSORS, "4");
		System.setProperty(RunDownloader.SYSTEM_PERCENT_UTILIZATION, "100");
		Downloader downloader = new Downloader(writeDirectory, remoteUrl);
		LinesToFileWriter pathGeneratedWriter = new LinesToFileWriter(
				new File(writeDirectory + "generatedPathList.txt"), true);
		//N00203494.jpg N00203608.jpg  635
		//W00079821.jpg W00079869.jpg
		Profile profile = new IncrementalFileProfile(821, 869, "W00079%03d.jpg", remoteUrl, writeDirectory,
				pathGeneratedWriter);
		downloader.getProfiles().add(profile);
		downloader.processProfiles();
	}

	//	@Test
	public void downloaderPdsTest() {
		String remoteUrl = "http://pds-imaging.jpl.nasa.gov/data/cassini/cassini_orbiter/coiss_2071/";
		String writeDirectory = "/Users/kmcabee/Desktop/downloaderTest2071/";
		System.setProperty(RunDownloader.MAX_NUM_PROCESSORS, "4");
		System.setProperty(RunDownloader.SYSTEM_PERCENT_UTILIZATION, "100");
		Downloader downloader = new Downloader(writeDirectory, remoteUrl);
		LinesToFileWriter pathCheckListWriter = new LinesToFileWriter(new File(writeDirectory
				+ "generatedPathCheckList.txt"), true);
		LinesToFileWriter pathGeneratedWriter = new LinesToFileWriter(
				new File(writeDirectory + "generatedPathList.txt"), true);
		HtmlUnitProfile profile = new PDSArchiveProfile(remoteUrl, writeDirectory, pathCheckListWriter,
				pathGeneratedWriter, "img", "IMG", "lbl", "LBL");
		downloader.getProfiles().add(profile);
		downloader.processProfiles();
	}

	//		@Test
	public void downloaderPdsRingsTest() {
		String remoteUrl = "http://pds-rings.seti.org/vol/NHJULO_2001/";
		String writeDirectory = "/Users/kmcabee/Desktop/downloaderTestNH/";
		Downloader downloader = new Downloader(writeDirectory, remoteUrl);
		LinesToFileWriter pathCheckListWriter = new LinesToFileWriter(new File(writeDirectory
				+ "generatedPathCheckList.txt"), true);
		LinesToFileWriter pathGeneratedWriter = new LinesToFileWriter(
				new File(writeDirectory + "generatedPathList.txt"), true);
		HtmlUnitProfile profile = new PDSRingsArchiveProfile(remoteUrl, writeDirectory, pathCheckListWriter,
				pathGeneratedWriter, "FIT", "LBL");
		downloader.getProfiles().add(profile);
		downloader.processProfiles();
	}

	//	@Test
	public void downloaderSDOTest() {
		String remoteUrl = "";
		String writeDirectory = "/Users/kmcabee/Desktop/downloaderSDOTest/";
		Downloader downloader = new Downloader(writeDirectory, remoteUrl);
		LinesToFileWriter pathCheckListWriter = new LinesToFileWriter(new File(writeDirectory
				+ "generatedPathCheckList.txt"), true);
		LinesToFileWriter pathGeneratedWriter = new LinesToFileWriter(
				new File(writeDirectory + "generatedPathList.txt"), true);
		String cadence = "hmi720";

		Calendar nov2012 = Calendar.getInstance();
		nov2012.set(2012, 10, 1, 0, 0, 0);
		SimpleDateFormat SDO_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		String startDate = SDO_FORMATTER.format(nov2012.getTime());

		String timeRange = "2h";
		String frequency = "1h";
		Integer iterations = 2;
		Profile profile = new SDOProfile(writeDirectory, cadence, startDate, timeRange, frequency, iterations, null);//"aia.lev1_euv_12s[2012-11-01T00:11:00Z/30h@1h]");
		downloader.getProfiles().add(profile);
		downloader.processProfiles();
	}

	//	@Test
	public void runDownloaderTest() {
		RunDownloader.main();
	}
}
