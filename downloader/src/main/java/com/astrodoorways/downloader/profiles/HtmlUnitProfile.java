package com.astrodoorways.downloader.profiles;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astrodoorways.filesystem.writers.LinesToFileWriter;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public abstract class HtmlUnitProfile extends AbstractBaseProfile {

	private Logger logger = LoggerFactory.getLogger(HtmlUnitProfile.class);

	public HtmlUnitProfile(String URL, String writeDirectory) {
		super(URL, writeDirectory);
		// TODO Auto-generated constructor stub
	}

	public HtmlUnitProfile(String URL, String writeDirectory, LinesToFileWriter writer) {
		super(URL, writeDirectory, writer);
	}

	public static int TIMEOUT_TIME = 15000;

	/* (non-Javadoc)
	 * @see com.astrodoorways.downloader.profiles.Profile#process()
	 */
	@Override
	public void process() {
		final WebClient webClient = new WebClient();
		webClient.getOptions().setTimeout(15000);
		HtmlPage page;
		boolean finished = false;
		while (!finished) {
			try {
				page = (HtmlPage) webClient.getPage(URL);
				logger.debug("about to process links for {}", URL);
				buildLinksFile(page);
				finished = true;
				page.cleanUp();
			} catch (FailingHttpStatusCodeException | IOException e) {
				logger.error("exception processing a link, will try again in " + TIMEOUT_TIME + " milliseconds", e);
				try {
					Thread.sleep(TIMEOUT_TIME);
				} catch (InterruptedException e1) {
					logger.error("failed while trying to wait {} milliseconds", TIMEOUT_TIME);
				}
				logger.debug("just woke up, trying again");
				finished = false;
			}
		}
		webClient.closeAllWindows();
		getPathGenerator().close();
	}

	protected abstract void buildLinksFile(HtmlPage page);

}
