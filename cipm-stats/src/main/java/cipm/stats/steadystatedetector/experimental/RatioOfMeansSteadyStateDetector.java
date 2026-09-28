package cipm.stats.steadystatedetector.experimental;

import cipm.stats.bootstrapconfidenceintervals.BcaBootstrapStrategy;
import cipm.stats.bootstrapconfidenceintervals.ConfidenceIntervalStrategy;
import cipm.stats.pelt.PeltResult;
import cipm.stats.steadystatedetector.SteadyStateResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the steady-state detection evaluation using Kalibera and Jones (2013) 
 * Section 7.3 Ratio of Means via Hierarchical Bootstrap Simulation.
 * 
 * @author ezgiyircali
 */
public class RatioOfMeansSteadyStateDetector {


	private final RatioOfMeansCalculator ratioOfMeansCalculator;
	private final double alpha;
	private final int bootstrapCount;

	/**
	 * Constructs a new RatioOfMeansSteadyStateDetector instance with default 
	 * parameters based on LucaTraini (2022) and Kalibera & Jones (2013).
	 */
	public RatioOfMeansSteadyStateDetector() {
		ConfidenceIntervalStrategy ciStrategy = new BcaBootstrapStrategy();
		this.alpha = 0.05; // 95% Confidence Interval
		this.bootstrapCount = 1000;
		this.ratioOfMeansCalculator = new RatioOfMeansCalculator(ciStrategy, alpha, bootstrapCount);
	}

	/**
	 * Constructs a new RatioOfMeansSteadyStateDetector instance with custom parameters.
	 * 
	 * @param ratioOfMeansCalculator The calculator for ratio of means CI
	 * @param alpha The alpha value of confidence intervals
	 * @param bootstrapCount The number of bootstrap resamples to perform
	 */
	public RatioOfMeansSteadyStateDetector(RatioOfMeansCalculator ratioOfMeansCalculator, double alpha, int bootstrapCount) {
		this.ratioOfMeansCalculator = ratioOfMeansCalculator;
		this.alpha = alpha;
		this.bootstrapCount = bootstrapCount;
	}

	/**
	 * Detects steady state using Kalibera and Jones (2013) Ratio of Means approach over PELT segments.
	 * 
	 * @param cleanedData Cleaned data list from a single fork (outliers removed as NaN or filtered)
	 * @param peltResult Result containing change points from PELT
	 * @return SteadyStateResult containing classification and stable data range
	 */
	public SteadyStateResult evaluate(List<Double> cleanedData, PeltResult peltResult) {
		if (cleanedData == null || cleanedData.isEmpty() || peltResult == null) {
			throw new IllegalArgumentException("Data and PeltResult cannot be null or empty.");
		}

		List<Integer> changePoints = peltResult.getChangePoints();

		// If no change points are detected, checking if the whole series is steady or not
		if (changePoints == null || changePoints.isEmpty()) {
			return new SteadyStateResult(false, -1, new ArrayList<>());
		}

		// Reconstructing segments from change points, filtering out Double.NaN if any
		List<List<Double>> segments = new ArrayList<>();
		int startIndex = 0;
		for (int cp : changePoints) {
			if (cp > startIndex && cp <= cleanedData.size()) {
				List<Double> segData = new ArrayList<>();
				for (int i = startIndex; i < cp; i++) {
					Double val = cleanedData.get(i);
					if (val != null && !Double.isNaN(val)) {
						segData.add(val);
					}
				}
				if (!segData.isEmpty()) {
					segments.add(segData);
				}
			}
			startIndex = cp;
		}
		
		// Adding the final segment (sf)
		if (startIndex < cleanedData.size()) {
			List<Double> finalSeg = new ArrayList<>();
			for (int i = startIndex; i < cleanedData.size(); i++) {
				Double val = cleanedData.get(i);
				if (val != null && !Double.isNaN(val)) {
					finalSeg.add(val);
				}
			}
			if (!finalSeg.isEmpty()) {
				segments.add(finalSeg);
			}
		}

		if (segments.isEmpty()) {
			return new SteadyStateResult(false, -1, new ArrayList<>());
		}

		// Final segment (sf) is the reference baseline
		List<Double> finalSegment = segments.get(segments.size() - 1);
		int steadyStartIndex = changePoints.get(changePoints.size() - 1); // Default to last segment start
		boolean steadyReached = true;

		// Traversing backwards from second-to-last segment to check Kalibera & Jones Ratio of Means rule
		for (int i = segments.size() - 2; i >= 0; i--) {
			List<Double> currentSeg = segments.get(i);

			// Preparing hierarchical structure expected by RatioOfMeansCalculator: List<List<Double>>
			List<List<Double>> oldHierarchical = new ArrayList<>();
			oldHierarchical.add(finalSegment);

			List<List<Double>> currentHierarchical = new ArrayList<>();
			currentHierarchical.add(currentSeg);

			// Calculating confidence interval of the ratio of means (current / final)
			double[] ratioCI = ratioOfMeansCalculator.calculateRatioOfMeans(oldHierarchical, currentHierarchical);

			double cdLow = ratioCI[0];
			double cdUp = ratioCI[1];

			//CI must lie within [0.95, 1.05] and include 1.0
			boolean eqToLast = cdLow >= 0.95 && cdUp <= 1.05 && cdLow <= 1.0 && cdUp >= 1.0;

			if (eqToLast) {
				// Expanding steady state start backwards to this equivalent segment
				steadyStartIndex = changePoints.get(i);
			} else {
				// Ratio confidence interval outside the [0.95, 1.05] tolerance range
				break;
			}
		}

		// Extracting steady state measurements (Mstable) from cleanedData starting from steadyStartIndex
		List<Double> mStable = new ArrayList<>();
		for (int i = steadyStartIndex; i < cleanedData.size(); i++) {
			Double val = cleanedData.get(i);
			if (val != null && !Double.isNaN(val)) {
				mStable.add(val);
			}
		}

		return new SteadyStateResult(steadyReached, steadyStartIndex, mStable);
	}
}