package com.astrodoorways.downloader.profiles.pds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astrodoorways.downloader.profiles.HtmlUnitProfile;
import com.astrodoorways.filesystem.writers.LinesToFileWriter;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Generic profile that can build a download list of archived data from NASA PDS data repositories.
 * 
 * @author kmcabee
 *
 */
public class PDSArchiveProfile extends HtmlUnitProfile {

	private static final Logger logger = LoggerFactory.getLogger(PDSArchiveProfile.class);
	public List<String> ACCEPTABLE_FILES = new ArrayList<String>();
	public static final int TIMEOUT_TIME = 15000;

	private LinesToFileWriter pathCheckList;

	public PDSArchiveProfile(String url, String writeDirectory, LinesToFileWriter pathCheckListWriter,
			LinesToFileWriter pathResultListWriter, String... acceptedExtensions) {
		super(url, writeDirectory, pathResultListWriter);
		if (acceptedExtensions != null) {
			ACCEPTABLE_FILES.addAll(Arrays.asList(acceptedExtensions));
		} else {
			ACCEPTABLE_FILES.addAll(Arrays.asList(new String[] { "img", "IMG", "lbl", "LBL", "FITS", "fits" }));
		}
		pathCheckList = pathCheckListWriter;
	}

	public PDSArchiveProfile(String url, String writeDirectory, LinesToFileWriter pathCheckListWriter,
			LinesToFileWriter pathResultListWriter, List<String> acceptedExtensions) {
		this(url, writeDirectory, pathCheckListWriter, pathResultListWriter, (String[]) acceptedExtensions.toArray());
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void buildLinksFile(HtmlPage page) {
		try {
			int numProcessors = System.getProperties().containsKey("max.num.processors") ? Integer.parseInt(System
					.getProperty("max.num.processors")) : Runtime.getRuntime().availableProcessors();
			logger.debug("number of processors for file download: {}", numProcessors);
			ForkJoinPool recursivePool = new ForkJoinPool(numProcessors);
			List<HtmlAnchor> links = (List<HtmlAnchor>) page.getByXPath("//a");
			for (HtmlAnchor link : links) {
				PDSLinkExplorerTask task = new PDSLinkExplorerTask(getRemoteServer(), link.getHrefAttribute(),
						getWriteDirectory(), getPathGenerator(), pathCheckList, ACCEPTABLE_FILES);
				recursivePool.execute(task);
			}
			page.cleanUp();
			logger.debug("top level links are being processed");
			while (recursivePool.hasQueuedSubmissions()) {

			}
			logger.debug("all tasks have been queued and are now running");
			recursivePool.shutdown();
			while (!recursivePool.isTerminated()) {

			}
			logger.debug("recursive pool for profile has terminated");
			pathCheckList.close();
		} catch (FailingHttpStatusCodeException e) {
			logger.error("error processing site", e);
		}
	}

	public String getDirExtension(Integer count) {
		return StringUtils.leftPad(count.toString(), 4, "0");
	}

	public String getRemoteServer() {
		return getURL();
	}
}
