package cipm.stats.bootstrapconfidenceintervals;

import java.util.Random;

//https://github.com/YoshihikoNishikawa/StationaryBootstrap/blob/main/Python/stationary_bootstrap.py

/**
 * Resampler class for Stationary Bootstrap method. It builds pseudo time series
 * by extracting data blocks with random lengths following a geometric
 * distribution.
 * 
 * @author ezgiyircali
 *
 */
public class StationaryResampler {

	private final Random random;

	// Default Constructor (For production use)
	public StationaryResampler() {
		this.random = new Random();
	}

	// Test Constructor (For reproducible and deterministic testing).

	public StationaryResampler(long seed) {
		this.random = new Random(seed); // Initializes the shared Random instance with a fixed seed.
	}

	/**
	 * Standard sequential resampling for single threaded runs and unit tests.
	 * Produces different pseudo-series without seed-loopingissues.
	 * 
	 * @param originalData the original data
	 * @param pBiasing     the probability parameter determining the average block
	 *                     length
	 * @return newly generated pseudo time series
	 */
	public double[] buildPseudoTimeSeries(double[] originalData, double pBiasing) {
		return buildPseudoTimeSeries(originalData, pBiasing, this.random);
	}

	/**
	 * Multi-threaded / Parallel resampling for parallel processing pipelines (like
	 * inside parallel streams in BCa) Local Random instance scoped only to the
	 * given thread seed for thread safety.
	 * 
	 * @param originalData the original data.
	 * @param pBiasing     the probability parameter determining the average block
	 *                     length
	 * @param threadSeed   deterministic seed unique to the calling thread/iteration
	 *                     step
	 * @return newly generated pseudo time series
	 */
	public double[] buildPseudoTimeSeries(double[] originalData, double pBiasing, long threadSeed) {
		return buildPseudoTimeSeries(originalData, pBiasing, new Random(threadSeed));
	}

	/**
	 * Performs the actual stationary bootstrap algorithm. Both public methods uses
	 * their execution here, passing their respective Random instance.
	 * 
	 * @param originalData the original data
	 * @param pBiasing     the probability parameter determining the average block
	 *                     length
	 * @param rng          specific Random instance (shared or thread-local) for
	 *                     resampling
	 * @return a newly generated pseudo time series
	 */
	private double[] buildPseudoTimeSeries(double[] originalData, double pBiasing, Random rng) {
		int length = originalData.length;
		double[] pseudoTimeSeries = new double[length];
		int currentIndex = 0;

		while (currentIndex < length) {
			int startIndex = rng.nextInt(length);
			int blockLength = generateGeometricRandomVariable(pBiasing, rng);

			for (int i = 0; i < blockLength && currentIndex < length; i++) {
				pseudoTimeSeries[currentIndex] = originalData[(startIndex + i) % length];
				currentIndex++;
			}
		}
		return pseudoTimeSeries;
	}

	/**
	 * Generates a random variable from a geometric distribution using inverse
	 * transform sampling.
	 * 
	 * @param p   success probability (pBiasing) which regulates block size.
	 * @param rng Random instance to drive the calculation
	 * @return the block length (minimum of 1)
	 */
	public int generateGeometricRandomVariable(double p, Random rng) {
		if (p <= 0.0 || p >= 1.0) {
			return 1;
		}

		double u = rng.nextDouble();
		// Avoiding potential log(0) undefined behavior by capping the lower bound of u
		if (u == 0.0) {
			u = 0.000001;
		}

		return Math.max(1, (int) Math.ceil(Math.log(1.0 - u) / Math.log(1.0 - p)));
	}
}