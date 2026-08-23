package cipm.stats.steadystatedetector;

import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the first step of the steady-state detection process: Outliers
 * Filtering.
 * 
 * Identifies and filters anomalous execution times using Tukey's formula with a
 * sliding window of 200 iterations: median ± 3 × (90th percentile - 10th
 * percentile) The first 200 warmup iterations are ignored from the filtering process.
 * 
 * @author ezgiyircali
 *
 */
public class OutliersFiltering {

	private final int windowSize;
	private final int warmupIgnore;
	private final double k;

	private int totalOutliersCount = 0;

	/**
	 * Constructs a new OutliersFiltering instance with default parameters(based on the Tukey method)
	 */
	public OutliersFiltering() {
		this.windowSize = 200;
		this.warmupIgnore = 200;
		this.k = 3.0;
	}

	/**
	 * Constructs a new OutliersFiltering instance with custom parameters.
	 * @param windowSize the size of the sliding window
	 * @param warmupIgnore the number of initial iterations to ignore
	 * @param k the multiplier for the Tukey spread
	 */
	public OutliersFiltering(int windowSize, int warmupIgnore, double k) {
		this.windowSize = windowSize;
		this.warmupIgnore = warmupIgnore;
		this.k = k;
	}

	/**
	 * Takes process-iterations and filters outliers based on the Tukey method
	 * 
	 * @param iterations time series data
	 * @return Cleaned data list with outliers replaced by Double.NaN
	 */
	public List<Double> filter(List<Double> iterations) {

		List<Double> cleanedData = new ArrayList<>(iterations);
		int startIndex = Math.max(windowSize, warmupIgnore);

		Percentile percentile = new Percentile();

		for (int i = startIndex; i < cleanedData.size(); i++) {
			// Extracting the sliding window of the past 200 elements and converting it to a
			// double array
			List<Double> subList = cleanedData.subList(i - windowSize, i);
			double[] windowData = subList.stream().mapToDouble(Double::doubleValue).toArray();

			// Median and Percentile Calculation
			percentile.setData(windowData);
			double median = percentile.evaluate(50.0);
			double p90 = percentile.evaluate(90.0);
			double p10 = percentile.evaluate(10.0);

			// Tukey formula: median ± 3 * (90% ile - 10% ile)
			double spread = p90 - p10;
			double lowerLimit = median - k * spread;
			double upperLimit = median + k * spread;

			double currentValue = cleanedData.get(i);

			// Defining an outlier when it lies outside the lower or upper limit
			if (currentValue < lowerLimit || currentValue > upperLimit) {
				cleanedData.set(i, Double.NaN);
				totalOutliersCount++;
			}
		}

		return cleanedData;
	}

	/**
	 * Returns the total outliers count.
	 * 
	 * @return the outlier count
	 */
	public int getTotalOutliersCount() {
		return totalOutliersCount;
	}
}