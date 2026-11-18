package com.astrodoorways.converter;

import com.astrodoorways.converter.db.filesystem.FileInfo;
import com.astrodoorways.converter.db.imagery.Metadata;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SequenceNumbersTest {

	private static Metadata image(String file, String target, String filterOne, String filterTwo, Long time) {
		FileInfo fileInfo = new FileInfo();
		fileInfo.setDirectory("/read");
		fileInfo.setFileName(file);
		Metadata metadata = new Metadata();
		metadata.setFileInfo(fileInfo);
		metadata.setTarget(target);
		metadata.setFilterOne(filterOne);
		metadata.setFilterTwo(filterTwo);
		metadata.setTime(time == null ? null : new Date(time));
		return metadata;
	}

	@Test
	public void numbersFollowTimeWithinEachTargetAndFilter() {
		Metadata late = image("c.IMG", "TITAN", "CL1", "CL2", 3000L);
		Metadata early = image("a.IMG", "TITAN", "CL1", "CL2", 1000L);
		Metadata middle = image("b.IMG", "TITAN", "CL1", "CL2", 2000L);
		Metadata otherFilter = image("d.IMG", "TITAN", "CL1", "GRN", 5000L);
		Metadata otherTarget = image("e.IMG", "RHEA", "CL1", "CL2", 4000L);
		Metadata noTime = image("f.IMG", "TITAN", "CL1", "CL2", null);
		Metadata tieB = image("h.IMG", "MIMAS", "CL1", null, 7000L);
		Metadata tieA = image("g.IMG", "MIMAS", "CL1", null, 7000L);

		List<Metadata> input = Arrays.asList(late, otherFilter, noTime, early, tieB, otherTarget, middle, tieA);
		Map<Metadata, Integer> numbers = ConverterImpl.assignSequenceNumbers(input);

		assertEquals(Integer.valueOf(1), numbers.get(early));
		assertEquals(Integer.valueOf(2), numbers.get(middle));
		assertEquals(Integer.valueOf(3), numbers.get(late));
		assertEquals(Integer.valueOf(4), numbers.get(noTime));
		assertEquals(Integer.valueOf(1), numbers.get(otherFilter));
		assertEquals(Integer.valueOf(1), numbers.get(otherTarget));
		assertEquals(Integer.valueOf(1), numbers.get(tieA));
		assertEquals(Integer.valueOf(2), numbers.get(tieB));
		assertEquals(8, numbers.size());
	}
}
