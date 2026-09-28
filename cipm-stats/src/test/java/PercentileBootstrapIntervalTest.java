import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import cipm.stats.bootstrapconfidenceintervals.ConfidenceIntervalStrategy;
import cipm.stats.bootstrapconfidenceintervals.PercentileBootstrapStrategy;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.StatUtils;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;

import java.util.function.ToDoubleFunction;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PercentileBootstrapIntervalTest {

	private static final long TEST_SEED = 12345L; // Fixed seed to ensure reproducible results

	@Test
	@Order(1)
	public void testWithStandardNormalDistribution() {
		System.out.println("RUNNING TEST: SYNTHETIC STANDARD NORMAL DISTRIBUTION - PERCENTILE (N(0,1))");

		// 1. Generate 200 synthetic data points from a standard normal distribution
		NormalDistribution normal = new NormalDistribution(0, 1);
		normal.reseedRandomGenerator(TEST_SEED);

		double[] signal = normal.sample(200);

		// 2. Compute sample statistics to verify they align with expectations
		double sampleMean = StatUtils.mean(signal);
		double sampleVar = StatUtils.variance(signal);
		double sampleStd = Math.sqrt(sampleVar);

		System.out.printf("Generated Sample Mean   : %.4f (Should be close to 0.0)%n", sampleMean);
		System.out.printf("Generated Sample Var    : %.4f (Should be close to 1.0)%n", sampleVar);
		System.out.printf("Generated Sample Std Dev: %.4f (Should be close to 1.0)%n%n", sampleStd);

		// 3. Initialize the Percentile strategy with a fixed seed
		ConfidenceIntervalStrategy percentile = new PercentileBootstrapStrategy(TEST_SEED);

		// --- Mean CI ---
		// Expectation: The 95% confidence interval should contain the true population mean of 0.0
		double[] pMeanInt = percentile.calculateInterval(signal, 0.05, 1000, 0.0, StatUtils::mean);
		System.out.printf("Percentile Mean 95%% CI       : [%.4f, %.4f]%n", pMeanInt[0], pMeanInt[1]);
		assertTrue(pMeanInt[0] <= 0.0 && pMeanInt[1] >= 0.0,
				"Standard normal mean CI should contain the true population mean of 0.0");

		// --- Variance CI ---
		// Expectation: The 95% confidence interval should contain the true population variance of 1.0
		double[] pVarInt = percentile.calculateInterval(signal, 0.05, 1000, 0.0, StatUtils::variance);
		System.out.printf("Percentile Variance 95%% CI   : [%.4f, %.4f]%n", pVarInt[0], pVarInt[1]);
		assertTrue(pVarInt[0] <= 1.0 && pVarInt[1] >= 1.0,
				"Standard normal variance CI should contain the true population variance of 1.0");

		// --- Standard Deviation CI ---
		// Expectation: The 95% confidence interval should contain the true population standard deviation of 1.0
		ToDoubleFunction<double[]> stdDevEstimator = d -> Math.sqrt(StatUtils.variance(d));
		double[] pStdInt = percentile.calculateInterval(signal, 0.05, 1000, 0.0, stdDevEstimator);
		System.out.printf("Percentile Std Dev 95%% CI    : [%.4f, %.4f]%n", pStdInt[0], pStdInt[1]);
		assertTrue(pStdInt[0] <= 1.0 && pStdInt[1] >= 1.0,
				"Standard normal std dev CI should contain the true population std dev of 1.0");

		System.out.println("==========================================================================");
	}

	@Test
	@Order(2)
	public void testWithExponentialDistribution() {
		System.out.println("RUNNING TEST: SYNTHETIC EXPONENTIAL DISTRIBUTION - PERCENTILE");

		double trueMean = 2.0;
		double trueVar = 4.0;
		double trueStd = 2.0;

		ExponentialDistribution exp = new ExponentialDistribution(trueMean);
		exp.reseedRandomGenerator(TEST_SEED);

		double[] signal = exp.sample(10000);

		double sampleMean = StatUtils.mean(signal);
		double sampleVar = StatUtils.variance(signal);
		double sampleStd = Math.sqrt(sampleVar);

		System.out.printf("Generated Sample Mean   : %.4f (Should be close to 2.0)%n", sampleMean);
		System.out.printf("Generated Sample Var    : %.4f (Should be close to 4.0)%n", sampleVar);
		System.out.printf("Generated Sample Std Dev: %.4f (Should be close to 2.0)%n%n", sampleStd);

		ConfidenceIntervalStrategy percentile = new PercentileBootstrapStrategy(TEST_SEED);

		// --- Mean CI ---
		// Expectation: The 95% confidence interval should contain the true population mean of 2.0
		double[] pMeanInt = percentile.calculateInterval(signal, 0.05, 1000, 0.0, StatUtils::mean);
		System.out.printf("Percentile Mean 95%% CI       : [%.4f, %.4f]%n", pMeanInt[0], pMeanInt[1]);
		assertTrue(pMeanInt[0] <= trueMean && pMeanInt[1] >= trueMean,
				"Exponential mean CI should contain the true population mean of 2.0");

		// --- Variance CI ---
		// Expectation: The 95% confidence interval should contain the true population variance of 4.0
		double[] pVarInt = percentile.calculateInterval(signal, 0.05, 1000, 0.0, StatUtils::variance);
		System.out.printf("Percentile Variance 95%% CI   : [%.4f, %.4f]%n", pVarInt[0], pVarInt[1]);
		assertTrue(pVarInt[0] <= trueVar && pVarInt[1] >= trueVar,
				"Exponential variance CI should contain the true population variance of 4.0");

		// --- Standard Deviation CI ---
		// Expectation: The 95% confidence interval should contain the true population standard deviation of 2.0
		ToDoubleFunction<double[]> stdDevEstimator = d -> Math.sqrt(StatUtils.variance(d));
		double[] pStdInt = percentile.calculateInterval(signal, 0.05, 1000, 0.0, stdDevEstimator);
		System.out.printf("Percentile Std Dev 95%% CI    : [%.4f, %.4f]%n", pStdInt[0], pStdInt[1]);
		assertTrue(pStdInt[0] <= trueStd && pStdInt[1] >= trueStd,
				"Exponential std dev CI should contain the true population std dev of 2.0");

		System.out.println("==========================================================================");
	}

	@Test
	@Order(3)
	public void testPreconditionsThrowException() {
		ConfidenceIntervalStrategy percentile = new PercentileBootstrapStrategy(TEST_SEED);
		double[] tinyData = { 1.0, 2.0 }; // Insufficient sample size for bootstrapping

		// Expectation: Verify that the algorithm fails fast with IllegalArgumentException
		assertThrows(IllegalArgumentException.class, () -> {
			percentile.calculateInterval(tinyData, 0.05, 500, 0.0, StatUtils::mean);
		}, "Should fail because data size is less than pre-condition limit.");
	}

}