package cipm.stats.pelt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.github.shravanasati.kneedle4j.*;

/**
 * Implements the CROPS (Changepoint Range Over a Penalty Spectrum) algorithm
 * based on Haynes, Eckley, and Fearnhead (2014). Runs PELT recursively across a
 * penalty interval to find all optimal segmentations and uses the Kneedle
 * algorithm to detect the most optimal penalty (elbow point).
 * 
 * @author ezgiyircali
 *
 */
public class Crops {

	private Pelt pelt;

	private Map<Double, PeltResult> results = new TreeMap<>();

	public Crops(Pelt pelt) {
		this.pelt = pelt;
	}

	/**
	 * Runs the CROPS algorithm over a specified penalty spectrum [minPenalty,
	 * maxPenalty]. Executes PELT for the boundary penalties first, then recursively
	 * searches for all distinct optimal segmentations within the interval.
	 *
	 * @param signal     The input The input multi-dimensional data.
	 * @param minPenalty The minimum penalty value.
	 * @param maxPenalty The maximum penalty value.
	 * @return A mapping each calculated penalty to its corresponding PeltResult.
	 */
	public Map<Double, PeltResult> runCrops(double[][] signal, double minPenalty, double maxPenalty) {

		results.clear();

		// First running the pelt for minimum and maximum values.
		PeltResult minRes = pelt.fitPredictWithCost(signal, minPenalty);

		PeltResult maxRes = pelt.fitPredictWithCost(signal, maxPenalty);

		while (maxRes.getChangePoints().isEmpty() && maxPenalty > minPenalty) {
			maxPenalty = maxPenalty / 2.0;
			maxRes = pelt.fitPredictWithCost(signal, maxPenalty);
		}

		results.put(minPenalty, minRes);
		results.put(maxPenalty, maxRes);

		solve(signal, minPenalty, maxPenalty);

		return results;
	}

	/**
	 * Recursively evaluates penalty values between beta0 and beta1.
	 * 
	 * @param signal The input signal matrix.
	 * @param beta0  Lower boundary penalty.
	 * @param beta1  Upper boundary penalty.
	 */
	private void solve(double[][] signal, double beta0, double beta1) {

		// To avoid infinite loops in very small intervals
		if (Math.abs(beta1 - beta0) < 1e-9) {
			return;
		}

		PeltResult r0 = results.get(beta0);
		PeltResult r1 = results.get(beta1);

		int m0 = r0.getChangePoints().size(); // number of the breakpoints on left side
		int m1 = r1.getChangePoints().size(); // number of the breakpoints on right side

		// if there is a difference more than 1 between right and left side
		// then there are more penalties(betas) to be discovered
		if (m0 > m1 + 1) {

			// subtracting the penalty from the total cost to find the pure error
			// Total Cost = Error (Cost) + (Penalty × Number of Points)
			double q0 = r0.getTotalCost() - beta0 * m0;
			double q1 = r1.getTotalCost() - beta1 * m1;

			double bInt = (q1 - q0) / (m0 - m1);

			// Stop if bInt is rounded to one of the extreme values
			if (bInt <= beta0 || bInt >= beta1) {
				return;
			}

			PeltResult rInt = pelt.fitPredictWithCost(signal, bInt);
			int mInt = rInt.getChangePoints().size();

			// If the new number of points we find (mInt) is different from the number of
			// points on the boundaries,
			// and we haven't encountered this penalty before
			if (!results.containsKey(bInt)) {
				results.put(bInt, rInt);
			}

			if (mInt != m1) {
				solve(signal, beta0, bInt); // [beta0, beta_int]
				solve(signal, bInt, beta1); // [beta_int, beta1]
			}
		}
	}

	/**
	 * Identifies the most optimal penalty and segmentation result using the Kneedle
	 * algorithm. Maps change-point count on the X axis against penalty values on
	 * the Y axis to locate the elbow point. Uses a nearest neighbor strategy if
	 * CROPS skipped the exact breakpoint count.
	 * 
	 * @return The optimal Pelt Result and segmentation result using the Kneedle
	 *         algorithm.
	 */
	public PeltResult getOptimalResultWithKneedle() {
		if (this.results == null || this.results.isEmpty()) {
			throw new IllegalStateException("Need to run the runCrops method first.");
		}

		// ?? Listing the data from the first map
		List<Map.Entry<Double, PeltResult>> entryList = new ArrayList<>(this.results.entrySet());

		// Filter out trivial m = 0 results (where penalty was too huge to form any
		// breakpoint)
		List<Map.Entry<Double, PeltResult>> validEntries = new ArrayList<>();
		for (Map.Entry<Double, PeltResult> entry : entryList) {
			if (entry.getValue().getChangePoints() != null && !entry.getValue().getChangePoints().isEmpty()) {
				validEntries.add(entry);
			}
		}

		// Fallback: If all penalties in range produced 0 breakpoints, return the first result
		if (validEntries.isEmpty()) {
			System.out.println("CROPS Warning: All penalty ranges produced 0 breakpoints. Returning upper boundary.");
			return entryList.get(0).getValue();
		}

		// Breakpoint count should be X axis
		// Reordering the list from smallest to largest
		Collections.reverse(entryList);

		int size = entryList.size();

		if (size < 3) {
			Map.Entry<Double, PeltResult> midEntry = entryList.get(size / 2);
			return midEntry.getValue();
		}

		double[] xPoints = new double[size]; // Breakpoint Count
		double[] yPoints = new double[size]; // Beta/Penalty Value

		for (int i = 0; i < size; i++) {
			Map.Entry<Double, PeltResult> entry = entryList.get(i);
			xPoints[i] = entry.getValue().getChangePoints().size(); // X Axis = Breakpoint Count
			yPoints[i] = entry.getKey(); // Y Axis = Beta
		}

		// Structuring the KneeLocator Class
		KneeLocator kl = new KneeLocator(xPoints, yPoints, 1.0, // Sensitivity (Standart = 1.0)
				Enums.CURVE_TYPE.CONCAVE, Enums.DIRECTION.DECREASING, Enums.INTERPOLATION_METHOD.POLYNOMIAL, false, // Online
																													// mode
				3 // Polynomial degree
		);

		// Getting the most optimal knee
		Double optimalKneeX = kl.getKnee();

		Map.Entry<Double, PeltResult> optimalEntry = null;

		// Guaranteeing that knee is not empty
		if (optimalKneeX != null) {
			int targetKneeCount = (int) Math.round(optimalKneeX);

			// Searching an exact match for the recommended breakpoint count
			for (Map.Entry<Double, PeltResult> entry : entryList) {
				if (entry.getValue().getChangePoints().size() == targetKneeCount) {
					optimalEntry = entry;
					break;
				}
			}

			// Selecting the nearest neighbor if CROPS skipped this specific count
			if (optimalEntry == null) {
				int minDiff = Integer.MAX_VALUE;
				for (Map.Entry<Double, PeltResult> entry : entryList) {
					int diff = Math.abs(entry.getValue().getChangePoints().size() - targetKneeCount);
					if (diff < minDiff) {
						minDiff = diff;
						optimalEntry = entry;
					}
				}
			}
		}

		// If Kneedle returns null or if no exact matching breakpoint number is found:
		if (optimalEntry == null) {
			System.out.println(
					"CROPS: No distinct elbow point was identified or matched. The most fair midpoint is selected..");
			optimalEntry = entryList.get(size / 2);
		}

		System.out.println("Kneedle4j Optimization:");
		System.out.println("Selected Ideal Breakpoint Number: " + optimalKneeX);
		System.out.println("Selected Ideal Beta (Penalty): " + optimalEntry.getKey());

		return optimalEntry.getValue();
	}

	/**
	 * Identifies the optimal penalty (beta) value corresponding to the optimal
	 * PeltResult.
	 * 
	 * @return
	 */
	public double getOptimalPenaltyWithKneedle() {
		PeltResult optimalResult = getOptimalResultWithKneedle();
		for (Map.Entry<Double, PeltResult> entry : results.entrySet()) {
			if (entry.getValue() == optimalResult) {
				return entry.getKey();
			}
		}
		return -1.0;
	}

}
