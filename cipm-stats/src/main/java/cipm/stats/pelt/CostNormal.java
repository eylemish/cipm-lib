package cipm.stats.pelt;

import org.apache.commons.math3.linear.*;

/**
 * Implements the cost function based on the Gaussian (Normal) distribution
 * assumption.
 * 
 * @author ezgiyircali
 *
 */
public class CostNormal implements BaseCost {

	private double[][] signal;
	private int nDims;

	/**
	 * Returns the minimum number of data points required.
	 */
	@Override
	public int getMinSize() {
		return 2;
	}

	/**
	 * Fits the model to the provided multivariate signal data.
	 */
	@Override
	public void fit(double[][] signal) {
		this.signal = signal;
		this.nDims = signal[0].length;

	}

	/**
	 * Calculates the negative log likelihood cost for a segment [start, end).
	 */
	@Override
	public double error(int start, int end) {
		int n = end - start;
		if (n < getMinSize())
			return 0.0; // not enough points

		// Covariance matrix calculation
		RealMatrix covarianceMatrix = calculateMleCovariance(start, end);

		// Regularization
		for (int i = 0; i < nDims; i++) {
			covarianceMatrix.setEntry(i, i, covarianceMatrix.getEntry(i, i) + 1e-6); // Regularization
		}

		LUDecomposition lu = new LUDecomposition(covarianceMatrix); // Determinant
		double det = lu.getDeterminant();

		// When determinant is equals to or smaller than 0.
		if (det <= 0) {
			det = 1e-10;
		}

		return n * Math.log(det);
	}

	/**
	 * Computes the covariance matrix for a specific segment.
	 * 
	 * @param start The starting index.
	 * @param end   The ending index.
	 * @return A RealMatrix as the covariance structure.
	 */
	private RealMatrix calculateMleCovariance(int start, int end) {
		int n = end - start;
		double[] means = new double[nDims];

		// 1. Calculating column means
		for (int i = start; i < end; i++) {
			for (int d = 0; d < nDims; d++) {
				means[d] += signal[i][d];
			}
		}
		for (int d = 0; d < nDims; d++) {
			means[d] /= n;
		}

		// 2. Calculating MLE covariance matrix (divided by n - 1)
		double[][] covData = new double[nDims][nDims];
		for (int i = start; i < end; i++) {
			for (int d1 = 0; d1 < nDims; d1++) {
				for (int d2 = 0; d2 < nDims; d2++) {
					covData[d1][d2] += (signal[i][d1] - means[d1]) * (signal[i][d2] - means[d2]);
				}
			}
		}

		for (int d1 = 0; d1 < nDims; d1++) {
			for (int d2 = 0; d2 < nDims; d2++) {
				covData[d1][d2] /= n;
			}
		}

		return new Array2DRowRealMatrix(covData);
	}

}
