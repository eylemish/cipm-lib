package cipm.stats.bootstrapconfidenceintervals;

import java.util.Random;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Synthetic time series generator for ARMA processes.
 * @author ezgiyircali
 */
public class ArmaProcessGenerator {

	private final Random random;

	/**
	 * Default constructor using non-deterministic seeding.
	 */
	public ArmaProcessGenerator() {
		this.random = new Random();
	}

	/**
	 * Deterministic constructor for repeatable tests.
	 * 
	 * @param seed Fixed random seed
	 */
	public ArmaProcessGenerator(long seed) {
		this.random = new Random(seed);
	}

	/**
	 * Generates a simple AR(1) process: X_t = c + phi * X_{t-1} + e_t
	 * 
	 * @param n     Length of the generated time series
	 * @param c     Const / Mean offset term
	 * @param phi   AR(1) parameter (Must be phi < 1.0 for stationarity)
	 * @param sigma Standard deviation of the white noise
	 * @return Simulated AR(1) time series
	 */
	public double[] generateAR1(int n, double c, double phi, double sigma) {
		return generateARMA(n, c, new double[] { phi }, new double[] {}, sigma);
	}

	/**
	 * Generates an ARMA(p, q) process with burn-in period .
	 * 
	 * @param n     Target length of the output time series
	 * @param c     Constant term
	 * @param phi   AR coefficients array (length p)
	 * @param theta MA coefficients array (length q)
	 * @param sigma Standard deviation of white noise
	 * @return Simulated ARMA(p, q) time series
	 */
	public double[] generateARMA(int n, double c, double[] phi, double[] theta, double sigma) {
		if (n <= 0) {
			throw new IllegalArgumentException("Length n must be positive.");
		}
		if (sigma < 0) {
			throw new IllegalArgumentException("Noise standard deviation sigma cannot be negative.");
		}

		int p = (phi != null) ? phi.length : 0;
		int q = (theta != null) ? theta.length : 0;

		// Large length (Starter steps will be discarded later)
		int burnIn = Math.max(50, 10 * Math.max(p, q));
		int totalLength = n + burnIn;

		double[] x = new double[totalLength];
		double[] epsilon = new double[totalLength];

		// Apache Commons Math distribution initialized with seed
		NormalDistribution noiseDist = new NormalDistribution(0, sigma);
		noiseDist.reseedRandomGenerator(random.nextLong());

		for (int t = 0; t < totalLength; t++) {
			epsilon[t] = noiseDist.sample();

			double arTerm = 0.0;
			for (int i = 0; i < p; i++) {
				if (t - i - 1 >= 0) {
					arTerm += phi[i] * x[t - i - 1];
				}
			}

			double maTerm = 0.0;
			for (int j = 0; j < q; j++) {
				if (t - j - 1 >= 0) {
					maTerm += theta[j] * epsilon[t - j - 1];
				}
			}

			x[t] = c + arTerm + epsilon[t] + maTerm;
		}

		// Extract final 'n' elements after burn-in
		double[] result = new double[n];
		System.arraycopy(x, burnIn, result, 0, n);

		return result;
	}

	/**
	 * Generate a time series with a synthetic break (Change-Point).
	 * Useful for testing PELT/CROPS on auto-correlated data.
	 * 
	 * @param n          Total length
	 * @param breakPoint Index where the regime shift occurs
	 * @param phi1       AR(1) parameter before the break
	 * @param phi2       AR(1) parameter after the break
	 * @param sigma      Noise standard deviation
	 * @return Time series with a change-point in dependence
	 */
	public double[] generateAR1WithBreak(int n, int breakPoint, double phi1, double phi2, double sigma) {
		if (breakPoint <= 0 || breakPoint >= n) {
			throw new IllegalArgumentException("Break point must be inside the range (0, n).");
		}

		double[] seg1 = generateAR1(breakPoint, 0.0, phi1, sigma);
		double[] seg2 = generateAR1(n - breakPoint, 0.0, phi2, sigma);

		double[] combined = new double[n];
		System.arraycopy(seg1, 0, combined, 0, breakPoint);
		System.arraycopy(seg2, 0, combined, breakPoint, n - breakPoint);

		return combined;
	}
}