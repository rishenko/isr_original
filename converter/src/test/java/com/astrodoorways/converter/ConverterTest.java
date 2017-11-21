package com.astrodoorways.converter;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(classes = RunConverter.class)
public class ConverterTest {

	@Autowired
	private Converter converter;

	@BeforeClass
	public static void setup() {
		ApplicationProperties.setProperty(ApplicationProperties.MAX_NUM_PROCESSORS, Integer.toString(Runtime.getRuntime().availableProcessors()));
	}

	//	@Test
	public void conversionMercury() throws Exception {
		String readDir = "/Users/kmcabee/Desktop/messenger";
		String writeDir = "/Users/kmcabee/Desktop/mercOutput";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionCassini() throws Exception {
		String readDir = "/Users/kmcabee/Desktop/DioneRheaAnim";
		String writeDir = "/Users/kmcabee/Desktop/drao";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionFitsLarge() throws Exception {
		String readDir = "src/test/resources/test-dirs/read/data/fits/large";
		String writeDir = "src/test/resources/test-dirs/write";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionHuygensTiff() throws Exception {
		String readDir = "src/test/resources/test-dirs/read/huygens";
		String writeDir = "src/test/resources/test-dirs/write";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionMarsTiff() throws Exception {
		String readDir = "/Users/kmcabee/Desktop/mars";
		String writeDir = "/Users/kmcabee/Desktop/mars2";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionChandraTiff() throws Exception {
		String readDir = "/Users/kmcabee/Desktop/TarantulaNebula";
		String writeDir = "/Users/kmcabee/Desktop/tarantulaNebulaOutput";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversion32BitTiff() throws Exception {
		String readDir = "src/test/resources/test-dirs/read/data/32BitTiff";
		String writeDir = "src/test/resources/test-dirs/write/32BitTiff";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionCassiniBrokenCalibrated() throws Exception {
		String readDir = "src/test/resources/test-dirs/read/data/cassini_broken";
		String writeDir = "src/test/resources/test-dirs/write/cassini_broken";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	@Test
	public void conversionCassiniBatch() throws Exception {
		String readDir = "/Users/kevinmcabee/sp/coiss_2098";
		String writeDir = "/Users/kevinmcabee/sp/img_out";
		converter.setReadDirectory(readDir);
		converter.setWriteDirectory(writeDir);
		converter.beginConversion();
	}

	@Test
	public void conversionCassiniNeedsCalibrated() throws Exception {
		String readDir = "./src/test/resources/test-dirs/read/data/cassini/needsCalib";
		String writeDir = "./src/test/resources/test-dirs/write/data/cassini/needsCalib";
		converter.setReadDirectory(readDir);
		converter.setWriteDirectory(writeDir);
		converter.beginConversion();
	}

	@Test
	public void conversionCassiniCalibrated() throws Exception {
		System.getProperties().setProperty(ApplicationProperties.SEQUENCE, "2012_CASS_TEST");
		String readDir = "src/test/resources/test-dirs/read/data/cassini/calibrated/";
		String writeDir = "src/test/resources/test-dirs/write/cassini/calibrated";
		converter.setReadDirectory(readDir);
		converter.setWriteDirectory(writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionFitsNewHorizonsLarge() throws Exception {
		String readDir = "src/test/resources/test-dirs/read/data/fits/large";
		String writeDir = "src/test/resources/test-dirs/write/nh";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionLargeScale() throws Exception {
		String readDir = "/Users/kmcabee/Downloads/coiss_0011_v2/calib/slope/";
		String writeDir = "/Users/kmcabee/Desktop/newOutput/slope";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//@Test
	public void convertSDO() throws Exception {
		/*System.getProperties().setProperty("cassini.calibration.dir",
				"/Users/kevinmcabee/Desktop/astroImagery/coiss_0011_v2");*/
		String readDir = "src/test/resources/test-dirs/read/data/lro";
		String writeDir = "src/test/resources/test-dirs/write/data/lro";

		//		System.getProperties().setProperty("target.process.list",
		//				"/Users/kevinmcabee/Desktop/astroImagery/coiss_0011_v2");
		//		
		//		System.getProperties().setProperty("filter.process.list",
		//				"/Users/kevinmcabee/Desktop/astroImagery/coiss_0011_v2");
		//		System.getProperties().setProperty(ApplicationProperties.NORMALIZE, "true");

		converter.setReadDirectory(readDir);
		converter.setWriteDirectory(writeDir);

		converter.beginConversion();
	}
}
