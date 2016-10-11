

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
import junit.framework.JUnit4TestAdapter;

import nom.tam.fits.*;
import nom.tam.util.*;
import java.io.*;

/**
 *
 * @author tmcglynn
 */
public class ChecksumTest {

    @Test public void testChecksum() throws Exception {

        int[][] data = new int[][] {{1,2}, {3,4}, {5,6}};
        Fits f = new Fits();
        BasicHDU bhdu = FitsFactory.HDUFactory(data);
        f.addHDU(bhdu);

        Fits.setChecksum(bhdu);
        ByteArrayOutputStream    bs   = new ByteArrayOutputStream();
        BufferedDataOutputStream bdos = new BufferedDataOutputStream(bs);
        f.write(bdos);
        bdos.close();
        byte[] stream = bs.toByteArray();
        long chk = Fits.checksum(stream);
        int val = (int)chk;

        assertEquals("CheckSum test", -1, val);
    }

}
