package cipm.stats.bootstrapconfidenceintervals;

import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

/**
 * * The class for the the Bias-Corrected and Accelerated (BCa) Bootstrap method
 * to estimate confidence intervals.
 * 
 * It supports both standard independent resampling and stationary block
 * bootstrap for time series.
 * 
 * @author ezgiyircali
 *
 */
public class BcaBootstrapStrategy implements ConfidenceIntervalStrategy {

	private final Long seed;
	private final NormalDistribution normalDistribution = new NormalDistribution(0, 1);
	private final StationaryResampler stationaryResampler;

	// Default Constructor (For random use)
	public BcaBootstrapStrategy() {
		this.seed = null;
		this.stationaryResampler = new StationaryResampler();
	}

	// Test Constructor
	public BcaBootstrapStrategy(long seed) {
		this.seed = seed;
		this.stationaryResampler = new StationaryResampler(seed);
	}

	/**
	 * Calculates the confidence interval using the BCa bootstrap method.
	 * 
	 */
	@Override
	public double[] calculateInterval(double[] data, double alpha, int bootstrapCount, double pOpt, ToDoubleFunction<double[]> theta) {

		
		//Pre-condiitons
		if (data == null || data.length < 10) {
            throw new IllegalArgumentException("Pre-condition failed: Data size must be at least 10 for bootstrapping.");
        }
        if (bootstrapCount < 100) {
            throw new IllegalArgumentException("Pre-condition failed: Bootstrap count should be at least 100.");
        }
        if (alpha <= 0.0 || alpha >= 1.0) {
            throw new IllegalArgumentException("Pre-condition failed: Alpha must be between 0 and 1.");
        }
        
		int n = data.length;
		double thetahat = theta.applyAsDouble(data); // thetahat is estimated statistical value of original data
		double[] thetastar = new double[bootstrapCount]; // determined by theta function
		
		long baseSeed = (this.seed != null) ? this.seed : System.nanoTime();

		// Bootstrap Loop in parallel
		IntStream.range(0, bootstrapCount).parallel().forEach(b -> {
			long threadSeed = baseSeed + b; // For each thread new different deterministic seed 
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
			thetastar[b] = theta.applyAsDouble(resample);
		});

		// // Calculating z0 which measures how far to the right or left of original
		// value
		double countLessThanOriginal = 0;

		for (double tStar : thetastar) {
			if (tStar < thetahat) {
				countLessThanOriginal++;
			}
		}

		double p = countLessThanOriginal / bootstrapCount;
		p = Math.max(0.0001, Math.min(0.9999, p)); // Avoiding edge values (0 or 1) to prevent infinite Z-score values
		double z0 = normalDistribution.inverseCumulativeProbability(p);

		// Jackknife Loop (Resampling through removing the elements one by one)
		double[] u = new double[n];
		double uSum = 0;
		for (int i = 0; i < n; i++) {
			// Creating a new helper array excluding the i-th element
			double[] subArray = new double[n - 1];
			int index = 0;
			for (int j = 0; j < n; j++) {
				if (i != j) {
					subArray[index++] = data[j];
				}
			}
			// Calculating and storing the theta results of sub arrays
			u[i] = theta.applyAsDouble(subArray);
			uSum += u[i];
		}

		double uMean = uSum / n;

		// Acceleration Calculation
		double numerator = 0;
		double denominator = 0;
		for (int i = 0; i < n; i++) {
			double deviation = uMean - u[i];
			numerator += Math.pow(deviation, 3);
			denominator += Math.pow(deviation, 2);
		}

		double acc = 0;
		if (denominator > 0) {
			acc = numerator / (6.0 * Math.pow(denominator, 1.5));
		}

		// Determining the cutting points

		// Quantile Normal (Z-scores for alpha boundaries)
		double zAlphaLowerBound = normalDistribution.inverseCumulativeProbability(alpha / 2.0);
		double zAlphaUpperBound = normalDistribution.inverseCumulativeProbability(1.0 - (alpha / 2.0));

		// Probabilistic Normal (BCa adjusted cumulative probabilities)
		double adjustedPercentileLower = normalDistribution
				.cumulativeProbability(z0 + (z0 + zAlphaLowerBound) / (1.0 - acc * (z0 + zAlphaLowerBound)));
		double adjustedPercentileUpper = normalDistribution
				.cumulativeProbability(z0 + (z0 + zAlphaUpperBound) / (1.0 - acc * (z0 + zAlphaUpperBound)));

		// Converting probability bounds to 0-100 scale percentiles
		double lowerPercentile = Math.max(0.001, Math.min(0.999, adjustedPercentileLower)) * 100.0;
		double upperPercentile = Math.max(0.001, Math.min(0.999, adjustedPercentileUpper)) * 100.0;

		Percentile percentile = new Percentile();
		percentile.setData(thetastar);

		// Final confidence interval values
		return new double[] { percentile.evaluate(lowerPercentile), percentile.evaluate(upperPercentile) };
	}

}
