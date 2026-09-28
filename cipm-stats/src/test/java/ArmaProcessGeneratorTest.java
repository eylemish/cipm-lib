

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.Test;

import cipm.stats.bootstrapconfidenceintervals.ArmaProcessGenerator;
import cipm.stats.bootstrapconfidenceintervals.StationaryParameterOptimizer;
import cipm.stats.pelt.Crops;
import cipm.stats.pelt.Pelt;
import cipm.stats.pelt.PeltResult;

public class ArmaProcessGeneratorTest {

	private static final long TEST_SEED = 98765L;
	
    // A helper method that converts an array to a 2D matrix.
    private double[][] to2D(double[] signal) {
        double[][] res = new double[signal.length][1];
        for (int i = 0; i < signal.length; i++) res[i][0] = signal[i];
        return res;
    }

	@Test
	public void testAR1AutocorrelationStructure() {
		ArmaProcessGenerator generator = new ArmaProcessGenerator(TEST_SEED);
		int n = 2000;
		double phi = 0.7; // Strong positive autocorrelation
		double sigma = 1.0;

		double[] arData = generator.generateAR1(n, 0.0, phi, sigma);

		assertEquals(n, arData.length);

		// Calculateing p_opt
		StationaryParameterOptimizer optimizer = new StationaryParameterOptimizer();
		double pOpt = optimizer.calculateOptimalP(arData);

		System.out.println("--- AR(1) Proces ---");
		System.out.printf("Theoretical AR(1) Phi: %.2f%n", phi);
		System.out.printf("Calculated p_opt      : %.4f%n", pOpt);
		System.out.printf("Estimated Average Block   : %.2f%n", 1.0 / pOpt);

	}

	@Test
	public void testAR1WithBreakGeneration() {
		ArmaProcessGenerator generator = new ArmaProcessGenerator(TEST_SEED);
		int n = 500;
		int breakPoint = 250;

		double[] breakData = generator.generateAR1WithBreak(n, breakPoint, 0.1, 0.85, 1.0);

		// Checking that the length is correct and the series contains valid variance (not flat)
		assertEquals(n, breakData.length);
		assertTrue(StatUtils.variance(breakData) > 0);
	}
	
	@Test
	public void testWeakVsStrongAutocorrelationBlockSizeComparison() {
		ArmaProcessGenerator generator = new ArmaProcessGenerator(TEST_SEED);
		StationaryParameterOptimizer optimizer = new StationaryParameterOptimizer();
		int n = 2000;

		//Weak Autocorrelation (Phi = 0.1) -> Data points are almost independent
		double[] weakData = generator.generateAR1(n, 0.0, 0.1, 1.0);
		double pOptWeak = optimizer.calculateOptimalP(weakData);
		double avgBlockWeak = 1.0 / pOptWeak;

		//Strong Autocorrelation (Phi = 0.85) -> High memory / strong dependence
		double[] strongData = generator.generateAR1(n, 0.0, 0.85, 1.0);
		double pOptStrong = optimizer.calculateOptimalP(strongData);
		double avgBlockStrong = 1.0 / pOptStrong;

		System.out.println("--- Weak vs Strong Autocorrelation Comparison ---");
		System.out.printf("Weak (Phi=0.1)   -> Avg Block: %.2f (p_opt: %.4f)%n", avgBlockWeak, pOptWeak);
		System.out.printf("Strong (Phi=0.85)-> Avg Block: %.2f (p_opt: %.4f)%n", avgBlockStrong, pOptStrong);

		// Strong autocorrelation should result in larger block sizes than weak autocorrelation
		assertTrue(avgBlockStrong > avgBlockWeak, 
			"Strongly autocorrelated data must have a larger block size than weakly correlated data!");
	}
	
	@Test
	public void testCropsWithSyntheticArmaBreakDataset() {
		try {
			// 1. Generate a controlled synthetic time series with a known break using ArmaProcessGenerator
			// Total length: 500, True Breakpoint: 250
			// Regime 1: Weak autocorrelation (phi1 = 0.1), Regime 2: Strong autocorrelation (phi2 = 0.8)
			int n = 500;
			int trueBreakPoint = 250;

			ArmaProcessGenerator generator = new ArmaProcessGenerator(TEST_SEED);
			double[] signal = generator.generateAR1WithBreak(n, trueBreakPoint, 0.1, 0.8, 1.0);
			assertNotNull(signal);

			// 2. Configure PELT and CROPS algorithms
			Pelt pelt = new Pelt("normal", null, 2, 1, null);
			Crops crops = new Crops(pelt);

			// 3. Run CROPS over a broad penalty spectrum
			System.out.println("--- CROPS TEST ON SYNTHETIC ARMA BREAK DATASET ---");
			crops.runCrops(to2D(signal), 2.0, 1000.0);

			PeltResult optimalResult = crops.getOptimalResultWithKneedle();
			double optimalPenalty = crops.getOptimalPenaltyWithKneedle();
			List<Integer> breakpoints = optimalResult.getChangePoints();

			System.out.println("True Breakpoint Index      : " + trueBreakPoint);
			System.out.println("Optimal Penalty (Beta)     : " + optimalPenalty);
			System.out.println("Identified Breakpoints     : " + breakpoints);

			// 4. Assertions and Validation
			assertNotNull(optimalResult, "CROPS optimal result should not be null.");
			assertFalse(breakpoints.isEmpty(), "CROPS should detect at least one change point.");

			// Verify that at least one of the identified breakpoints is close to the true break (e.g., within ±15 tolerance)
			boolean foundCloseBreakPoint = false;
			int tolerance = 15;
			for (int bp : breakpoints) {
				if (Math.abs(bp - trueBreakPoint) <= tolerance) {
					foundCloseBreakPoint = true;
					break;
				}
			}

			assertTrue(foundCloseBreakPoint, 
				"CROPS + Kneedle should successfully detect the synthetic break around index " + trueBreakPoint);

		} catch (Exception e) {
			fail("Test failed due to an exception: " + e.getMessage());
		}
	}
}