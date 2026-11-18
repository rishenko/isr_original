package com.astrodoorways.converter.unit;

import com.astrodoorways.converter.converters.VicarImageConverter;
import com.astrodoorways.converter.db.filesystem.FileInfo;
import com.astrodoorways.converter.db.imagery.Metadata;
import com.astrodoorways.converter.metadata.processor.MetadataProcessor;
import com.astrodoorways.converter.mission.MissionPlugin;
import com.astrodoorways.converter.mission.Missions;
import com.astrodoorways.converter.mission.cassini.CassiniMission;
import com.astrodoorways.converter.mission.rosetta.RosettaMission;
import com.astrodoorways.converter.mission.sdo.SdoMission;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/** Each sample file is routed to the right mission plugin. */
public class MissionSelectionTest {

	private static final File READ = new File("src/test/resources/test-dirs/read");

	private static MissionPlugin missionFor(String path) throws IOException {
		File file = new File(READ, path);
		FileInfo fileInfo = new FileInfo();
		fileInfo.setDirectory(file.getParentFile().getCanonicalPath());
		fileInfo.setFileName(file.getName());
		Metadata metadata = new Metadata();
		metadata.setFileInfo(fileInfo);
		VicarImageConverter converter = new VicarImageConverter(metadata, null, "unused", "TIFF");
		converter.readImage(file);
		MetadataProcessor processor = new MetadataProcessor();
		processor.process(converter.getIIOMetaData());
		return processor.getMission();
	}

	@Test
	public void cassini() throws IOException {
		assertEquals(CassiniMission.class, missionFor("data/cassini/needsCalib/N1711575444_1.IMG").getClass());
		assertEquals(CassiniMission.class, missionFor("data/cassini/calibrated/N1701606691_1_CALIB.IMG").getClass());
		// 1996 ground-test image
		assertEquals(CassiniMission.class, missionFor("data/1526038904_1526111192/121811.img").getClass());
	}

	@Test
	public void sdo() throws IOException {
		assertEquals(SdoMission.class, missionFor("data/fits/image_lev1.fits").getClass());
	}

	@Test
	public void rosetta() throws IOException {
		assertEquals(RosettaMission.class, missionFor("data/rosetta-nac/N20140320T204855543ID20F22.IMG").getClass());
	}

	@Test
	public void unrecognised() throws IOException {
		assertSame(Missions.UNKNOWN, missionFor("data/fits/rosat_pspc_rdf2_3_im1.fits"));
		assertSame(Missions.UNKNOWN, missionFor("data/rosetta/ROS_CAM1_20141024T113435F.FIT"));
	}

	@Test
	public void unknownMissionDoesNotChangePixels() throws IOException {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_USHORT_GRAY);
		assertSame(image, Missions.UNKNOWN.process(image, null, java.util.Collections.emptyMap()));
	}
}
