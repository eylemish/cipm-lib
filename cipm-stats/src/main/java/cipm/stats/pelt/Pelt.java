package cipm.stats.pelt;

import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Java implementation of PELT (Pruned Exact Linear Time) Algorithm. This class
 * identifies breakpoints in time series data using statistical cost functions.
 * 
 * @author ezgiyircali
 *
 */
public class Pelt {

	private BaseCost cost;
	private int minSize;
	private int jump;
	private Integer nSamples;

	/**
	 * Constructs a new Pelt instance with the specified configuration.
	 * 
	 * @param model      The statistical model to use for cost calculation ("l2" or
	 *                   "normal").
	 * @param customCost A user defined cost function. If null, the default model is
	 *                   used.
	 * @param minSize    The minimum number of data points required for a valid
	 *                   segment.
	 * @param jump       The step size for the search algorithm. Effects
	 *                   computational efficiency and precision.
	 * @param params     A map of additional parameters for the cost models.
	 */
	public Pelt(String model, BaseCost customCost, int minSize, int jump, Map<String, Object> params) {

		if (customCost != null) {
			this.cost = customCost;
		} else {
			this.cost = costFactory(model, params);
		}
		this.minSize = Math.max(minSize, this.cost.getMinSize());
		this.jump = jump;
		this.nSamples = null;

	}

	/**
	 * Initializes the cost function based on the requested model type.
	 * 
	 * @param model  The name of the cost function(model).
	 * @param params Additional parameters required by potential specific cost
	 *               models.
	 * @return A BaseCost implementation corresponding to the model.
	 */
	private BaseCost costFactory(String model, Map<String, Object> params) {
		if (model == null) {
			model = "normal"; // Default model
		}

		switch (model) {
		case "l2":
			return new L2Cost();
		case "normal":
			return new CostNormal();
		default:
			throw new IllegalArgumentException("Unknown Model: " + model);
		}
	}

	/**
	 * Executes the core PELT (Pruned Exact Linear Time) recursion to partition the
	 * signal.
	 * 
	 * @param penalty The penalty parameter (higher values result in fewer detected
	 *                change points).
	 * @return A map representing the optimal segments and their associated costs.
	 */
	private Map<PeltSegment, Double> segment(double penalty) {

		// Initialization
		Map<Integer, Map<PeltSegment, Double>> partitions = new HashMap<>(); // partitions[t] contains the optimal
																				// partition of signal[0:t]

		Map<PeltSegment, Double> initPartition = new HashMap<PeltSegment, Double>();
		initPartition.put(new PeltSegment(0, 0), 0.0);
		partitions.put(0, initPartition);

		List<Integer> admissible = new ArrayList<Integer>();

		List<Integer> index = new ArrayList<Integer>();
		for (int k = 0; k < this.nSamples; k += this.jump) {
			if (k >= this.minSize) {
				index.add(k);
			}
		}
		index.add(nSamples); // last point can always be a break point

		// Main Recursion
		for (int bkp : index) {
			// adding a point to the admissible set from the previous loop.
			int newAdmissiblePoint = (int) Math.floor((double) (bkp - this.minSize) / this.jump);
			newAdmissiblePoint *= this.jump;
			admissible.add(newAdmissiblePoint);

			List<Map<PeltSegment, Double>> subProblems = new ArrayList<Map<PeltSegment, Double>>();
			List<Integer> validTs = new ArrayList<Integer>();

			for (int t : admissible) {

				if (!partitions.containsKey(t)) {
					continue; // no partition of 0:t exists
				}

				Map<PeltSegment, Double> tmpPartition = new HashMap<PeltSegment, Double>(partitions.get(t));
				double costValue = this.cost.error(t, bkp) + penalty;
				tmpPartition.put(new PeltSegment(t, bkp), costValue);

				subProblems.add(tmpPartition);
				validTs.add(t);
			}

			// finding the optimal partition
			Map<PeltSegment, Double> bestPartition = null;
			double minTotalCost = Double.MAX_VALUE;

			for (Map<PeltSegment, Double> subProb : subProblems) {
				double currentTotalCost = 0.0;

				for (double val : subProb.values()) {
					currentTotalCost += val;
				}

				if (currentTotalCost < minTotalCost) {
					minTotalCost = currentTotalCost;
					bestPartition = subProb;
				}
			}

			partitions.put(bkp, bestPartition);

			// trimming the admissible set
			List<Integer> nextAdmissible = new ArrayList<Integer>();
			for (int i = 0; i < subProblems.size(); i++) {
				double subProbCost = 0.0;
				for (double value : subProblems.get(i).values()) {
					subProbCost += value;
				}

				if (subProbCost <= minTotalCost + penalty) {
					nextAdmissible.add(validTs.get(i));
				}
			}
			admissible = nextAdmissible;
		}

		Map<PeltSegment, Double> bestPartition = new HashMap<PeltSegment, Double>(partitions.get(this.nSamples));

		bestPartition.remove(new PeltSegment(0, 0));

		return bestPartition;
	}

	/**
	 * Fits the PELT model to a multi dimensional signal. This method prepares the
	 * algorithm for multidimensional analysis by initializing the cost function and
	 * calculating the number of samples from the input matrix.
	 * 
	 * @param signal A 2D array where rows represent time points and columns
	 *               represent dimensions(features).
	 * @return The current Pelt instance for method chaining.
	 */
	public Pelt fit(double[][] signal) {

		if (signal == null || signal.length == 0) {
			throw new IllegalArgumentException("Signal can not be null.");
		}
		this.cost.fit(signal);

		this.nSamples = signal.length;

		return this;
	}

	/**
	 * Fits the PELT model to a single dimensional signal. This is a helper method
	 * that reshapes the 1D array into a 2D matrix with a single dimension.
	 * 
	 * @param signal A 1D array of values representing the time series.
	 * @return The current Pelt instance for method chaining.
	 */
	public Pelt fit(double[] signal) {

		if (signal == null || signal.length == 0) {
			throw new IllegalArgumentException("Signal can not be null.");
		}

		double[][] reshapedSignal = new double[signal.length][1];
		for (int i = 0; i < signal.length; i++) {
			reshapedSignal[i][0] = signal[i];
		}
		return this.fit(reshapedSignal);
	}

	/**
	 * Executes the prediction phase by processing the current signal and returning
	 * the optimal set of detected change points.
	 * 
	 * @param penalty the penalty value to apply.
	 * @return A sorted list of indices representing the change points.
	 */
	public List<Integer> predict(double penalty) {

		if (this.nSamples == null || this.nSamples < this.minSize) {
			throw new IllegalStateException("Signal is too short.");
		}

		Map<PeltSegment, Double> partition = segment(penalty);

		List<Integer> breakPoints = new ArrayList<Integer>();
		for (PeltSegment segment : partition.keySet()) {
			int end = segment.getEnd();
			// Exclude the end of the signal (nSamples) as a breakpoint (tau_m < n)
			if (end < this.nSamples) {
				breakPoints.add(end);
			}
		}

		java.util.Collections.sort(breakPoints);

		return breakPoints;
	}

	/**
	 * Performs a complete fit and predict cycle for a multi dimensional signal.
	 * 
	 * @param signal  the 2D input matrix to be analyzed.
	 * @param penalty the penalty value to apply.
	 * @return A sorted list of detected change point indices.
	 */
	public List<Integer> fitPredict(double[][] signal, double penalty) {
		this.fit(signal);
		return this.predict(penalty);
	}

	/**
	 * Performs a complete fit and predict cycle for a single dimensional signal.
	 * 
	 * @param signal  The 1D input array to be analyzed.
	 * @param penalty The penalty value to apply.
	 * @return A sorted list of detected change point indices.
	 */
	public List<Integer> fitPredict(double[] signal, double penalty) {
		this.fit(signal);
		return this.predict(penalty);
	}

	/**
	 * Fits the signal and returns the detected change points along with the total penalized cost.
	 * 
	 * @param signal The 2D input matrix.
	 * @param penalty The penalty value applied.
	 * @return Pelt result that contains change points and total penalized cost.
	 */
	public PeltResult fitPredictWithCost(double[][] signal, double penalty) {
		this.fit(signal);

		Map<PeltSegment, Double> partition = segment(penalty);

		double totalPenalizedCost = 0.0;
		List<Integer> breakPoints = new ArrayList<>();

		for (Map.Entry<PeltSegment, Double> entry : partition.entrySet()) {
			int end = entry.getKey().getEnd();
			if (end < this.nSamples) {
				breakPoints.add(end);
			}

			totalPenalizedCost += entry.getValue();
		}
		Collections.sort(breakPoints);
		return new PeltResult(breakPoints, totalPenalizedCost);
	}
}
