package com.astrodoorways;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LogTest {

	@Test
	public void logTest() {
		// 0.001953125
		List<Double> results = new ArrayList<Double>();
		results.add(256 / (Math.log(4095) / Math.log(2)));
		// 2/256 = loge(4096)/loge(2)
		for (Double result : results) {
			System.out.println(result);
		}
	}
}
