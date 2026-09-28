package cipm.stats.steadystatedetector;

import cipm.stats.pelt.PeltResult;
import java.util.List;

/**
 * Pipeline class for the Ratio of Means steady-state detection process. 
 * 1. Filters outliers using Tukey's method (OutliersFiltering). 
 * 2. Detects optimal change points using PELT/CROPS on cleaned data (OptimalChangepointDetector). 
 * 3. Evaluates segments using Kalibera and Jones Section 7.3 Closed-Form Ratio of Means.
 * 
 * @author ezgiyircali
 */
public class ClosedFormRatioOfMeansSteadyStateDetectionPipeline {

	private final OutliersFiltering outliersFiltering;
	private final OptimalChangepointDetector changepointDetector;
	private final ClosedFormRatioOfMeansSteadyStateDetector ratioOfMeansSteadyStateDetector;

	/**
	 * Constructs a new ClosedFormRatioOfMeansSteadyStateDetectionPipeline instance with default components.
	 */
	public ClosedFormRatioOfMeansSteadyStateDetectionPipeline() {
		this.outliersFiltering = new OutliersFiltering();
		this.changepointDetector = new OptimalChangepointDetector();
		this.ratioOfMeansSteadyStateDetector = new ClosedFormRatioOfMeansSteadyStateDetector();
	}

	/**
	 * Constructs a new ClosedFormRatioOfMeansSteadyStateDetectionPipeline with custom components.
	 * @param outliersFiltering The outlier filtering component
	 * @param changepointDetector The optimal change-point detector component
	 * @param ratioOfMeansSteadyStateDetector The closed-form ratio of means steady-state detector component
	 */
	public ClosedFormRatioOfMeansSteadyStateDetectionPipeline(OutliersFiltering outliersFiltering,
			OptimalChangepointDetector changepointDetector, 
			ClosedFormRatioOfMeansSteadyStateDetector ratioOfMeansSteadyStateDetector) {
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

		// Step 5: Evaluating segments on cleaned data using Closed-Form Ratio of Means detector
		SteadyStateResult result = ratioOfMeansSteadyStateDetector.evaluate(cleanedData, peltResult);

		return result;
	}
}