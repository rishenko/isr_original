package nom.tam.util;
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

import nom.tam.util.BufferedFile;
import nom.tam.util.BufferedDataInputStream;
import java.io.FileInputStream;

public class BigFileTest {

    @Test
    public void test() throws Exception {
        try {
            // First create a 3 GB file.
            String fname = System.getenv("BIGFILETEST");
            if (fname == null) {
                System.out.println("BIGFILETEST environment not set.  Returning without test");
                return;
            }
            System.out.println("Big file test.  Takes quite a while.");
            byte[] buf = new byte[100000000]; // 100 MB
            BufferedFile bf = new BufferedFile(fname, "rw");
            byte sample = 13;

            for (int i = 0; i < 30; i += 1) {
                bf.write(buf);  // 30 x 100 MB = 3 GB.
                if (i == 24) {
                    bf.write(new byte[]{sample});
                } // Add a marker.
            }
            bf.close();

            // Now try to skip within the file.
            bf = new BufferedFile(fname, "r");
            long skip = 2500000000L; // 2.5 G

            long val1 = bf.skipBytes(skip);
            long val2 = bf.getFilePointer();
            int val = bf.read();
            bf.close();

            assertEquals("SkipResult", skip, val1);
            assertEquals("SkipPos", skip, val2);
            assertEquals("SkipVal", (int) sample, val);

            BufferedDataInputStream bdis = new BufferedDataInputStream(
                    new FileInputStream(fname));
            val1 = bdis.skipBytes(skip);
            val = bdis.read();
            bdis.close();
            assertEquals("SSkipResult", skip, val1);
            assertEquals("SSkipVal", (int) sample, val);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            throw e;
        }
    }
}
