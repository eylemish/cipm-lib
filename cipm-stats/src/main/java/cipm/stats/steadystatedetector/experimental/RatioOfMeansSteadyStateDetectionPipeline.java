package cipm.stats.steadystatedetector.experimental;

import cipm.stats.pelt.PeltResult;
import cipm.stats.steadystatedetector.OptimalChangepointDetector;
import cipm.stats.steadystatedetector.OutliersFiltering;
import cipm.stats.steadystatedetector.SteadyStateResult;

import java.util.List;

/**
 * Pipeline class for the Ratio of Means steady-state detection process. 
 * 1. Filters outliers using Tukey's method (OutliersFiltering). 
 * 2. Detects optimal change points using PELT/CROPS on cleaned data (OptimalChangepointDetector). 
 * 3. Evaluates segments using Kalibera and Jones Section 7.3 Hierarchical Bootstrap Ratio of Means.
 * 
 * @author ezgiyircali
 */
public class RatioOfMeansSteadyStateDetectionPipeline {

	private final OutliersFiltering outliersFiltering;
	private final OptimalChangepointDetector changepointDetector;
	private final RatioOfMeansSteadyStateDetector ratioOfMeansSteadyStateDetector;

	/**
	 * Constructs a new RatioOfMeansSteadyStateDetectionPipeline instance with default components.
	 */
	public RatioOfMeansSteadyStateDetectionPipeline() {
		this.outliersFiltering = new OutliersFiltering();
		this.changepointDetector = new OptimalChangepointDetector();
		this.ratioOfMeansSteadyStateDetector = new RatioOfMeansSteadyStateDetector();
	}

	/**
	 * Constructs a new RatioOfMeansSteadyStateDetectionPipeline with custom components.
	 * @param outliersFiltering The outlier filtering component
	 * @param changepointDetector The optimal change-point detector component
	 * @param RatioOfMeansSteadyStateDetector The ratio of means steady-state detector component
	 */
	public RatioOfMeansSteadyStateDetectionPipeline(OutliersFiltering outliersFiltering,
			OptimalChangepointDetector changepointDetector, 
			RatioOfMeansSteadyStateDetector ratioOfMeansSteadyStateDetector) {
		this.outliersFiltering = outliersFiltering;
		this.changepointDetector = changepointDetector;
		this.ratioOfMeansSteadyStateDetector = ratioOfMeansSteadyStateDetector;
	}

	/**
	 * Processes a single fork's measurements through the Ratio of Means steady-state
	 * detection pipeline.
	 * 
	 * @param measurementData Raw measurement data from a single benchmark fork
	 * @return SteadyStateResult containing classification, start index, and stable data (Mstable)
	 */
	public SteadyStateResult processFork(List<Double> measurementData) {

		// Step 1: Filtering outliers using Tukey's method
		List<Double> cleanedData = outliersFiltering.filter(measurementData);

		// Step 2-4: Detecting optimal change points on cleaned data
		PeltResult peltResult = changepointDetector.detect(cleanedData);

		// Step 5: Evaluating segments on cleaned data using Ratio of Means  detector
		SteadyStateResult result = ratioOfMeansSteadyStateDetector.evaluate(cleanedData, peltResult);

		return result;
	}
}