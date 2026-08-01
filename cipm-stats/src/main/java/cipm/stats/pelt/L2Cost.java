package cipm.stats.pelt;

import org.apache.commons.math3.stat.StatUtils;

/**
 * Implements the L2 (Least Squares) cost function for structural change detection.
 * This model detects change points by minimizing the sum of squared deviations from the mean within each segment.
 * @author ezgiyircali
 *
 */
public class L2Cost implements BaseCost {
	
	private double[][] signal;

	/**
	 * Returns the minimum number of data points required which is 1.
	 */
	@Override
	public int getMinSize() {
		return 1;
	}

	/**
	 * Fits the model to the input signal data.
	 */
	@Override
	public void fit(double[][] signal) {
		this.signal = signal;
	}

	/**
	 * Calculates the sum of squared deviations for a segment [start, end).
	 */
	@Override
	public double error(int start, int end) {
		int n = end - start;
        if (n < getMinSize()) return 0.0;

        double cost = 0.0;
        int dim = signal[0].length;
        
        for (int j = 0; j < dim; j++) {
            double[] subArray = new double[n];
            for (int i = start; i < end; i++) {
                subArray[i - start] = signal[i][j];
            }

            //Sum of Squared Errors (SSE)
            // SSE = sum((x - mean)^2) = sum(x^2) - n * mean^2
            double mean = StatUtils.mean(subArray);
            cost += StatUtils.sumSq(subArray) - n * mean * mean;
        }
        
        
        return cost;
	}

}
