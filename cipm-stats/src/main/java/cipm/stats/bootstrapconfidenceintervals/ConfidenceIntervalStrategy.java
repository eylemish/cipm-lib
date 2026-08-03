package cipm.stats.bootstrapconfidenceintervals;

import java.util.function.ToDoubleFunction;

/**
 * Interface that represents the strategy pettern for calculating the Bootstrap Confidence Intervals.
 * This interface accommodates both independent data and dependent time series data.
 * @author ezgiyircali
 *
 */
public interface ConfidenceIntervalStrategy {
	
//	double[] calculate(BootstrapStatistic statistic);
	
	//public double[] calculateInterval(double[] data, double alpha, int bootstrapCount);
	
	/**
	 * Calculates the bootstrap confidence interval.
	 * 
	 * @param data The original sample data array
	 * @param alpha The significance level
	 * @param bootstrapCount Number of bootstrap replications
	 * @param pOpt Optimal block probability for time series (Pass 0.0 for classic independent data)
	 * @param theta The statistical metric to calculate (e.g., mean, standart deviation etc.)
	 * @return An array containing [lower_bound, upper_bound]
	 */
	double[] calculateInterval(double[] data, double alpha, int bootstrapCount, double pOpt, ToDoubleFunction<double[]> theta);
}
