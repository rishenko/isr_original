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

import nom.tam.fits.FitsDate;

/** Test the FITS date class.
 *  This class is derived from the internal testing utilities
 *  in FitsDate written by David Glowacki.
 */
public class DateTester {

    @Test
    public void test() {

        assertEquals("t1", true, testArg("20/09/79"));
        assertEquals("t1", true, testArg("1997-07-25"));
        assertEquals("t1", true, testArg("1987-06-05T04:03:02.01"));
        assertEquals("t1", true, testArg("1998-03-10T16:58:34"));
        assertEquals("t1", true, testArg(null));
        assertEquals("t1", true, testArg("        "));

        assertEquals("t1", false, testArg("20/09/"));
        assertEquals("t1", false, testArg("/09/79"));
        assertEquals("t1", false, testArg("09//79"));
        assertEquals("t1", false, testArg("20/09/79/"));

        assertEquals("t1", false, testArg("1997-07"));
        assertEquals("t1", false, testArg("-07-25"));
        assertEquals("t1", false, testArg("1997--07-25"));
        assertEquals("t1", false, testArg("1997-07-25-"));

        assertEquals("t1", false, testArg("5-Aug-1992"));
        assertEquals("t1", false, testArg("28/02/91 16:32:00"));
        assertEquals("t1", false, testArg("18-Feb-1993"));
        assertEquals("t1", false, testArg("nn/nn/nn"));
    }

    boolean testArg(String arg) {
        try {
            FitsDate fd = new FitsDate(arg);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
