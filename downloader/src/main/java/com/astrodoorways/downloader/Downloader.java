package com.astrodoorways.downloader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.astrodoorways.downloader.profiles.Profile;
import com.astrodoorways.filesystem.writers.LinesToFileWriter;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.google.common.base.Strings;
import com.google.common.io.Files;

public class Downloader {

	private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

	private final List<Profile> profiles = new ArrayList<Profile>();

	public static int TIMEOUT_TIME = 15000;

	private String writeDirectory;

	private String remoteUrl;

	public static final String COMPLETED_PATH_LIST = "completedPathList.txt";

	public static final String SYSTEM_PERCENT_UTILIZATION = "system.percent.utilization";
	public static final String MAX_NUM_PROCESSORS = "max.num.processors";

	public Downloader(String writeDirectory, String remoteUrl) {
		if (Strings.isNullOrEmpty(writeDirectory)) {
			logger.error("write directory cannot be null or empty");
			throw new IllegalArgumentException("write directory is null or empty");
		}

		if (!writeDirectory.endsWith("/")) {
			writeDirectory += "/";
		}

		this.writeDirectory = writeDirectory;
		this.remoteUrl = remoteUrl;
	}

	public void processProfiles() {
		for (Profile profile : profiles) {
			logger.debug("about to process {} files will be written to {}", new Object[] { remoteUrl, writeDirectory });
			try {
				profile.process();

				logger.debug("profiler pool has terminated");
				// built profile links, prepare to save
				String profileLinkFile = profile.getLinksFileName();
				logger.debug("Completed gathering links from {}", profile.getRemoteServer());
				int maxLineCount = getLineCount(profileLinkFile);
				BufferedReader reader = new BufferedReader(new FileReader(profileLinkFile));
				String line = "";
				int numProcessors = System.getProperties().containsKey(MAX_NUM_PROCESSORS) ? Integer.parseInt(System
						.getProperty(MAX_NUM_PROCESSORS)) : Runtime.getRuntime().availableProcessors();
				logger.debug("number of processors for file download: {}", numProcessors);
				ExecutorService executorService = Executors.newFixedThreadPool(numProcessors);

				AtomicInteger counter = new AtomicInteger();
				while ((line = reader.readLine()) != null) {
					// check to see if line appears in completed file list
					executorService.execute(new DownloadLinkRunnable(profile.getRemoteServer(), line, writeDirectory,
							maxLineCount, counter));
				}
				reader.close();
				logger.debug("path file reader closed");
				executorService.shutdown();
				while (!executorService.isTerminated()) {

				}
				logger.debug("The application is finished. If this is still running, feel free to kill the process.");
			} catch (FailingHttpStatusCodeException e) {
				logger.error("error processing site", e);
			} catch (MalformedURLException e) {
				logger.error("error processing site", e);
			} catch (IOException e) {
				logger.error("error processing site", e);
			}
		}
	}

	private int getLineCount(String fileName) throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(fileName));
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n' || c[i] == '\r')
						++count;
				}
			}
			return (count == 0 && !empty) ? 1 : count;
		} finally {
			is.close();
		}
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	public String getWriteDirectory() {
		return writeDirectory;
	}

	public String getRemoteUrl() {
		return remoteUrl;
	}

	public static class DownloadLinkRunnable implements Runnable {

		private static final Logger logger = LoggerFactory.getLogger(DownloadLinkRunnable.class);

		private final String remoteUrl;
		private final String filePath;
		private final String localUrl;
		private final int lineCount;
		private final AtomicInteger counter;

		public DownloadLinkRunnable(String remoteUrl, String filePath, String localUrl, int lineCount,
				AtomicInteger counter) {
			if (!remoteUrl.endsWith("/")) {
				remoteUrl += "/";
			}

			if (!localUrl.endsWith("/")) {
				localUrl += "/";
			}

			this.remoteUrl = remoteUrl;
			this.localUrl = localUrl;
			this.filePath = filePath;
			this.lineCount = lineCount;
			this.counter = counter;
		}

		@Override
		public void run() {
			try {
				if (!linkPreviouslyDownloaded(filePath)) {
					logger.trace("about to inspect {} for action", filePath);
					saveLink();
				}
			} catch (FileNotFoundException e) {
				logger.error("problem while trying to save a file", e);
			}
		}

		private boolean linkPreviouslyDownloaded(String link) throws FileNotFoundException {
			File pathCheckList = new File(localUrl + COMPLETED_PATH_LIST);
			if (!pathCheckList.canRead())
				return false;
			Scanner scanner = new Scanner(pathCheckList);
			String scannerFound = scanner.findWithinHorizon(link, 0);
			boolean found = scannerFound != null;
			if (found) {
				counter.incrementAndGet();
				logger.debug("link was previously downloaded {} - {}", new Object[] { link, completionMessage() });
			}
			scanner.close();
			return found;
		}

		/**
		 * Process an individual link using information contained in the profile
		 *
		 * @throws IOException
		 */
		public void saveLink() {
			// check to see if the file exists locally
			String localFile = localUrl + "/" + filePath;

			boolean finished = false;
			while (!finished) {
				try {

					File file = new File(localFile);
					// build remote path
					String finalRemoteUrl = remoteUrl;
					if (filePath.startsWith("/")) {
						URL url = new URL(finalRemoteUrl);
						finalRemoteUrl = url.getProtocol() + "://" + url.getHost();
					}

					String remoteFile = finalRemoteUrl + filePath;
					if (file.canRead() && !file.isDirectory()) {
						Long fileLength = file.length();
						// if it does, check to see if it is complete based on
						// length of file
						logger.trace("file/dir already exists: {}", localFile);
						URLConnection connection = buildConnection(remoteFile);
						Map<String, List<String>> map = connection.getHeaderFields();
						if (map.get("Content-Length") == null) {
							logger.debug("no header fields were returned when attempting to access: {}", remoteFile);
							continue;
						}
						Long remoteLength = Long.parseLong(map.get("Content-Length").iterator().next());

						// if it is not complete, resume downloading
						if (remoteLength > fileLength || fileLength == 0) {
							transferFromRemoteToLocal(remoteFile, localFile, fileLength, remoteLength);
							finished = true;
						} else {
							logger.trace("file is already complete, moving on");
							finished = true;
						}
					} else {
						// if it is does not exist, create the directory or file
						logger.trace("file does not exist: {}", localFile);

						// take care of the directory structure
						Files.createParentDirs(new File(localFile));

						// save the file
						logger.trace("preparing to write remote file {} to local file {}", new Object[] { remoteFile,
								localFile });

						transferFromRemoteToLocal(remoteFile, localFile);
						logger.trace("file created: {}", localFile);
						finished = true;
					}

					// timeout took place. pause, then let the loop execute
					// again
				} catch (FileNotFoundException e) {
					logger.error("exception while processing a link", e);
					finished = true;
				} catch (IOException e) {
					logger.error("exception while processing a link", e);
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
			counter.incrementAndGet();
			logger.debug("successfully downloaded the file {} from {} to {} - {}", new Object[] { filePath, remoteUrl,
					localUrl, completionMessage() });

			LinesToFileWriter lineWriter = new LinesToFileWriter(localUrl, COMPLETED_PATH_LIST, true);
			// write line to completed file list
			lineWriter.writeLine(filePath);
			lineWriter.close();
		}

		private int percentComplete() {
			int counterVal = counter.get();
			return (int) (((double) counterVal / (double) lineCount) * 100.0);
		}

		private String completionMessage() {
			return String.format("downloader is %d%% complete with %d out of %d processed", percentComplete(),
					counter.get(), lineCount);
		}

		/**
		 * @param remoteUrl
		 * @return
		 * @throws MalformedURLException
		 * @throws IOException
		 */
		public URLConnection buildConnection(String remoteUrl) throws MalformedURLException, IOException {
			URLConnection connection = new URL(remoteUrl).openConnection();
			connection.setReadTimeout(TIMEOUT_TIME);
			connection.setConnectTimeout(TIMEOUT_TIME);
			return connection;
		}

		/**
		 * @param remoteUrl
		 * @param localFile
		 * @throws MalformedURLException
		 * @throws IOException
		 */
		public void transferFromRemoteToLocal(String remoteUrl, String localFile) throws MalformedURLException,
				IOException {
			transferFromRemoteToLocal(remoteUrl, localFile, null, null);
		}

		/**
		 * @param remoteUrl
		 * @param localFile
		 * @param fileLength
		 * @param remoteLength
		 * @throws MalformedURLException
		 * @throws IOException
		 */
		public void transferFromRemoteToLocal(String remoteUrl, String localFile, Long fileLength, Long remoteLength)
				throws MalformedURLException, IOException {
			URLConnection connection = buildConnection(remoteUrl);
			if (fileLength != null && remoteLength != null) {
				connection.setRequestProperty("Range", "Bytes=" + fileLength + "-");
			}

			ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
			FileOutputStream fos = new FileOutputStream(localFile, true);
			FileChannel channel = fos.getChannel();
			try {
				if (fileLength != null && remoteLength != null)
					channel.transferFrom(rbc, fileLength, remoteLength);
				else
					channel.transferFrom(rbc, 0, 1 << 24);
				logger.trace("completed download of file {}", localFile);
			} finally {
				channel.close();
				fos.close();
				rbc.close();
			}
		}
	}
}
