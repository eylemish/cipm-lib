package cipm.stats.steadystatedetector;

import cipm.stats.bootstrapconfidenceintervals.BcaBootstrapStrategy;
import cipm.stats.bootstrapconfidenceintervals.ConfidenceIntervalStrategy;
import cipm.stats.pelt.PeltResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the fifth step of the steady-state detection. Evaluates PELT
 * segments using Kalibera and Jones (2013) %5 tolerance rule via BCa Bootstrap
 * Confidence Intervals to determine the steady-state starting time (st).
 * 
 * @author ezgiyircali
 */
public class SteadyStateDetector {

	private final ConfidenceIntervalStrategy ciStrategy;
	private final double alpha;
	private final int bootstrapCount;

	/**
	 * Constructs a new SteadyStateDetector instance with parameters based on
	 * LucaTraini 2022).
	 */
	public SteadyStateDetector() {
		this.ciStrategy = new BcaBootstrapStrategy();
		this.alpha = 0.05; // 95% Confidence Interval
		this.bootstrapCount = 1000;
	}

	/**
	 * Constructs a new SteadyStateDetector instance with custom parameters.
	 * 
	 * @param ciStrategy     The strategy to use for calculating bootstrap
	 *                       confidence intervals
	 * @param alpha          The alpha value of confidence intervals
	 * @param bootstrapCount The number of bootstrap resamples to perform
	 */
	public SteadyStateDetector(ConfidenceIntervalStrategy ciStrategy, double alpha, int bootstrapCount) {
		this.ciStrategy = ciStrategy;
		this.alpha = alpha;
		this.bootstrapCount = bootstrapCount;
	}

	/**
	 * Detects steady state using Kalibera and Jones (2013) approach over PELT
	 * segments.
	 * 
	 * @originalData Full raw or cleaned data list from a single fork
	 * @param peltResult Result containing change points from PELT
	 * @return SteadyStateResult containing classification and stable data range
	 */
	public SteadyStateResult evaluate(List<Double> originalData, PeltResult peltResult) {
		if (originalData == null || originalData.isEmpty() || peltResult == null) {
			throw new IllegalArgumentException("Data and PeltResult cannot be null or empty.");
		}

		List<Integer> changePoints = peltResult.getChangePoints();

		// If no change points are detected, checking if the whole series is steady or
		// not
		if (changePoints == null || changePoints.isEmpty()) {
			return new SteadyStateResult(false, -1, new ArrayList<>());
		}

		// Reconstructing segments from change points
		List<double[]> segments = new ArrayList<>();
		int startIndex = 0;
		for (int cp : changePoints) {
			if (cp > startIndex && cp <= originalData.size()) {
				double[] segData = originalData.subList(startIndex, cp).stream().mapToDouble(Double::doubleValue)
						.toArray();
				segments.add(segData);
			}
			startIndex = cp;
		}
		// Adding the final segment (sf)
		if (startIndex < originalData.size()) {
			double[] finalSeg = originalData.subList(startIndex, originalData.size()).stream()
					.mapToDouble(Double::doubleValue).toArray();
			segments.add(finalSeg);
		}

		if (segments.isEmpty()) {
			return new SteadyStateResult(false, -1, new ArrayList<>());
		}

		// Final segment (sf)
		double[] finalSegment = segments.get(segments.size() - 1);

		// Calculating Confidence Interval for the final segment mean using BCa
		// Bootstrap
		double[] finalSegmentCI = ciStrategy.calculateInterval(finalSegment, alpha, bootstrapCount, 0.0,
				data -> java.util.Arrays.stream(data).average().orElse(0.0));

		int steadyStartIndex = changePoints.get(changePoints.size() - 1); // Default to last segment start
		boolean steadyReached = true;

		// Traversing backwards from second-to-last segment to check Kalibera & Jones 5%
		// tolerance rule
		for (int i = segments.size() - 2; i >= 0; i--) {
			double[] currentSeg = segments.get(i);
			double currentMean = java.util.Arrays.stream(currentSeg).average().orElse(0.0);

			// Checking if current segment mean falls within the 5% CI of the final segment
			double lowerBoundTolerance = finalSegmentCI[0] * 0.95;
			double upperBoundTolerance = finalSegmentCI[1] * 1.05;

			if (currentMean >= lowerBoundTolerance && currentMean <= upperBoundTolerance) {
				// Expanding steady state start backwards to this equivalent segment
				steadyStartIndex = changePoints.get(i);
			} else {
				// Mean variation exceeding 5% tolerance
				break;
			}
		}

		// Extracting steady state measurements (Mstable)
		List<Double> mStable = new ArrayList<>();
		for (int i = steadyStartIndex; i < originalData.size(); i++) {
			mStable.add(originalData.get(i));
		}

		return new SteadyStateResult(steadyReached, steadyStartIndex, mStable);
	}
}