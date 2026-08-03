package cipm.stats.pelt;

import java.util.List;

/**
 * Encapsulates the output of a PELT change-point detection analysis. Stores
 * both the detected change-point indices and the resulting overall cost value.
 * 
 * @author ezgiyircali
 *
 */
public class PeltResult {

	private final List<Integer> changePoints;
	private final double totalCost;

	/**
	 * Constructs a new PeltResult container.
	 * 
	 * @param changePoints The list of detected change-point indices.
	 * @param totalCost    The overall segment cost calculated by PELT.
	 */
	public PeltResult(List<Integer> changePoints, double totalCost) {
		this.changePoints = changePoints;
		this.totalCost = totalCost;
	}

	/**
	 * Returns the detected change-point indices.
	 * 
	 * @returnA list of integer indices representing the change points.
	 */
	public List<Integer> getChangePoints() {
		return changePoints;
	}

	/**
	 * Returns the total partition cost associated with the segmentation.
	 * 
	 * @returnThe total cost as a double value.
	 */
	public double getTotalCost() {
		return totalCost;
	}

}
