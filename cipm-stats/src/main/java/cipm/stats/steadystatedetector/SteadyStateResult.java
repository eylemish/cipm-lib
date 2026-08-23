package cipm.stats.steadystatedetector;

import java.util.List;

/**
 * The class that represents the result of the steady-state detection for a single fork.
 * 
 * @author ezgiyircali
 *
 */
public class SteadyStateResult {

	private final boolean isSteadyState;
	private final int steadyStateStartIndex;
	private final List<Double> stableMeasurements;

	/**
	 * Constructs a new SteadyStateResult instance.
	 * @param isSteadyState Indicating whether a steady state was detected.
	 * @param steadyStateStartIndex The starting index (st) where the steady state begins.
	 * @param stableMeasurements The list of measurements belonging to the steady-state period (Mstable).
	 */
	public SteadyStateResult(boolean isSteadyState, int steadyStateStartIndex, List<Double> stableMeasurements) {
		this.isSteadyState = isSteadyState;
		this.steadyStateStartIndex = steadyStateStartIndex;
		this.stableMeasurements = stableMeasurements;
	}

	/**
	 * Returns whether the fork reached a steady state.
	 * @return true if steady state is reached, false otherwise.
	 */
	public boolean isSteadyState() {
		return isSteadyState;
	}

	/**
	 * Returns the starting index of the steady-state period.
	 * @return the steady-state start index.
	 */
	public int getSteadyStateStartIndex() {
		return steadyStateStartIndex;
	}

	/**
	 * Returns the subset of measurements belonging to the steady state (Mstable).
	 * @return a list of stable performance measurements.
	 */
	public List<Double> getStableMeasurements() {
		return stableMeasurements;
	}
}
