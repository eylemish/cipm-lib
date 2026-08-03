
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import cipm.stats.bootstrapconfidenceintervals.BcaBootstrapStrategy;
import cipm.stats.bootstrapconfidenceintervals.ConfidenceIntervalStrategy;
import cipm.stats.bootstrapconfidenceintervals.PercentileBootstrapStrategy;
import cipm.stats.bootstrapconfidenceintervals.StationaryParameterOptimizer;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;

import java.util.function.ToDoubleFunction;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BootstrapConfidenceIntervalTest {

	private static final String RED_WINE_PATH = "src/test/resources/red_wine.csv";
	private static final int RED_WINE_COLUMN = 2;

	private static final String SYNTHETIC_DEMO_PATH = "src/test/resources/synthetic_demo.csv";
	private static final int SYNTHETIC_DEMO_COLUMN = 1;

	private static final long TEST_SEED = 12425L; // fixed seed to have constant results

	@Test
	@Order(1)
	public void testWithRedWineDataset() throws Exception {
		double[] signal = DatasetLoader.loadColumnFromCsv(RED_WINE_PATH, RED_WINE_COLUMN);
		assertNotNull(signal);

		System.out.println("RUNNING TEST: RED WINE DATASET");
		runFullBootstrapPipeline(signal);
	}

	@Test
	@Order(2)
	public void testWithSyntheticDemDataset() throws Exception {
		double[] signal = DatasetLoader.loadColumnFromCsv(SYNTHETIC_DEMO_PATH, SYNTHETIC_DEMO_COLUMN);
		assertNotNull(signal);

		System.out.println("RUNNING TEST: SYNTHETIC DEMO DATASET");
		runFullBootstrapPipeline(signal);
	}

	@Test
	@Order(3)
	public void testWithStandardNormalDistribution() {
		System.out.println("RUNNING TEST: SYNTHETIC STANDARD NORMAL DISTRIBUTION (N(0,1))");

		// Generating synthetic data points from a standard normal distribution
		NormalDistribution normal = new NormalDistribution(0, 1);
		normal.reseedRandomGenerator(TEST_SEED);

		double[] signal = normal.sample(200);

		// Computing sample statistics to check that they align with expectations
		double sampleMean = StatUtils.mean(signal);
		double sampleVar = StatUtils.variance(signal);
		double sampleStd = Math.sqrt(sampleVar);

		System.out.printf("Generated Sample Mean: %.4f (Should be close to 0.0)%n", sampleMean);
		System.out.printf("Generated Sample Var    : %.4f (Should be close to 1.0)%n", sampleVar);
		System.out.printf("Generated Sample Std Dev: %.4f (Should be close to 1.0)%n%n", sampleStd);

		// Initializing the BCa strategy with a fixed seed
		ConfidenceIntervalStrategy bca = new BcaBootstrapStrategy(TEST_SEED);

		// --- Mean CI ---
		double[] bMeanInt = bca.calculateInterval(signal, 0.05, 1000, 0.0, StatUtils::mean);
		System.out.printf("BCa Mean 95%% CI       : [%.4f, %.4f]%n", bMeanInt[0], bMeanInt[1]);
		assertTrue(bMeanInt[0] <= 0.0 && bMeanInt[1] >= 0.0,
				"Standard normal mean CI should contain the true population mean of 0.0");

		// --- Variance CI ---
		double[] bVarInt = bca.calculateInterval(signal, 0.05, 1000, 0.0, StatUtils::variance);
		System.out.printf("BCa Variance 95%% CI   : [%.4f, %.4f]%n", bVarInt[0], bVarInt[1]);
		assertTrue(bVarInt[0] <= 1.0 && bVarInt[1] >= 1.0,
				"Standard normal variance CI should contain the true population variance of 1.0");

		// --- Standard Deviation CI ---
		ToDoubleFunction<double[]> stdDevEstimator = d -> Math.sqrt(StatUtils.variance(d));
		double[] bStdInt = bca.calculateInterval(signal, 0.05, 1000, 0.0, stdDevEstimator);
		System.out.printf("BCa Std Dev 95%% CI    : [%.4f, %.4f]%n", bStdInt[0], bStdInt[1]);
		assertTrue(bStdInt[0] <= 1.0 && bStdInt[1] >= 1.0,
				"Standard normal std dev CI should contain the true population std dev of 1.0");
		System.out.println("==========================================================================");
	}

	@Test
	@Order(4)
	public void testWithExponentialDistribution() {
		System.out.println("RUNNING TEST: SYNTHETIC EXPONENTIAL DISTRIBUTION");

		// Generating synthetic data points from a standard exponential distribution
		double trueMean = 2.0;
		double trueVar = 4.0;
		double trueStd = 2.0; 

		ExponentialDistribution exp = new ExponentialDistribution(trueMean);
		exp.reseedRandomGenerator(TEST_SEED);

		double[] signal = exp.sample(250);

		// Computing sample statistics to check that they align with expectations
		double sampleMean = StatUtils.mean(signal);
		double sampleVar = StatUtils.variance(signal);
		double sampleStd = Math.sqrt(sampleVar);

		System.out.printf("Generated Sample Mean   : %.4f (Should be close to 2.0)%n", sampleMean);
		System.out.printf("Generated Sample Var    : %.4f (Should be close to 4.0)%n", sampleVar);
		System.out.printf("Generated Sample Std Dev: %.4f (Should be close to 2.0)%n%n", sampleStd);

		// Initializing the BCa strategy with a fixed seed
		ConfidenceIntervalStrategy bca = new BcaBootstrapStrategy(TEST_SEED);

		// --- Mean CI ---
		double[] bMeanInt = bca.calculateInterval(signal, 0.05, 1000, 0.0, StatUtils::mean);
		System.out.printf("BCa Mean 95%% CI       : [%.4f, %.4f]%n", bMeanInt[0], bMeanInt[1]);
		assertTrue(bMeanInt[0] <= trueMean && bMeanInt[1] >= trueMean,
				"Exponential mean CI should contain the true population mean of 2.0");

		// --- Variance CI ---
		double[] bVarInt = bca.calculateInterval(signal, 0.05, 1000, 0.0, StatUtils::variance);
		System.out.printf("BCa Variance 95%% CI   : [%.4f, %.4f]%n", bVarInt[0], bVarInt[1]);
		assertTrue(bVarInt[0] <= trueVar && bVarInt[1] >= trueVar,
				"Exponential variance CI should contain the true population variance of 4.0");

		// --- Standard Deviation CI ---
		ToDoubleFunction<double[]> stdDevEstimator = d -> Math.sqrt(StatUtils.variance(d));
		double[] bStdInt = bca.calculateInterval(signal, 0.05, 1000, 0.0, stdDevEstimator);
		System.out.printf("BCa Std Dev 95%% CI    : [%.4f, %.4f]%n", bStdInt[0], bStdInt[1]);
		assertTrue(bStdInt[0] <= trueStd && bStdInt[1] >= trueStd,
				"Exponential std dev CI should contain the true population std dev of 2.0");

		System.out.println("==========================================================================");
	}

	@Test
	@Order(5)
	public void testPreconditionsThrowException() {
		ConfidenceIntervalStrategy bca = new BcaBootstrapStrategy(TEST_SEED);
		double[] tinyData = { 1.0, 2.0 }; // Very small dataset

		assertThrows(IllegalArgumentException.class, () -> {
			bca.calculateInterval(tinyData, 0.05, 500, 0.0, StatUtils::mean);
		}, "Should fail because data size is less than pre-condition limit.");
	}

	/**
	 * Calculates both Percentile and BCa confidence intervals for Mean, Variance,
	 * and Standard Deviation.
	 */
	private void runFullBootstrapPipeline(double[] data) {
		System.out.println("Data points loaded: " + data.length);

		// p_opt Parameter
		StationaryParameterOptimizer optimizer = new StationaryParameterOptimizer();
		double pOpt = optimizer.calculateOptimalP(data);
		System.out.printf("Calculated p_opt: %.4f (Block length: %.2f)%n", pOpt, (1.0 / pOpt));
		assertTrue(pOpt >= 0.001 && pOpt <= 1.0);

		// Common Test Parameters
		double alpha = 0.05; // %95 Confidence Interval
		int bootstrapCount = 500;

		ConfidenceIntervalStrategy percentile = new PercentileBootstrapStrategy();
		ConfidenceIntervalStrategy bca = new BcaBootstrapStrategy();

		// Mean Test
		System.out.println("\n   METRIC: MEAN");
		System.out.printf("   Original Mean: %.4f%n", StatUtils.mean(data));

		double[] pMeanInt = percentile.calculateInterval(data, alpha, bootstrapCount, pOpt, StatUtils::mean);
		double[] bMeanInt = bca.calculateInterval(data, alpha, bootstrapCount, pOpt, StatUtils::mean);

		System.out.printf("   Percentile 95%% CI : [%.4f, %.4f]%n", pMeanInt[0], pMeanInt[1]);
		System.out.printf("   BCa Adjusted 95%% CI: [%.4f, %.4f]%n", bMeanInt[0], bMeanInt[1]);
		assertTrue(pMeanInt[0] <= pMeanInt[1]);
		assertTrue(bMeanInt[0] <= bMeanInt[1]);

		// Variance Test
		System.out.println("\n   METRIC: VARIANCE");
		System.out.printf("   Original Variance: %.4f%n", StatUtils.variance(data));

		double[] pVarInt = percentile.calculateInterval(data, alpha, bootstrapCount, pOpt, StatUtils::variance);
		double[] bVarInt = bca.calculateInterval(data, alpha, bootstrapCount, pOpt, StatUtils::variance);

		System.out.printf("   Percentile 95%% CI : [%.4f, %.4f]%n", pVarInt[0], pVarInt[1]);
		System.out.printf("   BCa Adjusted 95%% CI: [%.4f, %.4f]%n", bVarInt[0], bVarInt[1]);
		assertTrue(pVarInt[0] <= pVarInt[1]);
		assertTrue(bVarInt[0] <= bVarInt[1]);

		// Standard Deviation Test
		System.out.println("\n   METRIC: STANDARD DEVIATION");
		ToDoubleFunction<double[]> stdDevEstimator = d -> Math.sqrt(StatUtils.variance(d));
		System.out.printf("   Original Std Dev: %.4f%n", stdDevEstimator.applyAsDouble(data));

		double[] pStdInt = percentile.calculateInterval(data, alpha, bootstrapCount, pOpt, stdDevEstimator);
		double[] bStdInt = bca.calculateInterval(data, alpha, bootstrapCount, pOpt, stdDevEstimator);

		System.out.printf("   Percentile 95%% CI : [%.4f, %.4f]%n", pStdInt[0], pStdInt[1]);
		System.out.printf("   BCa Adjusted 95%% CI: [%.4f, %.4f]%n", bStdInt[0], bStdInt[1]);
		assertTrue(pStdInt[0] <= pStdInt[1]);
		assertTrue(bStdInt[0] <= bStdInt[1]);
		System.out.println("==========================================================================");
	}
}