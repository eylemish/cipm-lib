package cipm.stats.steadystatedetector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the OutliersFiltering class with console reporting.
 * 
 * @author ezgiyircali
 */
class OutliersFilteringTest {

    private OutliersFiltering outliersFiltering;

    @BeforeEach
    void setUp() {
        outliersFiltering = new OutliersFiltering();
    }

    @Test
    void testFilterWithExplicitOutlierAndConsoleOutput() {
        List<Double> data = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < 250; i++) {
            data.add(50.0 + random.nextDouble() * 2.0);
        }

        int outlierIndex = 225;
        double originalOutlierValue = 9999.0;
        data.set(outlierIndex, originalOutlierValue);

        List<Double> cleanedData = outliersFiltering.filter(data);

        // Assertions
        assertNotNull(cleanedData, "Cleaned data should not be null.");
        assertEquals(data.size(), cleanedData.size(), "Data size should remain the same.");
        assertTrue(Double.isNaN(cleanedData.get(outlierIndex)), "The explicit outlier should be replaced by Double.NaN.");

        System.out.println("=== Outliers Filtering Test Results ===");
        System.out.println("Total Data Size           : " + data.size());
        System.out.println("Injected Outlier Index    : " + outlierIndex);
        System.out.println("Original Outlier Value    : " + originalOutlierValue);
        System.out.println("Cleaned Value at Index    : " + cleanedData.get(outlierIndex)); 
        System.out.println("Total Detected Outliers   : " + outliersFiltering.getTotalOutliersCount());
        System.out.println("=======================================");
    }
    
    @Test
    void testFilterWithNormalDataNoOutliers() {
        List<Double> data = new ArrayList<>();
        Random random = new Random(123);

        for (int i = 0; i < 250; i++) {
            data.add(50.0 + random.nextDouble() * 1.0);
        }

        List<Double> cleanedData = outliersFiltering.filter(data);

        assertNotNull(cleanedData);
  
        long nanCount = cleanedData.stream().mapToDouble(Double::doubleValue).filter(Double::isNaN).count();

        System.out.println("=== Test 2: Normal Data (No Outliers) Results ===");
        System.out.println("Total Data Size           : " + cleanedData.size());
        System.out.println("Accidental NaN (False Pos): " + nanCount);
        System.out.println("=================================================");

        assertEquals(0, nanCount, "Normal data should not contain any filtered NaN values.");
    }
}