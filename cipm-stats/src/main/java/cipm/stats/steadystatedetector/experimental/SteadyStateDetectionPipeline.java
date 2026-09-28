package cipm.stats.steadystatedetector.experimental;

import cipm.stats.pelt.PeltResult;
import cipm.stats.steadystatedetector.OptimalChangepointDetector;
import cipm.stats.steadystatedetector.OutliersFiltering;
import cipm.stats.steadystatedetector.SteadyStateResult;

import java.util.List;

/**
 * Pipeline class for the steady-state detection process. 1. Filters outliers
 * using Tukey's method (OutliersFiltering). 2. Detects optimal change points
 * using PELT/CROPS (OptimalChangepointDetector). 3. Evaluates segments using
 * Kalibera and Jones %5 rule to find the steady-state starting point
 * (SteadyStateDetector).
 * 
 * @author ezgiyircali
 */
public class SteadyStateDetectionPipeline {

	private final OutliersFiltering outliersFiltering;
	private final OptimalChangepointDetector changepointDetector;
	private final SteadyStateDetector steadyStateDetector;

	/**
	 * Constructs a new SteadyStateDetectionPipeline instance with default
	 * components.
	 */
	public SteadyStateDetectionPipeline() {
		this.outliersFiltering = new OutliersFiltering();
		this.changepointDetector = new OptimalChangepointDetector();
		this.steadyStateDetector = new SteadyStateDetector();
	}

	/**
	 * * Constructs a new SteadyStateDetectionPipeline with custom components.
	 * 
	 * @param outliersFiltering   The outlier filtering component
	 * @param changepointDetector The optimal change-point detector component
	 * @param steadyStateDetector The steady-state detector component
	 */
	public SteadyStateDetectionPipeline(OutliersFiltering outliersFiltering,
			OptimalChangepointDetector changepointDetector, SteadyStateDetector steadyStateDetector) {
		this.outliersFiltering = outliersFiltering;
		this.changepointDetector = changepointDetector;
		this.steadyStateDetector = steadyStateDetector;
	}

	/**
	 * Processes a single fork's measurements through the complete steady-state
	 * detection pipeline.
	 * 
	 * @param rawForkData Raw measurement data from a single benchmark fork
	 * @return SteadyStateResult containing classification, start index, and stable
	 *         data (Mstable)
	 */
	public SteadyStateResult processFork(List<Double> measurementData) {

		// Step 1: Filtering outliers using Tukey's method
		List<Double> cleanedData = outliersFiltering.filter(measurementData);

		// Step 2-4: Detecting optimal change points
		PeltResult peltResult = changepointDetector.detect(cleanedData);

		// Step 5: Evaluating segments and extracting steady-state data
		SteadyStateResult result = steadyStateDetector.evaluate(cleanedData, peltResult);

		return result;
	}

}