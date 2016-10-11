package com.astrodoorways.downloader.profiles.pds.rings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astrodoorways.downloader.profiles.HtmlUnitProfile;
import com.astrodoorways.filesystem.writers.LinesToFileWriter;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class PDSRingsArchiveProfile extends HtmlUnitProfile {

	private static final Logger logger = LoggerFactory.getLogger(PDSRingsArchiveProfile.class);

	public List<String> ACCEPTABLE_FILES = new ArrayList<String>();
	private LinesToFileWriter pathCheckList;

	public PDSRingsArchiveProfile(String url, String writeDirectory, LinesToFileWriter pathCheckListWriter,
			LinesToFileWriter pathResultListWriter, String... acceptedExtensions) {
		super(url, writeDirectory, pathResultListWriter);
		if (acceptedExtensions != null) {
			ACCEPTABLE_FILES.addAll(Arrays.asList(acceptedExtensions));
		} else {
			ACCEPTABLE_FILES.addAll(Arrays.asList(new String[] { "FIT", "LBL", "fit", "lbl" }));
		}
		pathCheckList = pathCheckListWriter;
	}

	public PDSRingsArchiveProfile(String url, String writeDirectory, LinesToFileWriter pathCheckListWriter,
			LinesToFileWriter pathResultListWriter, List<String> acceptedExtensions) {
		this(url, writeDirectory, pathCheckListWriter, pathResultListWriter, (String[]) acceptedExtensions.toArray());
	}

	@Override
	protected void buildLinksFile(HtmlPage page) {
		try {
			ForkJoinPool recursivePool = new ForkJoinPool();
			List<HtmlAnchor> links = (List<HtmlAnchor>) page.getByXPath("//table//a");
			for (HtmlAnchor link : links) {
				PDSRingsLinkExplorerTask task = new PDSRingsLinkExplorerTask(getRemoteServer(),
						link.getHrefAttribute(), getWriteDirectory(), getPathGenerator(), pathCheckList,
						ACCEPTABLE_FILES);
				recursivePool.execute(task);
			}
			logger.debug("top level links are being processed");
			while (recursivePool.hasQueuedSubmissions()) {

			}
			logger.debug("all tasks have been queued and are now running");
			recursivePool.shutdown();
			while (!recursivePool.isTerminated()) {

			}
			logger.debug("recursive pool for profile has terminated");
		} catch (FailingHttpStatusCodeException e) {
			logger.error("error processing site", e);
		}
	}

	@Override
	public String getRemoteServer() {
		return getURL();
	}

}
