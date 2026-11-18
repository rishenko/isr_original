package com.astrodoorways.converter.golden;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Collects "key=value" observations about what the converter did, so that the output of one
 * build (the 2018 baseline) can be compared line by line with another (the modernized build).
 *
 * Deliberately written in Java 8 syntax: the same file is compiled against the original code to
 * generate the golden files.
 */
public class GoldenRecorder {

	private final TreeMap<String, String> values = new TreeMap<>();
	private final List<String[]> pathReplacements = new ArrayList<>();

	/** Replace machine-specific absolute paths in recorded values with a stable placeholder. */
	public GoldenRecorder normalizePath(String absolutePath, String placeholder) {
		pathReplacements.add(new String[] { absolutePath, placeholder });
		return this;
	}

	public void put(String key, Object value) {
		String str = String.valueOf(value);
		for (String[] replacement : pathReplacements) {
			str = str.replace(replacement[0], replacement[1]);
		}
		// keep each observation on one line
		str = str.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
		values.put(key, str);
	}

	public void putException(String key, Throwable t) {
		put(key + ".exception", t.getClass().getName());
		// ClassCastException messages name JVM- and library-specific classes; the type is what matters
		if (!(t instanceof ClassCastException)) {
			put(key + ".message", t.getMessage());
		}
	}

	/**
	 * Some handlers fall back to the current time when an image has no usable date. Record such
	 * values as <NOW> so a golden generated on one day still matches on another.
	 */
	public void putTime(String key, String value, String... nowPrefixes) {
		if (value != null) {
			for (String prefix : nowPrefixes) {
				if (value.startsWith(prefix)) {
					put(key, "<NOW>");
					return;
				}
			}
		}
		put(key, value);
	}

	public void putEpochMillis(String key, Long millis) {
		if (millis != null && Math.abs(System.currentTimeMillis() - millis) < 2L * 24 * 60 * 60 * 1000) {
			put(key, "<NOW>");
			return;
		}
		put(key, millis);
	}

	/** Date prefixes for "today" in the formats the converter uses (UTC). */
	public static String[] todayPrefixes() {
		java.util.Date now = new java.util.Date();
		java.util.TimeZone utc = java.util.TimeZone.getTimeZone("UTC");
		String[] patterns = { "yyyy-DDD'T'", "yyyy:MM:dd ", "yyyy-MM-dd'T'", "yyyyMMdd" };
		List<String> prefixes = new ArrayList<>();
		for (String pattern : patterns) {
			java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern);
			format.setTimeZone(utc);
			prefixes.add(format.format(now));
			// tolerate a run that crosses midnight
			prefixes.add(format.format(new java.util.Date(now.getTime() - 24L * 60 * 60 * 1000)));
		}
		return prefixes.toArray(new String[0]);
	}

	public void putValueMap(String prefix, Map<String, String> map) {
		if (map == null) {
			put(prefix, "null");
			return;
		}
		String[] today = todayPrefixes();
		for (Map.Entry<String, String> entry : new TreeMap<>(map).entrySet()) {
			putTime(prefix + "." + entry.getKey(), entry.getValue(), today);
		}
	}

	/** Record dimensions, data layout, a hash of every sample, and simple statistics. */
	public void putImage(String prefix, BufferedImage image) {
		Raster raster = image.getRaster();
		put(prefix + ".width", image.getWidth());
		put(prefix + ".height", image.getHeight());
		put(prefix + ".bands", raster.getNumBands());
		put(prefix + ".dataType", raster.getSampleModel().getDataType());
		double[] pixels = raster.getPixels(0, 0, image.getWidth(), image.getHeight(), (double[]) null);
		putPixels(prefix, pixels);
	}

	public void putPixels(String prefix, double[] pixels) {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		double sum = 0;
		for (double p : pixels) {
			min = Math.min(min, p);
			max = Math.max(max, p);
			sum += p;
		}
		put(prefix + ".count", pixels.length);
		put(prefix + ".min", min);
		put(prefix + ".max", max);
		put(prefix + ".sum", sum);
		put(prefix + ".sha256", sha256(pixels));
	}

	public static String sha256(double[] pixels) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			ByteBuffer buffer = ByteBuffer.allocate(8);
			for (double p : pixels) {
				buffer.clear();
				buffer.putLong(Double.doubleToLongBits(p));
				digest.update(buffer.array());
			}
			return toHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public static String sha256(String text) {
		try {
			return toHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder hex = new StringBuilder();
		for (byte b : bytes) {
			hex.append(String.format("%02x", b));
		}
		return hex.toString();
	}

	/**
	 * Sequencer keeps a JVM-wide static counter per target/filter, so sequence numbers depend on
	 * whatever ran earlier in the same JVM. Reset it so each golden case starts from 1.
	 */
	public static void resetSequencer() {
		try {
			java.lang.reflect.Field field = com.astrodoorways.converter.metadata.handler.Sequencer.class
					.getDeclaredField("map");
			field.setAccessible(true);
			Map<?, ?> map = (Map<?, ?>) field.get(null);
			synchronized (map) {
				map.clear();
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	public TreeMap<String, String> getValues() {
		return values;
	}

	public void write(File file) throws IOException {
		file.getParentFile().mkdirs();
		List<String> lines = new ArrayList<>();
		for (Map.Entry<String, String> entry : values.entrySet()) {
			lines.add(entry.getKey() + "=" + entry.getValue());
		}
		Files.write(file.toPath(), lines, StandardCharsets.UTF_8);
	}

	public static TreeMap<String, String> read(File file) throws IOException {
		TreeMap<String, String> result = new TreeMap<>();
		for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
			int split = line.indexOf('=');
			if (split > 0) {
				result.put(line.substring(0, split), line.substring(split + 1));
			}
		}
		return result;
	}
}
