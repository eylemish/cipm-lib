package cipm.stats.steadystatedetector;

import cipm.stats.bootstrapconfidenceintervals.ConfidenceIntervalStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The class for the Ratio of Means confidence interval calculation with
 * hierarchical resampling based on Kalibera and Jones (Section 7.3)
 * 
 * @author ezgiyircali
 */
public class RatioOfMeansCalculator {

	private final ConfidenceIntervalStrategy ciStrategy;
	private final double alpha;
	private final int bootstrapCount;
	private final Random random;

	/**
	 * Constructs a new RatioOfMeansCalculator instance with default parameters
	 * @param ciStrategy The strategy to use for calculating confidence intervals
	 * @param alpha The alpha value for confidence intervals
	 * @param bootstrapCount The number of bootstrapping
	 */
	public RatioOfMeansCalculator(ConfidenceIntervalStrategy ciStrategy, double alpha, int bootstrapCount) {
		this.ciStrategy = ciStrategy;
		this.alpha = alpha;
		this.bootstrapCount = bootstrapCount;
		this.random = new Random();
	}
	
	/**
	 * Constructs a new RatioOfMeansCalculator instance with a custom seed for testing.
	 * @param ciStrategy  The strategy to use for calculating confidence intervals
	 * @param alpha The alpha value for confidence intervals
	 * @param bootstrapCount  The number of bootstrapping
	 * @param seed the seed for the random number generator to ensure determinism when testing
	 */
	public RatioOfMeansCalculator(ConfidenceIntervalStrategy ciStrategy, double alpha, int bootstrapCount, long seed) {
		this.ciStrategy = ciStrategy;
		this.alpha = alpha;
		this.bootstrapCount = bootstrapCount;
		this.random = new Random(seed);
	}

	/**
	 * Calculates the confidence interval for the ratio of means between two
	 * hierarchical datasets.
	 * 
	 * @param oldDataHierarchical Hierarchical nested structure for old system
	 * @param newDataHierarchical Hierarchical nested structure for new system
	 * @return An array containing [lowerBound, upperBound] for the ratio of means
	 */
	public double[] calculateRatioOfMeans(List<List<Double>> oldDataHierarchical,
			List<List<Double>> newDataHierarchical) {

		if (oldDataHierarchical == null || oldDataHierarchical.isEmpty() || newDataHierarchical == null
				|| newDataHierarchical.isEmpty()) {
			throw new IllegalArgumentException("Hierarchical datasets cannot be null or empty.");
		}

		double[] simulatedRatios = new double[bootstrapCount];

		// Simulating bootstrap mean for both systems
		for (int i = 0; i < bootstrapCount; i++) {
			double oldMeanSample = simulateMean(oldDataHierarchical);
			double newMeanSample = simulateMean(newDataHierarchical);

			// Computing the simulated ratio
			if (oldMeanSample == 0.0) {
				simulatedRatios[i] = 0.0;
			} else {
				simulatedRatios[i] = newMeanSample / oldMeanSample;
			}
		}

		// Confidence interval over all simulated ratios
		return ciStrategy.calculateInterval(simulatedRatios, alpha, bootstrapCount, 0.0,
				data -> java.util.Arrays.stream(data).average().orElse(0.0));
	}

	/**
	 * The pseudocode function simulateMean(oldnew) based on Kalibera and Jones,
	 * Section 7.3
	 */
	private double simulateMean(List<List<Double>> hierarchicalData) {
		List<Double> simulatedMeasurements = new ArrayList<>();

		// Recursively resampling the hierarchical data structure to generate sample
		// means
		resampleHierarchical(hierarchicalData, 0, new ArrayList<>(), simulatedMeasurements);

		return simulatedMeasurements.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
	}

	private void resampleHierarchical(List<List<Double>> levels, int currentLevel, List<Double> currentPath,
			List<Double> simulatedMeans) {
		if (currentLevel == levels.size()) {

			// In the bottom level, computing the mean of the path and adding it to the
			// sampled means
			double sampleMean = currentPath.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
			simulatedMeans.add(sampleMean);
			return;
		}

		List<Double> levelData = levels.get(currentLevel);
		int n = levelData.size();

		// replacement = yes (resampling across the current level)
		for (int i = 0; i < n; i++) {
			int randomIndex = random.nextInt(n);
			List<Double> nextPath = new ArrayList<>(currentPath);
			nextPath.add(levelData.get(randomIndex));
			resampleHierarchical(levels, currentLevel + 1, nextPath, simulatedMeans);
		}
	}
}