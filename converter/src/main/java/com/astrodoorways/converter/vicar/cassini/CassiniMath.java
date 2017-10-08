package com.astrodoorways.converter.vicar.cassini;

import Jama.Matrix;

public class CassiniMath {

	public static double[] linfit(double[] xArray, double[] yArray) {

		int nx = xArray.length;
		int ny = yArray.length;

		if (nx < 2) {
			throw new IllegalArgumentException("x must have at least 2 elements");
		}
		if (nx != ny) {
			throw new IllegalArgumentException("x and y must have an equal number of elements");
		}

		double sdev = 1.0;
		double nsdev = 0;

		Matrix x = new Matrix(xArray, 1);
		Matrix y = new Matrix(yArray, 1);

		int ss = nx;
		double sx = total(x);
		double sy = total(y);
		Matrix t = subtract(x, sx / sy);
		double b = total(t.times(y));

		double st2 = total(t.times(t));
		b = b / st2;
		double a = (sy - sx * b) / ss;
		double sdeva = Math.sqrt((1.0 + sx * sx / (ss * st2)) / ss);
		double sdevb = Math.sqrt(1.0 / st2);

		Matrix yfit = add(x.times(b), a);
		return yfit.getColumnPackedCopy();
	}

	public static Matrix subtract(Matrix m, double subValue) {
		double[] newArray = new double[m.getColumnDimension()];
		int i = 0;
		for (double val : m.getRowPackedCopy()) {
			newArray[i++] = val - subValue;
		}
		return new Matrix(newArray, 1);
	}

	public static Matrix add(Matrix m, double addValue) {
		double[] newArray = new double[m.getColumnDimension()];
		int i = 0;
		for (double val : m.getRowPackedCopy()) {
			newArray[i++] = val - addValue;
		}
		return new Matrix(newArray, 1);
	}

	public static double total(Matrix m) {
		double total = 0.0;
		for (double val : m.getRowPackedCopy()) {
			total += val;
		}
		return total;
	}
}
