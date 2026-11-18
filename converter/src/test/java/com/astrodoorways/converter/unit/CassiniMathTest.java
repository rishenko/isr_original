package com.astrodoorways.converter.unit;

import Jama.Matrix;
import com.astrodoorways.converter.mission.cassini.CassiniMath;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** CassiniMath helpers used by the (currently unwired) 2 Hz calibrator. Expected fits checked by hand. */
public class CassiniMathTest {

	@Test
	public void total() {
		assertEquals(10.0, CassiniMath.total(new Matrix(new double[] { 1, 2, 3, 4 }, 1)), 0);
	}

	@Test
	public void subtract() {
		Matrix result = CassiniMath.subtract(new Matrix(new double[] { 5, 6, 7 }, 1), 2);
		assertArrayEquals(new double[] { 3, 4, 5 }, result.getRowPackedCopy(), 0);
	}

	/** Used to subtract. */
	@Test
	public void add() {
		Matrix result = CassiniMath.add(new Matrix(new double[] { 5, 6, 7 }, 1), 2);
		assertArrayEquals(new double[] { 7, 8, 9 }, result.getRowPackedCopy(), 0);
	}

	/** Used to throw for every valid input (a matrix product of two 1xN rows). */
	@Test
	public void linfitExactLine() {
		double[] fit = CassiniMath.linfit(new double[] { 1, 2, 3, 4 }, new double[] { 5, 7, 9, 11 });
		assertArrayEquals(new double[] { 5, 7, 9, 11 }, fit, 1e-12);
	}

	/** Least-squares fit of noisy points: for x = 0..3 and y = 1, 1, 3, 3 the fit is y = 0.8 + 0.8 * x. */
	@Test
	public void linfitNoisyPoints() {
		double[] fit = CassiniMath.linfit(new double[] { 0, 1, 2, 3 }, new double[] { 1, 1, 3, 3 });
		assertArrayEquals(new double[] { 0.8, 1.6, 2.4, 3.2 }, fit, 1e-12);
	}

	/** The x mean must use the point count; it used total(y), which only worked when total(y) = n. */
	@Test
	public void linfitOffsetData() {
		double[] fit = CassiniMath.linfit(new double[] { 10, 20, 30 }, new double[] { 100, 300, 200 });
		// slope 5, intercept 100: fitted 150, 200, 250
		assertArrayEquals(new double[] { 150, 200, 250 }, fit, 1e-9);
	}

	@Test(expected = IllegalArgumentException.class)
	public void linfitNeedsTwoPoints() {
		CassiniMath.linfit(new double[] { 1 }, new double[] { 1 });
	}

	@Test(expected = IllegalArgumentException.class)
	public void linfitNeedsEqualLengths() {
		CassiniMath.linfit(new double[] { 1, 2 }, new double[] { 1, 2, 3 });
	}
}
