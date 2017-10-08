package com.astrodoorways.downloader.profiles;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class SingleFileProfile extends HtmlUnitProfile {

	public static final String singleFileUrl = "http://pds-imaging.jpl.nasa.gov/data/cassini/cassini_orbiter/coiss_2005/data/1469618524_1469750418/";

	public SingleFileProfile(String URL, String writeDirectory) {
		super(URL, writeDirectory);
	}

	@Override
	protected void buildLinksFile(HtmlPage page) {
		getPathGenerator().writeLine("N1469720016_2.IMG");
	}

	@Override
	public String getRemoteServer() {
		return singleFileUrl;
	}

}
