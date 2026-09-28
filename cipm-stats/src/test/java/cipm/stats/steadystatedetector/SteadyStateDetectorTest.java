package cipm.stats.steadystatedetector;

import cipm.stats.pelt.PeltResult;
import cipm.stats.steadystatedetector.experimental.SteadyStateDetector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SteadyStateDetectorTest {

    private SteadyStateDetector steadyStateDetector;

    @BeforeEach
    void setUp() {
        steadyStateDetector = new SteadyStateDetector();
    }

    @Test
    void testEvaluateWithNoChangePoints() {
        List<Double> data = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        PeltResult emptyPeltResult = new PeltResult(new ArrayList<>(), 0.0);

        SteadyStateResult result = steadyStateDetector.evaluate(data, emptyPeltResult);

        assertNotNull(result);
        assertFalse(result.isSteadyState(), "Steady state should not be reached when there are no change points.");
        assertEquals(-1, result.getSteadyStateStartIndex());
        assertTrue(result.getStableMeasurements().isEmpty());
    }

    @Test
    void testEvaluateSteadyStateSuccess() {
        List<Double> testData = new ArrayList<>();
        
        for (int i = 0; i < 100; i++) {
            testData.add(10.0 + (i % 5)); 
        }
        
        for (int i = 0; i < 200; i++) {
            testData.add(50.0 + (i % 2)); 
        }

        List<Integer> mockChangePoints = List.of(100);
        PeltResult mockPeltResult = new PeltResult(mockChangePoints, 150.0);

        SteadyStateResult result = steadyStateDetector.evaluate(testData, mockPeltResult);

        assertNotNull(result);
        assertTrue(result.isSteadyState());
        assertTrue(result.getSteadyStateStartIndex() >= 0);
        assertFalse(result.getStableMeasurements().isEmpty());

        System.out.println("=== Steady State Test Results ===");
        System.out.println("Steady Reached     : " + result.isSteadyState());
        System.out.println("Steady Start Index : " + result.getSteadyStateStartIndex());
        System.out.println("M Stable Size      : " + result.getStableMeasurements().size());
        System.out.println("=================================");
    }
    
    @Test
    void testEvaluateMultiSegmentSteadyState() {
        List<Double> complexData = new ArrayList<>();
        
        for (int i = 0; i < 50; i++) {
            complexData.add(100.0 + (i % 10));
        }
        
        for (int i = 50; i < 120; i++) {
            complexData.add(30.0 + (i % 5));
        }
        
        for (int i = 120; i < 300; i++) {
            complexData.add(50.0 + (i % 2));
        }

        List<Integer> multiChangePoints = List.of(50, 120);
        PeltResult multiPeltResult = new PeltResult(multiChangePoints, 450.0);

        SteadyStateResult result = steadyStateDetector.evaluate(complexData, multiPeltResult);


        assertNotNull(result);
        assertTrue(result.isSteadyState());
        assertTrue(result.getSteadyStateStartIndex() >= 120); 
        assertFalse(result.getStableMeasurements().isEmpty());

        System.out.println("=== Multi-Break Complex Test Results ===");
        System.out.println("Steady Reached     : " + result.isSteadyState());
        System.out.println("Steady Start Index : " + result.getSteadyStateStartIndex());
        System.out.println("M Stable Size      : " + result.getStableMeasurements().size());
        System.out.println("========================================");
    }
}