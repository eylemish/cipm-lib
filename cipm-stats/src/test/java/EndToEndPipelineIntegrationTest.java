import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.Test;

import cipm.stats.bootstrapconfidenceintervals.ArmaProcessGenerator;
import cipm.stats.bootstrapconfidenceintervals.BcaBootstrapStrategy;
import cipm.stats.bootstrapconfidenceintervals.ConfidenceIntervalStrategy;
import cipm.stats.bootstrapconfidenceintervals.PercentileBootstrapStrategy;
import cipm.stats.bootstrapconfidenceintervals.StationaryParameterOptimizer;

public class EndToEndPipelineIntegrationTest {
	
	private static final long TEST_SEED = 12345L;
	private static final String NILE_PATH = "src/test/resources/nile.csv";
	private static final int NILE_COLUMN = 2; // Depending on the dataset structure

	@Test
	public void testCompletePipelineWithArmaData() {
		//Synthetic & Autocorrelated Data Generation via ARMA(1)
		ArmaProcessGenerator generator = new ArmaProcessGenerator(TEST_SEED);
		int n = 500;
		double phi = 0.7; // Strong autocorrelation
		double sigma = 1.0;

		double[] originalData = generator.generateAR1(n, 0.0, phi, sigma);
		assertNotNull(originalData);
		assertEquals(n, originalData.length);

		System.out.println("--- ARMA Pipeline Test Started ---");
		System.out.printf("Original Data Size: %d, Sample Mean: %.4f%n", n, StatUtils.mean(originalData));

		//Finding the Optimal Block Parameter (p_opt)
		StationaryParameterOptimizer optimizer = new StationaryParameterOptimizer();
		double pOpt = optimizer.calculateOptimalP(originalData);

		assertTrue(pOpt > 0.0 && pOpt <= 1.0);
		System.out.printf("Calculated p_opt (Block Probability): %.4f (Average Block Size: %.2f)%n", pOpt, (1.0 / pOpt));

		//Calculating Confidence Intervals by Calling Strategies
		double alpha = 0.05; // 95% Confidence Interval
		int bootstrapCount = 500;

		//Confidence Interval using BCa Strategy
		ConfidenceIntervalStrategy bcaStrategy = new BcaBootstrapStrategy(TEST_SEED);
		double[] bcaInterval = bcaStrategy.calculateInterval(originalData, alpha, bootstrapCount, pOpt, StatUtils::mean);

		//Confidence Interval using Percentile Strategy
		ConfidenceIntervalStrategy percentileStrategy = new PercentileBootstrapStrategy(TEST_SEED);
		double[] percentileInterval = percentileStrategy.calculateInterval(originalData, alpha, bootstrapCount, pOpt, StatUtils::mean);
		System.out.printf("BCa 95% Confidence Interval        : [%.4f, %.4f]%n", bcaInterval[0], bcaInterval[1]);
		System.out.printf("Percentile 95% Confidence Interval : [%.4f, %.4f]%n", percentileInterval[0], percentileInterval[1]);

		// Basic logical assertions
		assertTrue(bcaInterval[0] <= bcaInterval[1]);
		assertTrue(percentileInterval[0] <= percentileInterval[1]);
		
		System.out.println("==========================================================");
		System.out.println("End-to-end ARMA + Bootstrap pipeline successfully completed!");
	}
	
	@Test
	public void testIndependenceCheckUsingAutocorrelation() {
		ArmaProcessGenerator generator = new ArmaProcessGenerator(TEST_SEED);
		int n = 1000;

		// Independent / Weakly Autocorrelated Data (Phi = 0.05)
		double[] independentData = generator.generateAR1(n, 0.0, 0.05, 1.0);
		
		//Strongly Autocorrelated / Dependent Data (Phi = 0.8)
		double[] dependentData = generator.generateAR1(n, 0.0, 0.8, 1.0);

		StationaryParameterOptimizer optimizer = new StationaryParameterOptimizer();
		
		double pOptIndependent = optimizer.calculateOptimalP(independentData);
		double pOptDependent = optimizer.calculateOptimalP(dependentData);

		// For independent data, the optimal block size should be small because points are already independent, requiring no large blocks.
		// For dependent data, the block size should be large.
		System.out.println("--- Independence Check via Autocorrelation ---");
		System.out.printf("Independent-like Data -> p_opt: %.4f (Block Size: %.2f)%n", pOptIndependent, 1.0 / pOptIndependent);
		System.out.printf("Dependent Data        -> p_opt: %.4f (Block Size: %.2f)%n", pOptDependent, 1.0 / pOptDependent);

		//Checking that dependent data's block size must be larger than independent data's block size
		assertTrue((1.0 / pOptDependent) > (1.0 / pOptIndependent), 
			"Autocorrelation check failed: Dependent data must require larger blocks than independent data.");
	}
	
	@Test
	public void testPipelineWithRealWorldNileData() throws Exception {

		double[] nileData = DatasetLoader.loadColumnFromCsv(NILE_PATH, NILE_COLUMN);

		assertNotNull(nileData);
		assertTrue(nileData.length > 0);

		// 1. Calculating optimal block parameter
		StationaryParameterOptimizer optimizer = new StationaryParameterOptimizer();
		double pOpt = optimizer.calculateOptimalP(nileData);

		assertTrue(pOpt > 0.0 && pOpt <= 1.0);

		// 2. Computing 95% Confidence Interval using BCa Strategy
		ConfidenceIntervalStrategy bcaStrategy = new BcaBootstrapStrategy(TEST_SEED);
		double alpha = 0.05;
		int bootstrapCount = 500;

		double[] interval = bcaStrategy.calculateInterval(nileData, alpha, bootstrapCount, pOpt, StatUtils::mean);

		System.out.println("\n--- Real-World Dataset (Nile CSV) Pipeline Test ---");
		System.out.printf("Dataset Size          : %d%n", nileData.length);
		System.out.printf("Calculated p_opt      : %.4f (Block Size: %.2f)%n", pOpt, (1.0 / pOpt));
		System.out.printf("95%% BCa Confidence Int. : [%.2f, %.2f]%n", interval[0], interval[1]);

		// Verifying that interval bounds are logical
		assertTrue(interval[0] <= interval[1]);
	}
}