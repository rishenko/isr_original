package com.astrodoorways.downloader.sdo;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.BinaryTableHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;

import org.junit.Test;

import com.astrodoorways.downloader.sdo.proofofconcept.FitsDecomp;

public class FitsDecompTet {

	@Test
	public void test() throws IOException, FitsException {
		File file = new File("src/test/resources/com/astrodoorways/downloader/sdo/image_lev1.fits");
		Fits fits = new Fits(file);
		BasicHDU[] hdus = fits.read();

		BinaryTableHDU imageHDU = (BinaryTableHDU) hdus[1];
		short[] data = (short[]) imageHDU.getKernel();

		InputStream stream = new FileInputStream(file);
		FitsDecomp decomp = new FitsDecomp();
		short[] arrayOfValues = new short[4096 * 4096];
		decomp.fits_rdecomp_short(stream, file.length(), arrayOfValues, 4096 * 4096, 32);

		BufferedImage image = new BufferedImage(4096, 4096, BufferedImage.TYPE_USHORT_GRAY);
		WritableRaster raster = image.getRaster();
		for (int x = 0; x < 4096; x++) {
			for (int y = 0; y < 4096; y++) {
				raster.setPixel(x, y, new int[] { (int) arrayOfValues[x + y] });
			}
		}

		ImageIO.write(image, "tiff",
				new File("src/test/resources/com/astrodoorways/downloader/sdo/image_unpacked.tiff"));
	}

}
