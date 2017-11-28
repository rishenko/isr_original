package com.astrodoorways.converter.metadata.handler;

import java.util.HashMap;
import java.util.Map;

/**
 * Sequencer maintains a threadsafe, application wide map of counters. 
 * 
 * @author kmcabee
 *
 */
public class Sequencer {
	private static final Map<String, Integer> map = new HashMap<>();

	/**
	 * Pass in an ordered set of strings repreesnting a specific sequence and receive
	 * an incremented count of that sequence.
	 * 
	 * @param args ordered list of strings representing a sequence
	 * @return the incremented value of that sequence
	 */
	public static int incrementAndGet(String... args) {
		StringBuilder argBuilder = new StringBuilder();
		for (String arg : args) {
			argBuilder.append(arg);
		}
		String argFinal = argBuilder.toString();

		synchronized (map) {
			int returnVal = 1;
			if (map.containsKey(argFinal)) {
				returnVal = map.get(argFinal);
				map.put(argFinal, ++returnVal);
			} else {
				map.put(argFinal, returnVal);
			}
			return returnVal;
		}
	}
}
