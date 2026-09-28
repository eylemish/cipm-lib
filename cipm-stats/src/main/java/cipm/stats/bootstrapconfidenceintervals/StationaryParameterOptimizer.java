package cipm.stats.bootstrapconfidenceintervals;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Estimates the optimal p-parameter (p_opt) for the Stationary Bootstrap.
 * 
 * @author ezgiyircali
 *
 */
public class StationaryParameterOptimizer {

	/**
	 * Calculates the optimal success probability (p_opt) for the geometric
	 * distribution.
	 * 
	 * @param trajectory The input time series
	 * @return The optimal probability between 0.001 and 1.0
	 */
	public double calculateOptimalP(double[] trajectory) {
		int n = trajectory.length;

		// Calculating normalized autocorrelation
		double[] autocorrelation = calculateAutocorrelation(trajectory);

		// Finding the automated implied bandwidth (M)
		int bandwidth = findBandwidth(autocorrelation);

		// Computing G (implied bias component) using the flat top window function
		double gSum = 0;
		for (int lag = 0; lag < bandwidth; lag++) {
			gSum += windowFunction(lag, bandwidth) * lag * autocorrelation[lag];
		}
		double gValue = 2.0 * gSum;

		// Computing D (implied variance component)
		double dSum = 0;
		for (int lag = 1; lag < bandwidth; lag++) {
			dSum += windowFunction(lag, bandwidth) * autocorrelation[lag];
		}
		double dValue = 2.0 * Math.pow(autocorrelation[0] + 2.0 * dSum, 2.0);

		if (dValue == 0) { // Avoiding division by zero if data has zero variance
			return 1.0;
		}

		// Final Politis-White formula
		double pOpt = Math.pow((2.0 * Math.pow(gValue, 2.0) / dValue) * n, -1.0 / 3.0);

		// Probability boundary checks
		if (Double.isNaN(pOpt) || pOpt > 1.0) { // Must be between 0.0 and 1.0
			return 1.0;
		}

		return Math.max(0.001, pOpt); // Avoiding absolute 0 to prevent infinite block lengths
	}

	/**
	 * Calculates the normalized autocorrelation function for given time series.
	 * 
	 * @param trajectory The continuous data array.
	 * @return An array containing autocorrelation values for positive lags.
	 */
	private double[] calculateAutocorrelation(double[] trajectory) {
		int n = trajectory.length;
		double mean = StatUtils.mean(trajectory);
		double[] rho = new double[n];

		// Centering the data (subtracting mean)
		double[] centered = new double[n];
		for (int i = 0; i < n; i++) {
			centered[i] = trajectory[i] - mean;
		}

		// Calculating the denominator (sum of squared deviations - variance base)
		double denominator = 0.0;
		for (int i = 0; i < n; i++) {
			denominator += Math.pow(centered[i], 2);
		}

		// Avoiding division by zero
		if (denominator == 0.0) {
			rho[0] = 1.0;
			return rho;
		}

		// Compute positive lags and normalize them directly (lag =k)
		for (int lag = 0; lag < n; lag++) {
			double numerator = 0;
			for (int i = 0; i < n - lag; i++) {
				numerator += centered[i] * centered[i + lag];
			}

			rho[lag] = numerator / denominator;
		}
		return rho;
	}

	/**
	 * Finds the automated bandwidth parameter based on where the correlation drops
	 * below the statistical noise floor.
	 * 
	 * @param autocorrelation
	 * @return
	 */
	private int findBandwidth(double[] autocorrelation) {
		int n = autocorrelation.length;
		double c = 2.0;

		// Boundary condition threshold: c * sqrt(log10(N) / N)
		double threshold = c * Math.sqrt(Math.log10(n) / n);
		int kBoundary = Math.max(5, (int) Math.sqrt(Math.log10(n)));


		int bandwidth = n;

		// Scan the correlation chain to see where dependencies drop below the noise
		// floor
		for (int j = 0; j < n; j++) {
			double normalizedRho = Math.abs(autocorrelation[j]);
			if (normalizedRho < threshold) {
				// Check if it stays under the threshold for the next K lags
				boolean isNoise = true;
				for (int i = 0; i < kBoundary; i++) {
					if (j + i < n && Math.abs(autocorrelation[j + i]) >= threshold) {
						isNoise = false;
						break;
					}
				}
				if (isNoise) {
					bandwidth = j;
					break;
				}
			}
		}

		// 2 * bandwidth
		return Math.max(2, 2 * bandwidth);
	}

	/**
	 * Implements the flat-top window function to smooth autocorrelation estimates.
	 * 
	 * @param lag
	 * @param bandwidth
	 * @return
	 */
	private double windowFunction(double lag, double bandwidth) {
		if (lag <= 0.5 * bandwidth) {
			return 1.0;
		} else if (lag < bandwidth) {
			return 2.0 * (1.0 - lag / bandwidth);
		} else {
			return 0.0;
		}
	}

}
