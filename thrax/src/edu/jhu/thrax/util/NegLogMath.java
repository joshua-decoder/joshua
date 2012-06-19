package edu.jhu.thrax.util;

public class NegLogMath {

	// Number of entries in the table.
	private static final int LOG_ADD_TABLE_SIZE = 640000;
	// Smallest value for nlog_a - nlog_b.
	private static final double LOG_ADD_MIN = -64;
	private static final double AS_GOOD_AS_ZERO = 1e-10; 
	private static final double logAddInc = -LOG_ADD_MIN / LOG_ADD_TABLE_SIZE;
	private static final double invLogAddInc = LOG_ADD_TABLE_SIZE / -LOG_ADD_MIN;
	private static final double[] logAddTable = new double[LOG_ADD_TABLE_SIZE + 1];

	static {
		for (int i = 0; i <= LOG_ADD_TABLE_SIZE; i++) {
			logAddTable[i] = -Math.log1p(Math.exp((i * logAddInc) + LOG_ADD_MIN));
		}
	}

	public static double logAdd(double nlog_a, double nlog_b) {
		if (nlog_b < nlog_a) {
			double temp = nlog_a;
			nlog_a = nlog_b;
			nlog_b = temp;
		}
		double neg_diff = (nlog_a - nlog_b) - LOG_ADD_MIN;
		if (neg_diff < AS_GOOD_AS_ZERO) {
			return nlog_a;
		}
		return nlog_a + logAddTable[(int) (neg_diff * invLogAddInc)];
	}
}