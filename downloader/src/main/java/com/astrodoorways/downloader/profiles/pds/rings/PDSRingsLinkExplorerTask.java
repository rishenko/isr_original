package com.astrodoorways.downloader.profiles.pds.rings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.RecursiveTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astrodoorways.filesystem.writers.LinesToFileWriter;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class PDSRingsLinkExplorerTask extends RecursiveTask<String> {

	private static final long serialVersionUID = 3120970443923002197L;

	public static final int TIMEOUT_TIME = 15000; // timeout in milliseconds

	Logger logger = LoggerFactory.getLogger(PDSRingsLinkExplorerTask.class);

	private final String remoteServer;
	private final String link;
	private final String writeDirectory;
	private final LinesToFileWriter pathGenerator;
	private final LinesToFileWriter pathCheckList;
	private final List<String> acceptableFiles;

	PDSRingsLinkExplorerTask(String remoteServer, String link, String writeDirectory, LinesToFileWriter pathGenerator,
			LinesToFileWriter pathCheckList, List<String> acceptableFiles) {
		this.remoteServer = remoteServer;
		this.link = link;
		this.writeDirectory = writeDirectory;
		this.pathGenerator = pathGenerator;
		this.pathCheckList = pathCheckList;
		this.acceptableFiles = acceptableFiles;
	}

	public String compute() {
		try {
			recursiveLinkExplorer(link);
		} catch (FailingHttpStatusCodeException | IOException e) {
			logger.error("there as an error performing recursive exploration", e);
		}
		return "";
	}

	@SuppressWarnings("unchecked")
	public void recursiveLinkExplorer(String link) throws FailingHttpStatusCodeException, MalformedURLException,
			IOException {
		if (link.endsWith("/") && directoryPreviouslyMapped(link)) {
			return;
		}
		boolean finished = false;
		while (!finished) {
			try {
				if (link.endsWith("/") && !link.startsWith("/")) {
					final WebClient webClient = new WebClient();
					webClient.setTimeout(TIMEOUT_TIME);
					final HtmlPage page = (HtmlPage) webClient.getPage(getRemoteServer() + link);
					List<HtmlAnchor> pageLinks = (List<HtmlAnchor>) page.getByXPath("//table//a");
					List<PDSRingsLinkExplorerTask> tasks = new ArrayList<PDSRingsLinkExplorerTask>();
					for (HtmlAnchor anchor : pageLinks) {
						String href = anchor.getHrefAttribute();
						if (!link.contains(href)) {
							String finalLink = href.startsWith("/") ? href : link + href;
							tasks.add(new PDSRingsLinkExplorerTask(getRemoteServer(), finalLink, getWriteDirectory(),
									getPathGenerator(), pathCheckList, acceptableFiles));
						}
					}
					invokeAll(tasks);
					// write out that the directory has been searched
					pathCheckList.writeLine(getRemoteServer() + link);
				}

				String extension = link.substring(link.length() - 3, link.length());
				if (acceptableFiles.contains(extension) && !linkPreviouslyMapped(link)) {
					getPathGenerator().writeLine(link);
				}
				finished = true;
			} catch (IOException e) {
				logger.error("exception while recursively exploring link: " + getRemoteServer() + link, e);
				logger.debug("will try again in {} milliseconds", TIMEOUT_TIME);
				try {
					Thread.sleep(TIMEOUT_TIME);
				} catch (InterruptedException e1) {
					logger.error("failed while trying to wait {} milliseconds", TIMEOUT_TIME);
				}
				logger.debug("just woke up, trying again");
				finished = false;
			}
		}
	}

	private boolean directoryPreviouslyMapped(String link) throws FileNotFoundException {
		File pathCheckListFile = new File(pathCheckList.getFileAbsolutePath());
		if (!pathCheckListFile.canRead())
			return false;
		Scanner scanner = new Scanner(pathCheckListFile);
		String url = getRemoteServer() + link;
		String pattern = url + "\\B";
		String scannerFound = scanner.findWithinHorizon(pattern, 0);
		scanner.close();
		boolean found = scannerFound != null;
		if (found) {
			logger.debug("directory was previously mapped {}", url);
		} else {
			logger.debug("directory was NOT previously mapped {}", url);
		}
		return found;
	}

	private boolean linkPreviouslyMapped(String link) throws FileNotFoundException {
		File pathCheckList = new File(getWriteDirectory() + "/" + PDSRingsArchiveProfile.PATH_FILE_NAME);
		if (!pathCheckList.canRead())
			return false;
		Scanner scanner = new Scanner(pathCheckList);
		String scannerFound = scanner.findWithinHorizon(link, 0);
		boolean found = scannerFound != null;
		if (found) {
			logger.debug("link was previously mapped {}", getRemoteServer() + link);
		}
		scanner.close();
		return found;
	}

	public String getWriteDirectory() {
		return writeDirectory;
	}

	public String getRemoteServer() {
		return remoteServer;
	}

	public LinesToFileWriter getPathGenerator() {
		return pathGenerator;
	}
}
