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

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import junit.framework.JUnit4TestAdapter;

import nom.tam.image.*;
import nom.tam.util.*;
import nom.tam.fits.*;

import java.io.File;

/** Test adding a little junk after a valid image.
 *  We wish to test three scenarios:
 *     Junk at the beginning (should continue to fail)
 *     Short (<80 byte) junk after valid HDU
 *     Long  (>80 byte) junk after valid HDU
 *  The last two should succeed after FitsFactory.setAllowTerminalJunk(true).
 */
public class JunkTest {

    @Test
    public void test() throws Exception {

        Fits f = new Fits();

        byte[] bimg = new byte[40];
        for (int i = 10; i < bimg.length; i += 1) {
            bimg[i] = (byte)i;
        }

        // Make HDUs of various types.
        f.addHDU(Fits.makeHDU(bimg));


        // Write a FITS file.

        // Valid FITS with one HDU
        BufferedFile bf = new BufferedFile("j1.fits", "rw");
        f.write(bf);
        bf.flush();
        bf.close();
        
        // Invalid junk with no valid FITS.
        bf = new BufferedFile("j2.fits", "rw");
        bf.write(new byte[10]);
        bf.close();

        // Valid FITS followed by short junk.
        bf = new BufferedFile("j3.fits", "rw");
        f.write(bf);
        bf.write("JUNKJUNK".getBytes());
        bf.close();

        // Valid FITS followed by long junk.
        bf = new BufferedFile("j4.fits", "rw");
        f.write(bf);
        for (int i=0; i<100; i += 1) {
            bf.write("A random string".getBytes());
        }
        bf.close();

        int pos = 0;
        try {
            f = new Fits("j1.fits");
            f.read();
        } catch (Exception e) {
            pos = 1;
        }
        assertTrue("Junk Test: Valid File OK,Dft",        readSuccess("j1.fits"));
        assertTrue("Junk Test: Invalid File Fails, Dft", !readSuccess("j2.fits"));
        assertTrue("Junk Test: Short junk fails, Dft",   !readSuccess("j3.fits"));
        assertTrue("Junk Test: Long junk fails, Dft",    !readSuccess("j4.fits"));

        FitsFactory.setAllowTerminalJunk(true);

        assertTrue("Junk Test: Valid File OK,with junk",        readSuccess("j1.fits"));
        assertTrue("Junk Test: Invalid File Fails, with junk", !readSuccess("j2.fits"));
        assertTrue("Junk Test: Short junk OK, with junk",       readSuccess("j3.fits"));
        assertTrue("Junk Test: Long junk OK, with junk",        readSuccess("j4.fits"));

        FitsFactory.setAllowTerminalJunk(false);

        assertTrue("Junk Test: Valid File OK,No junk",        readSuccess("j1.fits"));
        assertTrue("Junk Test: Invalid File Fails, No junk", !readSuccess("j2.fits"));
        assertTrue("Junk Test: Short junk fails, No junk",   !readSuccess("j3.fits"));
        assertTrue("Junk Test: Long junk fails, No junk",    !readSuccess("j4.fits"));
    }

    boolean readSuccess(String file) {
        try {
            Fits f = new Fits(file);
            f.read();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
