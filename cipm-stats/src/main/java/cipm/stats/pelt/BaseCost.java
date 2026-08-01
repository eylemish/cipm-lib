package cipm.stats.pelt;

/**
 * The interface used for statistical cost calculations on a given dataset.
 * @author ezgiyircali
 *
 */
public interface BaseCost {
	
	/**
	 * Returns the minimum number of data points required to form a valid segment.
	 * @return The minimum size limit.
	 */
	int getMinSize();
	/**
	 * Prepares the cost model with the provided signal data.
	 * @param signal The multi dimensional input signal.
	 */
	void fit(double[][] signal);
	/**
	 * Calculates the statistical error (cost) for a specific segment defined by [start, end)
	 * @param start The starting index of the segment.
	 * @param end The ending index of the segment.
	 * @return The calculated cost value for the segment.
	 */
	double error(int start, int end);

}
