import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import cipm.stats.pelt.Crops;
import cipm.stats.pelt.Pelt;
import cipm.stats.pelt.PeltResult;

import java.util.List;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CropsAlgorithmTest {

    private static final String RED_WINE_PATH = "src/test/resources/red_wine.csv";
    private static final int RED_WINE_COLUMN = 2; 

    private static final String SYNTHETIC_DEMO_PATH = "src/test/resources/synthetic_demo.csv";
    private static final int SYNTHETIC_DEMO_COLUMN = 1;

    private static final String NILE_PATH = "src/test/resources/nile.csv";
    private static final int NILE_COLUMN = 2;

    // A helper method that converts an array to a 2D matrix.
    private double[][] to2D(double[] signal) {
        double[][] res = new double[signal.length][1];
        for (int i = 0; i < signal.length; i++) res[i][0] = signal[i];
        return res;
    }
    
    @Test
    @Order(1)
    public void testRunCropsInitializationAndMapStorage() throws Exception {
        double[] signal = DatasetLoader.loadColumnFromCsv(SYNTHETIC_DEMO_PATH, SYNTHETIC_DEMO_COLUMN);
        double[][] signal2D = to2D(signal);

        Pelt pelt = new Pelt("l2", null, 2, 1, null);
        Crops crops = new Crops(pelt);

        // Running the algorithm
        Map<Double, PeltResult> results = crops.runCrops(signal2D, 10.0, 100.0);

        assertTrue(results.containsKey(10.0), "The minimum penalty value should be on the map.");
        assertTrue(results.containsKey(100.0), "The maximum penalty value should be on the map.");
        
        //The number of breakpoints decreases (or remains equal) as the penalty amount increases.
        int minPenaltyBpSize = results.get(10.0).getChangePoints().size();
        int maxPenaltyBpSize = results.get(100.0).getChangePoints().size();
        assertTrue(minPenaltyBpSize >= maxPenaltyBpSize);
    }


    @Test
    @Order(2)
    public void testSolveRecursionDiscoveryPower() throws Exception {
        double[] signal = DatasetLoader.loadColumnFromCsv(SYNTHETIC_DEMO_PATH, SYNTHETIC_DEMO_COLUMN);
        double[][] signal2D = to2D(signal);

        Pelt pelt = new Pelt("l2", null, 2, 1, null);
        Crops crops = new Crops(pelt);

        Map<Double, PeltResult> results = crops.runCrops(signal2D, 1.0, 300.0);

        assertTrue(results.size() > 2);
    }

   
    @Test
    @Order(2)
    public void testCropsWithSyntheticDemDataset() throws Exception {
        double[] signal = DatasetLoader.loadColumnFromCsv(SYNTHETIC_DEMO_PATH, SYNTHETIC_DEMO_COLUMN);
        assertNotNull(signal);

        Pelt pelt = new Pelt("l2", null, 2, 1, null);
        Crops crops = new Crops(pelt);

        System.out.println("CROPS UNIT TEST: SYNTHETIC DEMO");
        crops.runCrops(to2D(signal), 2.0, 150.0);
        
        PeltResult optimal = crops.getOptimalResultWithKneedle();
        assertNotNull(optimal);
        assertNotNull(optimal.getChangePoints());
        System.out.println("Synthetic Demo Optimal Breakpoints: " + optimal.getChangePoints());
    }

    @Test
    @Order(3)
    public void testCropsWithNileDataset() throws Exception {
        double[] signal = DatasetLoader.loadColumnFromCsv(NILE_PATH, NILE_COLUMN);
        assertNotNull(signal);

        Pelt pelt = new Pelt("l2", null, 2, 1, null);
        Crops crops = new Crops(pelt);

        System.out.println("CROPS UNIT TEST: NILE");
        crops.runCrops(to2D(signal), 100.0, 500000.0);
        
        PeltResult optimal = crops.getOptimalResultWithKneedle();
        assertNotNull(optimal);
        assertNotNull(optimal.getChangePoints());
        System.out.println("Nile Optimal Breakpoints: " + optimal.getChangePoints());
    }

    @Test
    @Order(4)
    public void testCropsWithRedWineDataset() throws Exception {
        double[] signal = DatasetLoader.loadColumnFromCsv(RED_WINE_PATH, RED_WINE_COLUMN);
        assertNotNull(signal);

        Pelt pelt = new Pelt("l2", null, 5, 1, null);
        Crops crops = new Crops(pelt);

        System.out.println("CROPS UNIT TEST: RED WINE");
        
        crops.runCrops(to2D(signal), 0.5, 50.0);
        PeltResult optimal = crops.getOptimalResultWithKneedle();
        assertNotNull(optimal);
        System.out.println("Red Wine Optimal Breakpoints: " + optimal.getChangePoints());
    }
    
    @Test
    @Order(5)
    public void testCompareRandomPenaltyVsCropsOptimal() throws Exception {
        double[] signal = DatasetLoader.loadColumnFromCsv(NILE_PATH, NILE_COLUMN);
        assertNotNull(signal);

        Pelt pelt = new Pelt("normal", null, 2, 1, null);
        Crops crops = new Crops(pelt);

        // 2.PELT with arbitrary penalties
        double arbitrarySmallPenalty = 2.0; 
        double arbitraryBigPenalty = 10000.0;
        
        List<Integer> randomSmallResult = pelt.fitPredict(signal, arbitrarySmallPenalty);
        List<Integer> randomBigResult = pelt.fitPredict(signal, arbitraryBigPenalty);

        // 3. Finding the optimal break points with CROPS + Kneedle
        crops.runCrops(to2D(signal), 2.0, 1000.0);
        PeltResult optimalCropsResult = crops.getOptimalResultWithKneedle();
        List<Integer> cropsBreakpoints = optimalCropsResult.getChangePoints();

        System.out.println("------------------------------------------------");
        System.out.println("--- PELT vs CROPS COMPARISON ON NILE DATASET ---");
        System.out.println("Arbitrary Small Penalty (" + arbitrarySmallPenalty + ") Breakpoint Count : " + randomSmallResult.size());
        System.out.println("Arbitrary Big Penalty   (" + arbitraryBigPenalty + ") Breakpoint Count : " + randomBigResult.size());
        System.out.println("CROPS + Kneedle Optimal Breakpoints Count           : " + cropsBreakpoints.size());
        System.out.println("CROPS Identified Breakpoints List                   : " + cropsBreakpoints);
        System.out.println("------------------------------------------------");

       
        assertTrue(randomSmallResult.size() > cropsBreakpoints.size(), 
                "Arbitrary small penalty should produce too many breakpoints compared to optimal CROPS.");
        assertFalse(cropsBreakpoints.isEmpty(), "CROPS optimal result should not be empty.");
    }
    
    @AfterEach
    public void afterEachTest() {
        System.out.println("==================================================\n");
    }


}