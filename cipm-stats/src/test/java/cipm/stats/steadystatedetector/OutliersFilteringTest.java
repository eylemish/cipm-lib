package cipm.stats.steadystatedetector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
    void testFilterWithExplicitOutlier() {
        List<Double> data = new ArrayList<>();
        Random random = new Random(42);

        for (int i = 0; i < 250; i++) {
            data.add(50.0 + random.nextDouble() * 2.0);
        }

        int outlierIndex = 225;
        double originalOutlierValue = 9999.0;
        data.set(outlierIndex, originalOutlierValue);

        List<Double> cleanedData = outliersFiltering.filter(data);

        assertNotNull(cleanedData);
        assertEquals(data.size(), cleanedData.size());
        assertTrue(Double.isNaN(cleanedData.get(outlierIndex)));

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

        System.out.println("=== Normal Data (No Outliers) Results ===");
        System.out.println("Total Data Size           : " + cleanedData.size());
        System.out.println("Accidental NaN (False Pos): " + nanCount);
        System.out.println("=================================================");

        assertEquals(0, nanCount, "Normal data should not contain any filtered NaN values.");
    }
    
    @Test
    void evaluateRealDataOutliers() {
        String filePath = "src/test/resources/complete-raw-data.csv";
        int processedSeriesCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 2) continue;

                String configName = tokens[0].trim();

                if (isFirstLine && (configName.equalsIgnoreCase("config") || 
                                    configName.equalsIgnoreCase("series") || 
                                    configName.equalsIgnoreCase("configuration") ||
                                    !tokens[1].trim().matches("-?\\d+(\\.\\d+)?"))) {
                    isFirstLine = false;
                    continue;
                }
                isFirstLine = false;

                List<Double> measurementData = new ArrayList<>();

                for (int i = 1; i < tokens.length; i++) {
                    try {
                        measurementData.add(Double.parseDouble(tokens[i].trim()));
                    } catch (NumberFormatException e) {
                    }
                }

                if (measurementData.size() < 10) continue;

                processedSeriesCount++;

                int outlierCount = outliersFiltering.getTotalOutliersCount();

                System.out.println("--- Series [" + processedSeriesCount + "]: " + configName + " (Points: " + measurementData.size() + ") ---");
                System.out.println("  Outliers found         : " + outlierCount);
            }
        } catch (IOException e) {
            System.out.println("File not found");
        }

        assertTrue(processedSeriesCount > 0, "complete");
    }
}