package nom.tam.fits;
/*
 * This code is part of the Java FITS library developed 1996-2012 by T.A. McGlynn (NASA/GSFC)
 * The code is available in the public domain and may be copied, modified and used
 * by anyone in any fashion for any purpose without restriction. 
 * 
 * No warranty regarding correctness or performance of this code is given or implied.
 * Users may contact the author if they have questions or concerns.
 * 
 * The author would like to thank many who have contributed suggestions, 
 * enhancements and bug fixes including:
 * David Glowacki, R.J. Mathar, Laurent Michel, Guillaume Belanger,
 * Laurent Bourges, Rose Early, Jorgo Baker, A. Kovacs, V. Forchi, J.C. Segovia,
 * Booth Hartley and Jason Weiss.  
 * I apologize to any contributors whose names may have been inadvertently omitted.
 * 
 *      Tom McGlynn
 */

import nom.tam.util.*;
import nom.tam.fits.*;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import junit.framework.JUnit4TestAdapter;

/** Test random groups formats in FITS data.
 *    Write and read random groups data
 */
public class RandomGroupsTest {

    @Test
    public void test() throws Exception {

        float[][] fa = new float[20][20];
        float[] pa = new float[3];

        BufferedFile bf = new BufferedFile("rg1.fits", "rw");

        Object[][] data = new Object[1][2];
        data[0][0] = pa;
        data[0][1] = fa;

        // First lets write out the file painfully group by group.
        BasicHDU hdu = Fits.makeHDU(data);
        Header hdr = hdu.getHeader();
        // Change the number of groups
        hdr.addValue("GCOUNT", 20, "Number of groups");
        hdr.write(bf);

        for (int i = 0; i < 20; i += 1) {

            for (int j = 0; j < pa.length; j += 1) {
                pa[j] = i + j;
            }
            for (int j = 0; j < fa.length; j += 1) {
                fa[j][j] = i * j;
            }
            // Write a group
            bf.writeArray(data);
        }

        byte[] padding = new byte[FitsUtil.padding(20 * ArrayFuncs.computeLSize(data))];
        bf.write(padding);

        bf.flush();
        bf.close();

        // Read back the data.
        Fits f = new Fits("rg1.fits");
        BasicHDU[] hdus = f.read();

        data = (Object[][]) hdus[0].getKernel();

        for (int i = 0; i < data.length; i += 1) {

            pa = (float[]) data[i][0];
            fa = (float[][]) data[i][1];
            for (int j = 0; j < pa.length; j += 1) {
                assertEquals("paramTest:" + i + " " + j, (float) (i + j), pa[j], 0);
            }
            for (int j = 0; j < fa.length; j += 1) {
                assertEquals("dataTest:" + i + " " + j, (float) (i * j), fa[j][j], 0);
            }
        }

        // Now do it in one fell swoop -- but we have to have
        // all the data in place first.
        f = new Fits();

        // Generate a FITS HDU from the kernel.
        f.addHDU(Fits.makeHDU(data));
        bf = new BufferedFile("rg2.fits", "rw");
        f.write(bf);

        bf.flush();
        bf.close();

        f = new Fits("rg2.fits");
        data = (Object[][]) f.read()[0].getKernel();
        for (int i = 0; i < data.length; i += 1) {

            pa = (float[]) data[i][0];
            fa = (float[][]) data[i][1];
            for (int j = 0; j < pa.length; j += 1) {
                assertEquals("paramTest:" + i + " " + j, (float) (i + j), pa[j], 0);
            }
            for (int j = 0; j < fa.length; j += 1) {
                assertEquals("dataTest:" + i + " " + j, (float) (i * j), fa[j][j], 0);
            }
        }
    }
}
