package cipm.stats.steadystatedetector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cipm.stats.bootstrapconfidenceintervals.ArmaProcessGenerator;
import cipm.stats.steadystatedetector.experimental.RatioOfMeansSteadyStateDetectionPipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the RatioOfMeansSteadyStateDetectionPipeline class.
 * 
 * @author ezgiyircali
 */
class RatioOfMeansSteadyStateDetectionPipelineTest {

    private RatioOfMeansSteadyStateDetectionPipeline pipeline;

    @BeforeEach
    void setUp() {
        pipeline = new RatioOfMeansSteadyStateDetectionPipeline();
    }

    @Test
    void testProcessForkWithValidData() {
        List<Double> sampleData = generate500SampleData();

        SteadyStateResult result = pipeline.processFork(sampleData);

        assertNotNull(result, "Pipeline result should not be null.");
        assertNotNull(result.getStableMeasurements(), "Stable measurements list should not be null.");
        assertTrue(result.getSteadyStateStartIndex() >= 0, "Start index should be valid.");
    }
    
    @Test
    void testProcessForkWithValidData1() {
        List<Double> sampleData500 = generate500SampleData();
        SteadyStateResult result500 = pipeline.processFork(sampleData500);
        
        assertNotNull(result500, "Pipeline result should not be null.");
        assertNotNull(result500.getStableMeasurements(), "Stable measurements list should not be null.");

        System.out.println("=== Ratio of Means Steady-State Results (500) ===");
        System.out.println("Is Steady State Reached? : " + result500.isSteadyState());
        System.out.println("Steady State Start Index  : " + result500.getSteadyStateStartIndex());
        System.out.println("Total Stable Measurements : " + result500.getStableMeasurements().size());
        if (!result500.getStableMeasurements().isEmpty()) {
            System.out.println("First 5 Stable Values     : " + result500.getStableMeasurements().subList(0, Math.min(5, result500.getStableMeasurements().size())));
        }
        System.out.println("==================================================");
    }
    
    @Test
    void testProcessForkWithValidData2() {
        List<Double> sampleData1000 = generate1000SampleData();
        SteadyStateResult result1000 = pipeline.processFork(sampleData1000);

        assertNotNull(result1000, "Pipeline result should not be null.");
        assertNotNull(result1000.getStableMeasurements(), "Stable measurements list should not be null.");

        System.out.println("=== Ratio of Means Steady-State Results (1000) ===");
        System.out.println("Is Steady State Reached? : " + result1000.isSteadyState());
        System.out.println("Steady State Start Index  : " + result1000.getSteadyStateStartIndex());
        System.out.println("Total Stable Measurements : " + result1000.getStableMeasurements().size());
        if (!result1000.getStableMeasurements().isEmpty()) {
            System.out.println("First 5 Stable Values     : " + result1000.getStableMeasurements().subList(0, Math.min(5, result1000.getStableMeasurements().size())));
        }
        System.out.println("===================================================");
    }

    private List<Double> generate500SampleData() {
        List<Double> data = new ArrayList<>();
        Random random = new Random(12345);

        // First 200 iterations
        for (int i = 0; i < 200; i++) {
            data.add(100.0 + random.nextDouble() * 50.0);
        }

        // Subsequent iterations: Steady-state phase
        for (int i = 200; i < 500; i++) {
            data.add(50.0 + random.nextDouble() * 5.0);
        }

        return data;
    }
    
    private List<Double> generate1000SampleData() {
        List<Double> data = new ArrayList<>();
        Random random = new Random(12345);

        for (int i = 0; i < 200; i++) {
            data.add(100.0 + random.nextDouble() * 50.0);
        }

        for (int i = 200; i < 1000; i++) {
            data.add(50.0 + random.nextDouble() * 5.0);
        }

        return data;
    }
    
    @Test
    void testProcessForkWithArmaBreakData() {
        long testSeed = 98765L;
        ArmaProcessGenerator generator = new ArmaProcessGenerator(testSeed);
        
        int n = 500;
        int trueBreakPoint = 250;
   
        double[] rawArray = generator.generateAR1WithBreak(n, trueBreakPoint, 0.1, 0.8, 1.0);
        
        List<Double> sampleData = new ArrayList<>();
        for (double val : rawArray) {
            sampleData.add(val);
        }

        SteadyStateResult result = pipeline.processFork(sampleData);

        assertNotNull(result, "Pipeline result should not be null.");
        assertNotNull(result.getStableMeasurements(), "Stable measurements list should not be null.");

        System.out.println("=== ARMA Break Dataset Ratio of Means Pipeline Results ===");
        System.out.println("Is Steady State Reached? : " + result.isSteadyState());
        System.out.println("Steady State Start Index  : " + result.getSteadyStateStartIndex());
        System.out.println("Total Stable Measurements : " + result.getStableMeasurements().size());
        System.out.println("===========================================================");
    }
}