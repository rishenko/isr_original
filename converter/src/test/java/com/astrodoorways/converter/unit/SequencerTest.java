package com.astrodoorways.converter.unit;

import com.astrodoorways.converter.metadata.handler.Sequencer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;

/** Sequencer keeps one global counter per joined key. Keys here are unique per test. */
public class SequencerTest {

	@Test
	public void countsFromOnePerKey() {
		assertEquals(1, Sequencer.incrementAndGet("seqTestA", "CL1"));
		assertEquals(2, Sequencer.incrementAndGet("seqTestA", "CL1"));
		assertEquals(1, Sequencer.incrementAndGet("seqTestA", "CL2"));
		assertEquals(3, Sequencer.incrementAndGet("seqTestA", "CL1"));
	}

	@Test
	public void keysAreJoinedWithoutASeparator() {
		assertEquals(1, Sequencer.incrementAndGet("seqTestB", "AB", "C"));
		assertEquals("'seqTestB','A','BC' is the same key", 2, Sequencer.incrementAndGet("seqTestB", "A", "BC"));
	}

	@Test
	public void isThreadSafe() throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(8);
		List<Future<Integer>> futures = new ArrayList<>();
		for (int i = 0; i < 1000; i++) {
			futures.add(pool.submit(() -> Sequencer.incrementAndGet("seqTestC")));
		}
		List<Integer> values = new ArrayList<>();
		for (Future<Integer> future : futures) {
			values.add(future.get());
		}
		pool.shutdown();
		Collections.sort(values);
		for (int i = 0; i < 1000; i++) {
			assertEquals(i + 1, values.get(i).intValue());
		}
	}
}
