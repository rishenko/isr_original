import com.astrodoorways.converter.ApplicationProperties;
import com.astrodoorways.converter.Converter;
import com.astrodoorways.converter.ConverterImpl;
import com.astrodoorways.converter.vicar.cassini.DebiasCalibrator;
import com.astrodoorways.converter.vicar.cassini.Lut8to12BitCalibrator;
import com.astrodoorways.converter.vicar.cassini.TwoHzCalibrator;
import com.astrodoorways.db.filesystem.FileInfo;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class ConverterTest {

	@Autowired
	private Converter converter = null;

	//	@Test(expected = IllegalArgumentException.class)
	public void constructor() throws FileNotFoundException, IOException {
		new ConverterImpl(null, null);
	}

	//	@Test(expected = IllegalArgumentException.class)
	public void constructor2() throws FileNotFoundException, IOException {
		new ConverterImpl("", "");
	}

	//	@Test
	public void sessionInsert() throws Exception {
		Configuration cfg = new Configuration();
		cfg.configure("hibernate.cfg.xml");
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();
		FileInfo dto = new FileInfo();
		dto.setDirectory("directory");
		dto.setExtension("tiff");
		dto.setFileName("fileName");
		session.save(dto);
		session.getTransaction().commit();
	}

	//	@Test
	public void sessionSelect() throws Exception {
		Configuration cfg = new Configuration();
		cfg.configure("hibernate.cfg.xml");
		SessionFactory sessionFactory = cfg.buildSessionFactory();
		Session session = sessionFactory.getCurrentSession();

		session.beginTransaction();
		List records = session.createCriteria(FileInfo.class).list();
		assertTrue(records.size() > 0);
		System.out.println("Record count: " + records.size());
		session.getTransaction().commit();
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
	public void conversionCassiniNeedsCalibrated() throws Exception {
		String readDir = "src/test/resources/test-dirs/read/data/cassini/needsCalib";
		String writeDir = "src/test/resources/test-dirs/write/cassini/needsCalib";
		converter.setReadDirectory(readDir);
		converter.setWriteDirectory(writeDir);
		converter.beginConversion();
	}

	@Test
	public void conversionCassiniCalibrated() throws Exception {
		System.getProperties().setProperty(ApplicationProperties.SEQUENCE, "2012_CASS_TEST");
		String readDir = "src/test/resources/test-dirs/read/data/cassini/calibrated/";
		String writeDir = "src/test/resources/test-dirs/write/cassini/calibrated";
		Converter converter = new ConverterImpl(readDir, writeDir);
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

	//	@Test
	public void convertCassini() throws Exception {
		System.getProperties().setProperty("cassini.calibration.dir",
				"/Users/kevinmcabee/Desktop/astroImagery/coiss_0011_v2");
		String readDir = "/Users/kevinmcabee/Desktop/astroImagery/coiss_2023/data/1526038904_1526111192";
		String writeDir = "/Users/kevinmcabee/Desktop/astroImagery/coissTest";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void conversionLargeSDO() throws Exception {
		String readDir = "/Users/kevinmcabee/Documents/Programming/Eclipse/workspace-juno/Controller/src/test/resources/test-dirs/read/data/fits/large";
		String writeDir = "/Users/kevinmcabee/Documents/Programming/Eclipse/workspace-juno/Controller/src/test/resources/test-dirs/write/data/fits/large";
		Converter converter = new ConverterImpl(readDir, writeDir);
		converter.beginConversion();
	}

	//	@Test
	public void cassiniDustCalibrationTest() throws Exception {
		File imageFile = new File("src/test/resources/test-dirs/calib/dust/N1520433208_1.IMG");
		assertTrue(imageFile.exists());
		ImageReader reader = ImageIO.getImageReaders(ImageIO.createImageInputStream(imageFile)).next();
		reader.setInput(ImageIO.createImageInputStream(imageFile));
		BufferedImage image = reader.read(0);
		IIOMetadata metadata = reader.getImageMetadata(0);

		double[] rasterArray = image.getRaster().getPixels(0, 0, 1024, 1024, new double[1024 * 1024]);

		Lut8to12BitCalibrator lutCalibrator = new Lut8to12BitCalibrator();
		System.out.println("lutCal: " + lutCalibrator.calibrate(rasterArray, metadata));

		//		BitweightCalibrator bwCalibrator = new BitweightCalibrator();
		//		System.out.println("bwCalibrator: " + bwCalibrator.calibrate(rasterArray, metadata));

		DebiasCalibrator debiasCalibrator = new DebiasCalibrator();
		System.out.println("debiasCal: " + debiasCalibrator.calibrate(rasterArray, metadata));

		//		CassiniDustRingCalibrator cassiniCalibrator = new CassiniDustRingCalibrator();
		//		System.out.println("cassCal: " + cassiniCalibrator.calibrate(rasterArray, metadata));

		TwoHzCalibrator twoHzCalibrator = new TwoHzCalibrator();
		System.out.println("twoHzCal: " + twoHzCalibrator.calibrate(rasterArray, metadata));

		int boxSize = (int) Math.sqrt(rasterArray.length);

		BufferedImage imageNew = new BufferedImage(boxSize, boxSize, BufferedImage.TYPE_USHORT_GRAY);

		imageNew.getRaster().setPixels(0, 0, boxSize, boxSize, rasterArray);
		File outputFile = new File("src/test/resources/test-dirs/calib/dust/dust-test1.tiff");
		ImageIO.write(imageNew, "tiff", outputFile);
	}
}
