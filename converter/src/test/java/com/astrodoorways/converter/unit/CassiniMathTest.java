package com.astrodoorways.converter.unit;

import Jama.Matrix;
import com.astrodoorways.converter.vicar.cassini.CassiniMath;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** CassiniMath helpers used by the (currently unwired) 2 Hz calibrator. */
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

	/** Current behaviour (bug): add() subtracts. */
	@Test
	public void addCurrentlySubtracts() {
		Matrix result = CassiniMath.add(new Matrix(new double[] { 5, 6, 7 }, 1), 2);
		assertArrayEquals(new double[] { 3, 4, 5 }, result.getRowPackedCopy(), 0);
	}

	/**
	 * Current behaviour (bug): linfit multiplies two 1xN matrices, which Jama rejects, so it never
	 * returns a fit for valid input.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void linfitCurrentlyAlwaysThrows() {
		CassiniMath.linfit(new double[] { 1, 2, 3 }, new double[] { 2, 4, 6 });
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
