package cipm.stats.steadystatedetector;

import cipm.stats.pelt.Crops;
import cipm.stats.pelt.Pelt;
import cipm.stats.pelt.PeltResult;

import java.util.List;

/**
 * Implements second, third and fourth step of of the steady-state detection
 * process: Step 2 (Penalty Sensitivity):CROPS algorithm in the range [4, 10^5].
 * Step 3 (Penalty Selection): Kneedle algorithm to select the elbow point. Step
 * 4 (Change Point Detection):PELT algorithm using the optimal penalty.
 *
 * 
 * @author ezgiyircali
 */
public class OptimalChangepointDetector {

	private final int windowSize;
	private final double minPenalty;
	private final double maxPenalty;

	/**
	 * Constructs a new OptimalChangepointDetector with default parameters(based on LucaTraini 2022)
	 */
	public OptimalChangepointDetector() {
		this.windowSize = 200;
		this.minPenalty = 4.0;
		this.maxPenalty = 100000.0;
	}

	/**
	 * Constructs a new OptimalChangepointDetector with custom parameters.
	 * 
	 * @param windowSize The minimum segment size or window constraint for PELT
	 * @param minPenalty The minimum penalty value for the CROPS algorithm range
	 * @param maxPenalty The maximum penalty value for the CROPS algorithm range
	 */
	public OptimalChangepointDetector(int windowSize, double minPenalty, double maxPenalty) {
		this.windowSize = windowSize;
		this.minPenalty = minPenalty;
		this.maxPenalty = maxPenalty;
	}

	/**
	 * Executes the CROPS, Kneedle, and PELT pipeline on the cleaned time
	 * seriesdata.
	 * 
	 * @param cleanedData Data cleaned from outliers (where outliers are Double.NaN)
	 * @return PeltResult containing the optimal change points and total cost
	 */
	public PeltResult detect(List<Double> cleanedData) {

		if (cleanedData == null || cleanedData.isEmpty()) {
			throw new IllegalArgumentException("Cleaned data cannot be null or empty.");
		}

		// Converting List<Double> to 2D signal array for PELT.
		double[][] signal = new double[cleanedData.size()][1];
		double lastValidValue = 0.0;

		// Finding initial fallback value if series starts with NaN
		for (Double val : cleanedData) {
			if (val != null && !Double.isNaN(val)) {
				lastValidValue = val;
				break;
			}
		}

		for (int i = 0; i < cleanedData.size(); i++) {
			Double val = cleanedData.get(i);
			if (val != null && !Double.isNaN(val)) {
				lastValidValue = val;
			}
			signal[i][0] = lastValidValue;
		}

		// Initializing PELT and CROPS
		Pelt pelt = new Pelt("normal", null, windowSize, 1, null);
		Crops crops = new Crops(pelt);

		// Running CROPS between the minimum and maximum penalty and finding the optimal
		// penalty using Kneedle.
		crops.runCrops(signal, minPenalty, maxPenalty);
		PeltResult optimalResult = crops.getOptimalResultWithKneedle();

		System.out.println("Optimal Changepoint Detection Completed.");
		System.out.println("Detected Change Points: " + optimalResult.getChangePoints());

		return optimalResult;
	}
}