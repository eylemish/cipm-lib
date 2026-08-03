package cipm.stats.bootstrapconfidenceintervals;

import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

//  https://github.com/mvanga/pybootstrap/blob/master/pybootstrap/__init__.py

/**
 * * The class for the classical Percentile Bootstrap method to estimate
 * confidence intervals.
 * 
 * It supports both standard independent resampling and stationary block
 * bootstrap for time series.
 * 
 * @author ezgiyircali
 *
 */
public class PercentileBootstrapStrategy implements ConfidenceIntervalStrategy {

	private final Long seed;
	private final StationaryResampler stationaryResampler;

	// Default Constructor (For random use)
	public PercentileBootstrapStrategy() {
		this.seed = null;
		this.stationaryResampler = new StationaryResampler();
	}

	// Test Constructor
	public PercentileBootstrapStrategy(long seed) {
		this.seed = seed;
		this.stationaryResampler = new StationaryResampler(seed);
	}

	/**
	 * Calculates the confidence interval using the percentile bootstrap method.
	 * 
	 * Evaluates the estimator in parallel over the bootstrap samples and crops the
	 * alpha from ends.
	 */
	@Override
	public double[] calculateInterval(double[] data, double alpha, int bootstrapCount, double pOpt,
			ToDoubleFunction<double[]> theta) {

		// Pre-conditions
		if (data == null || data.length < 10) {
			throw new IllegalArgumentException(
					"Pre-condition failed: Data size must be at least 10 for bootstrapping.");
		}
		if (bootstrapCount < 100) {
			throw new IllegalArgumentException("Pre-condition failed: Bootstrap count should be at least 100.");
		}
		if (alpha <= 0.0 || alpha >= 1.0) {
			throw new IllegalArgumentException("Pre-condition failed: Alpha must be between 0 and 1.");
		}

		int n = data.length;
		double[] thetastar = new double[bootstrapCount];

		long baseSeed = (this.seed != null) ? this.seed : System.nanoTime();

		// Bootstrap Loop in parallel
		IntStream.range(0, bootstrapCount).parallel().forEach(b -> {
			long threadSeed = baseSeed + b; // For each thread, a new different deterministic seed
			double[] resample;
			if (pOpt > 0.0) {
				// Time Series Mode: Call Stationary Block Bootstrap
				resample = stationaryResampler.buildPseudoTimeSeries(data, pOpt, threadSeed);
			} else {
				// Classical Independent Mode: Standard random selection with replacement
				java.util.Random localRandom = new java.util.Random(threadSeed);
				resample = new double[n];
				for (int i = 0; i < n; i++) {
					resample[i] = data[localRandom.nextInt(n)];
				}
			}
			// Dynamically evaluate and store the statistical estimator (theta)
			thetastar[b] = theta.applyAsDouble(resample);
		});

		Percentile percentile = new Percentile();
		percentile.setData(thetastar);

		// Dividing alpha in two so that it's fair to both ends.
		double lowerPercentile = (alpha / 2.0) * 100.0;
		double upperPercentile = (1.0 - (alpha / 2.0)) * 100.0;

		// Sort the array of per-sample statistics and cut off ends
		double lowerBound = percentile.evaluate(lowerPercentile);
		double upperBound = percentile.evaluate(upperPercentile);

		return new double[] { lowerBound, upperBound };
	}

}
