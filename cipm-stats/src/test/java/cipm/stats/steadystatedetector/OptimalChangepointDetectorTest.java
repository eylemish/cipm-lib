package cipm.stats.steadystatedetector;

import cipm.stats.bootstrapconfidenceintervals.ArmaProcessGenerator;
import cipm.stats.pelt.PeltResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the OptimalChangepointDetector class with console reporting.
 * 
 * @author ezgiyircali
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OptimalChangepointDetectorTest {

    private OptimalChangepointDetector changepointDetector;

    @BeforeEach
    void setUp() {
        changepointDetector = new OptimalChangepointDetector();
    }

    @Test
    @Order(1)
    void testDetectWithArmaBreakData() {
        long testSeed = 98765L;
        ArmaProcessGenerator generator = new ArmaProcessGenerator(testSeed);
        
        int n = 1000;
        int trueBreakPoint = 301;
        double[] rawArray = generator.generateAR1WithBreak(n, trueBreakPoint, 0.1, 0.8, 1.0);
        
        List<Double> cleanedData = new ArrayList<>();
        for (double val : rawArray) {
            cleanedData.add(val);
        }

        PeltResult result = changepointDetector.detect(cleanedData);


        assertNotNull(result, "PeltResult should not be null.");
        assertNotNull(result.getChangePoints(), "Change points list should not be null.");
        assertFalse(result.getChangePoints().isEmpty(), "At least one change point should be detected.");

        System.out.println("=== OptimalChangepointDetector Test Results ===");
        System.out.println("True Breakpoint Index      : " + trueBreakPoint);
        System.out.println("Detected Change Points     : " + result.getChangePoints());
        System.out.println("===============================================");
    }
    
  
    @Test
    @Order(2)
    void testDetectWithMultipleBreakpoints() {
        long testSeed = 12345L;
        ArmaProcessGenerator generator = new ArmaProcessGenerator(testSeed);
        
        List<Double> multiBreakData = new ArrayList<>();
        
        int segmentLength = 100; 
        int numBreaks = 8;     
        
        double currentPhi = 0.1;
        
       
        for (int i = 0; i <= numBreaks; i++) {
     
            currentPhi = (i % 2 == 0) ? 0.2 : 0.8; 
            double[] segment = generator.generateAR1(segmentLength, 0.0, currentPhi, 1.0);
            
            for (double val : segment) {
                multiBreakData.add(val);
            }
       
        }

        PeltResult result = changepointDetector.detect(multiBreakData);
        assertNotNull(result, "PeltResult should not be null.");
        assertNotNull(result.getChangePoints(), "Change points list should not be null.");

        System.out.println("=== Multiple Breakpoints Test Results ===");
        System.out.println("Detected Change Points    : " + result.getChangePoints());
        System.out.println("Total Detected Breaks     : " + result.getChangePoints().size());
        System.out.println("=========================================");
    }
    
    @Test
    @Order(3)
    void testDetectWithClearMeanShiftBreakpoints() {
        List<Double> multiBreakData = new ArrayList<>();
        List<Integer> expectedBreakPoints = new ArrayList<>();
        
        int segmentLength = 200;
        int numBreaks = 5; 
      
        double[] meanLevels = {50.0, 150.0, 80.0, 200.0, 100.0, 250.0};
        java.util.Random random = new java.util.Random(15);

        for (int i = 0; i <= numBreaks; i++) {
            double currentMean = meanLevels[i];
            
            for (int j = 0; j < segmentLength; j++) {
                multiBreakData.add(currentMean + (random.nextDouble() * 5.0)); 
            }
            if (i < numBreaks) {
                expectedBreakPoints.add((i + 1) * segmentLength);
            }
        }

        PeltResult result = changepointDetector.detect(multiBreakData);

        assertNotNull(result, "PeltResult should not be null.");
        assertNotNull(result.getChangePoints(), "Change points list should not be null.");

        System.out.println("=== Clear Mean-Shift Breakpoints Test Results ===");
        System.out.println("Expected Breakpoints      : " + expectedBreakPoints);
        System.out.println("Detected Change Points    : " + result.getChangePoints());
        System.out.println("================================================");
    }

}