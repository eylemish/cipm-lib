import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import cipm.stats.bootstrapconfidenceintervals.StationaryResampler;

class StationaryResamplerTest {

    private double[] sampleData;
    private static final double P_BIASING = 0.25; // Expected block length of 4

    @BeforeEach
    void setUp() {
        // Simple synthetic time series data
        sampleData = new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
    }

    @Test
    void testDeterministicSequentialFlow() {
        // Initializing with a fixed seed
        long fixedSeed = 42L;
        StationaryResampler resampler = new StationaryResampler(fixedSeed);

        // Generating two series sequentially (using buildPseudoTimeSeries with 2 parameters)
        double[] firstSeries = resampler.buildPseudoTimeSeries(sampleData, P_BIASING);
        double[] secondSeries = resampler.buildPseudoTimeSeries(sampleData, P_BIASING);
        
        assertEquals(sampleData.length, firstSeries.length, "Output series must preserve the original length.");
        assertEquals(sampleData.length, secondSeries.length, "Output series must preserve the original length.");

        // Checking that sequential calls with a fixed seed constructor produce different sequences (avoiding seed-looping).
        boolean isIdentical = true;
        for (int i = 0; i < sampleData.length; i++) {
            if (firstSeries[i] != secondSeries[i]) {
                isIdentical = false;
                break;
            }
        }
        assertFalse(isIdentical, "Sequential calls on the same resampler instance should not yield identical arrays.");
    }

    @Test
    void testThreadScopedSeedIsDeterministic() {
        StationaryResampler resampler = new StationaryResampler(); // default constructor
        long specificSeed = 999L;

     // Generating two series sequentially with same seed (using buildPseudoTimeSeries with 3 paremeters)
        double[] seriesA = resampler.buildPseudoTimeSeries(sampleData, P_BIASING, specificSeed);
        double[] seriesB = resampler.buildPseudoTimeSeries(sampleData, P_BIASING, specificSeed);

        // Assert: They must be absolutely identical
        assertArrayEquals(seriesA, seriesB, "Using the exact same threadSeed must produce identical results.");
    }

    @Test
    void testInvalidProbabilityEdgeCases() {
        StationaryResampler resampler = new StationaryResampler(42L);
        Random mockRng = new Random(42L);

        // If p <= 0 or p >= 1, the block length should always return to 1
        assertEquals(1, resampler.generateGeometricRandomVariable(-0.5, mockRng));
        assertEquals(1, resampler.generateGeometricRandomVariable(0.0, mockRng));
        assertEquals(1, resampler.generateGeometricRandomVariable(1.0, mockRng));
        assertEquals(1, resampler.generateGeometricRandomVariable(1.5, mockRng));
    }

    @Test
    void testGeometricGenerationOutputRange() {
        StationaryResampler resampler = new StationaryResampler();
        Random rng = new Random();

        // Running multiple generations to verify bounds
        for (int i = 0; i < 1000; i++) {
            int blockLength = resampler.generateGeometricRandomVariable(P_BIASING, rng);
            assertTrue(blockLength >= 1, "Generated block length must always be at least 1.");
        }
    }

    @Test
    void testPseudoSeriesDataIntegrity() {
        StationaryResampler resampler = new StationaryResampler();
        double[] pseudoSeries = resampler.buildPseudoTimeSeries(sampleData, P_BIASING);

        // Checks that every element in the pseudo time series exists in the original dataset
        for (double val : pseudoSeries) {
            boolean found = false;
            for (double orig : sampleData) {
                if (Double.compare(val, orig) == 0) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "The value " + val + " in the pseudo-series does not belong to the original data.");
        }
    }
}