package com.astrodoorways.downloader.profiles.sdo;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astrodoorways.downloader.profiles.AbstractBaseProfile;
import com.astrodoorways.filesystem.writers.LinesToFileWriter;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;

public class SDOProfile extends AbstractBaseProfile {

	private String operation = "exp_request";
	private final String query;

	private String cadence;
	private String startDate;
	private String timeRange;
	private String frequency;
	private Integer iterations;
	private String wavelength;

	private final SimpleDateFormat SDO_FORMATTER = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	Logger logger = LoggerFactory.getLogger(SDOProfile.class);

	private final LinesToFileWriter responseFileWriter;

	public SDOProfile(String writeDirectory, String query) {
		super("http:///jsoc.stanford.edu", writeDirectory);
		this.query = query;
		responseFileWriter = new LinesToFileWriter(writeDirectory, "jsonResponseFileWriter.txt", true);
	}

	public SDOProfile(String writeDirectory, String cadence, String startDate, String timeRange, String frequency,
			Integer iterations, String wavelength) {
		super("http://jsoc.stanford.edu", writeDirectory);
		this.query = null;
		this.cadence = cadence;
		this.startDate = startDate;
		this.timeRange = timeRange;
		this.frequency = frequency;
		if (iterations > 0) {
			this.iterations = iterations;
		} else {
			this.iterations = 1;
		}
		this.wavelength = wavelength;
		responseFileWriter = new LinesToFileWriter(writeDirectory, "jsonResponseFileWriter", true);
	}

	@Override
	public void process() {
		HttpRequestFactory factory = new NetHttpTransport().createRequestFactory(new HttpRequestInitializer() {
			@Override
			public void initialize(HttpRequest request) throws IOException {
				request.setParser(new JsonObjectParser(new JacksonFactory()));
			}
		});

		//Inspect query parameters

		//if iterations is set, iterate
		Date date;
		try {
			date = SDO_FORMATTER.parse(startDate);
		} catch (ParseException e) {
			logger.error("problem parsing the start date for the SDO query", e);
			return;
		}

		for (int i = 0; i < iterations; i++) {
			String query = "";
			if (getQuery() != null) {
				query = getQuery();
			} else {
				if (i > 0) {
					date = calculateDate(date);
				}
				query = buildQuery(date);
			}

			logger.debug("query: {}", query);

			// submit query
			String req = getRemoteServer() + "/cgi-bin/ajax/jsoc_fetch?" + getOperation() + "&" + query + "&"
					+ getProcess() + "&" + getRequestor() + "&" + getNotify() + "&" + getMethod() + "&"
					+ getFilenameFormat() + "&" + getProtocol() + "&" + getFormat() + "&_=";
			try {
				HttpRequest request = factory.buildGetRequest(new GenericUrl(req));
				HttpResponse response = request.execute();
				//			String wtf = response.parseAsString();
				//			logger.debug("Response content: {}", wtf);
				SDOWrapper respStr = response.parseAs(SDOWrapper.class);
				for (SDORecord record : respStr.getRecords()) {
					String filename = record.getFilename();
					if (!filename.contains("spikes")) {
						getPathGenerator().writeLine(filename);
						responseFileWriter.writeLine(record.getRecord() + " :: " + filename);
					}
				}
			} catch (IOException e) {
				logger.error("there was an error processing the request", e);
			}
		}
		responseFileWriter.close();
		logger.debug("completed sdo profile iterations");
	}

	private String buildQuery(Date date) {
		//"aia.lev1_euv_12s[2012-11-01T00:11:00Z/30h@1h]");

		//build cadence
		String queryCadence = "";
		switch (cadence) {
		case "aia1":
			queryCadence = "aia.lev1_vis_1h";
			break;
		case "aia12":
			queryCadence = "aia.lev1_euv_12s";
			break;
		case "aia24":
			queryCadence = "aia.lev1_uv_24s";
			break;
		case "hmi45":
			queryCadence = "hmi.Ic_45s";
			break;
		case "hmi720":
			queryCadence = "hmi.Ic_720s";
			break;
		}

		String dateString = SDO_FORMATTER.format(date);
		String rangeString = "";
		if (timeRange != null) {
			rangeString = "/" + timeRange;
		}
		String freqString = "";
		if (frequency != null) {
			freqString = "@" + frequency;
		}
		String wavelengthString = "";
		if (wavelength != null) {
			wavelengthString = "[" + wavelength + "]";
		}
		return "ds=" + queryCadence + "[" + dateString + rangeString + freqString + "]" + wavelengthString;
	}

	private Date calculateDate(Date date) {
		Pattern rangePattern = Pattern.compile("(\\d+)(\\w)*");
		Matcher matcher = rangePattern.matcher(timeRange);
		matcher.find();
		Integer rangeInt = Integer.parseInt(matcher.group(1));
		String rangeType = matcher.group(2).toLowerCase();

		Calendar returnCal = Calendar.getInstance();
		returnCal.setTime(date);

		switch (rangeType) {
		case ("h"):
			returnCal.add(Calendar.HOUR, rangeInt);
			break;
		case ("m"):
			returnCal.add(Calendar.MINUTE, rangeInt);
			break;
		case ("d"):
			returnCal.add(Calendar.DAY_OF_YEAR, rangeInt);
			break;
		case ("s"):
			returnCal.add(Calendar.SECOND, rangeInt);
		}

		return returnCal.getTime();
	}

	private String getFormat() {
		return "format=json";
	}

	private String getFilenameFormat() {
		return "filenamefmt=aia.lev1_euv_12s.%7BT_REC%3AA%7D.%7BWAVELNTH%7D.%7Bsegment%7D";
	}

	private String getMethod() {
		return "method=url_quick";
	}

	private String getNotify() {
		return "notify=solarmail";
	}

	private String getRequestor() {
		return "requestor=";
	}

	private String getProcess() {
		return "process=n%3D0%7Cno_op";
	}

	private String getQuery() {
		if (query != null) {
			return "ds=" + query;
		} else {
			return null;
		}
		//		return "ds=aia.lev1_euv_12s[2011-01-17T23:59:58Z/40d@1d][193,171]";
	}

	public String getOperation() {
		return "op=exp_request";
	}

	public String getProtocol() {
		//		return "protocol=jpeg";
		return "";
	}

	@Override
	public String getRemoteServer() {
		return "http://jsoc.stanford.edu";
	}

	public static class SDOWrapper {
		@Key
		private Integer count;
		@Key
		private Integer rcount;
		@Key
		private Integer size;
		@Key
		private String dir;
		@Key("data")
		private List<SDORecord> records;

		public Integer getCount() {
			return count;
		}

		public Integer getRcount() {
			return rcount;
		}

		public Integer getSize() {
			return size;
		}

		public String getDir() {
			return dir;
		}

		public List<SDORecord> getRecords() {
			return records;
		}
	}

	public static class SDORecord {
		@Key
		private String record;
		@Key
		private String filename;

		public String getRecord() {
			return record;
		}

		public String getFilename() {
			return filename;
		}
	}

}
