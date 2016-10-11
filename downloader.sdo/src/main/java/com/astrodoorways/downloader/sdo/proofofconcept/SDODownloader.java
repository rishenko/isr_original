package com.astrodoorways.downloader.sdo.proofofconcept;

import java.io.IOException;
import java.util.List;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.Key;

public class SDODownloader {

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

	public static void main(String... args) {
		HttpRequestFactory factory = new NetHttpTransport().createRequestFactory(new HttpRequestInitializer() {

			@Override
			public void initialize(HttpRequest request) throws IOException {
				request.setParser(new JsonObjectParser(new JacksonFactory()));
			}
		});
		String req = "http://jsoc.stanford.edu/cgi-bin/ajax/jsoc_fetch?op=exp_request&ds=aia.lev1_euv_12s%5B2010.07.25_13%3A30%2F30m%5D%5B%3F%20WAVELNTH%20%3D%20193%20%3F%5D&process=n%3D0%7Cno_op&requestor=&notify=solarmail&method=url_quick&filenamefmt=aia.lev1_euv_12s.%7BT_REC%3AA%7D.%7BWAVELNTH%7D.%7Bsegment%7D&format=json&_=";
		try {
			HttpRequest request = factory.buildGetRequest(new GenericUrl(req));
			HttpResponse response = request.execute();
			SDOWrapper respStr = response.parseAs(SDOWrapper.class);
			System.out.println(respStr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
