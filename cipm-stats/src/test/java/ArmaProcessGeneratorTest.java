

import static org.junit.jupiter.api.Assertions.*;

import org.apache.commons.math3.stat.StatUtils;
import org.junit.jupiter.api.Test;

import cipm.stats.bootstrapconfidenceintervals.ArmaProcessGenerator;
import cipm.stats.bootstrapconfidenceintervals.StationaryParameterOptimizer;

public class ArmaProcessGeneratorTest {

	private static final long TEST_SEED = 98765L;

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
}