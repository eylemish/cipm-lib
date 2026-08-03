import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import cipm.stats.pelt.Pelt;

public class PeltTest {
	
/**
 * A signal of 20 units in length is generated, making a sudden transition from 0.0 to 10.0.
 * It is checked whether the algorithm correctly detects the transition point.
 */
	@Test
	public void testBreakPointDetection() {
		//Signal generation
		double[] signal = new double[20];
		for (int i = 0; i < 10; i++)
			signal[i] = 0.0;
		for (int i = 10; i < 20; i++)
			signal[i] = 10.0;

		//Test Pelt
		Pelt pelt = new Pelt("normal", null, 2, 1, null);

		List<Integer> breakpoints = pelt.fitPredict(signal, 1.0);

		assertTrue(breakpoints.contains(10), "Breaking point: 10");
		System.out.println("Breakpoints(simple): " + breakpoints);
	}
	
	/**
	 *  A signal of 20 units in length is generated with random noise, where first half starts with 0 and second half starts with 10.
	 *  It is checked whether the algorithm correctly detects the transition point
	 *  and whether it is robust against noise.
	 */
	@Test
	public void testBreakPointWithNoise() {
		double[] signal = new double[20];
		Random rand = new Random(42); // Noise
		
		//Signal generation
		for (int i = 0; i < 10; i++) {
			signal[i] = 0.0 + (rand.nextGaussian() * 0.1); // 0  + noise
		}
		for (int i = 10; i < 20; i++) {
			signal[i] = 10.0 + (rand.nextGaussian() * 0.1); // 10  + noise
		}

		//Test Pelt
		Pelt pelt = new Pelt("normal", null, 2, 1, null);
		
		List<Integer> breakpoints = pelt.fitPredict(signal, 50.0); // More penalty because of noise

		assertTrue(breakpoints.contains(10), "Breaking point: 10");
		System.out.println("Breakpoints(simple with noise): " + breakpoints);
	}

	@Test
	public void testNileDataset() throws Exception {
		double[] nileSignal = loadNileData();

		Pelt pelt = new Pelt("l2", null, 5, 1, null);
		List<Integer> breakpoints = pelt.fitPredict(nileSignal, 200000.0); // Big penalty because of big dataset and l2 cost function
		
		Pelt pelt2 = new Pelt("normal", null, 5, 1, null);
		List<Integer> breakpoints2 = pelt2.fitPredict(nileSignal, 50000.0);
																	

		assertTrue(breakpoints.size() > 0);
		System.out.println("Breakpoints(Nile with cost l2): " + breakpoints);
		System.out.println("Breakpoints(Nile with cost normal): " + breakpoints2);
	}

	/**
	 * Nile dataset is tested with different penalties with normal cost function.
	 * It is checked whether bigger penalties have less or equal number of break points.
	 * @throws Exception Exception if the Nile dataset file cannot be loaded.
	 */
	@Test
    public void testPenaltyEffect() throws Exception {
        double[] nileSignal = loadNileData();
        Pelt pelt = new Pelt("normal", null, 10, 1, null);
        
        List<Integer> breakpoints1 = pelt.fitPredict(nileSignal, 1.0); // Small Penalty
        List<Integer> breakpoints2 = pelt.fitPredict(nileSignal, 50.0); //Middle Penalty
        List<Integer> breakpoints3 = pelt.fitPredict(nileSignal, 1000000.0); // Big Penalty
        
        System.out.println("Small penalty break point count: " + breakpoints1.size());
        System.out.println("Middle penalty break point count: " + breakpoints2.size());
        System.out.println("Big penalty break point count: " + breakpoints3.size());
        
      
        assertTrue(breakpoints1.size() >= breakpoints2.size());
    }
	
	/**
	 * Cost normal function can handle multidimensional data with the help of covariance matrix. 
	 * A signal of 100 units in length with 2 dimensions is generated, making a sudden transition from 0.0 to 10.0.
	 * It is checked whether the algorithm correctly detects the transition point in index 50 using cost normal function.
	 */
    @Test
    public void testMultiDimensionalData() {
    	//Signal generation
        double[][] signal = new double[100][2];
        for (int i = 0; i < 50; i++) { signal[i][0] = 0.0; signal[i][1] = 0.0; }
        for (int i = 50; i < 100; i++) { signal[i][0] = 10.0; signal[i][1] = 10.0; }

        //Test Pelt
        Pelt pelt = new Pelt("normal", null, 5, 1, null);
        List<Integer> breakpoints = pelt.fitPredict(signal, 50.0);
        
        assertTrue(breakpoints.contains(50));
        
    }
    
    /**
     * A 3-dimensional signal of 200 units with noise is generated, with a sudden shift from 0.0 to 10.0.
     * It is checked whether the algorithm correctly detects the transition point in index 50 using cost normal function
     * and whether it is robust against noise.
     */
    @Test
    public void testNoisyMultiDimentionalData() {
    	//Signal Generation
        int nSamples = 200;
        int nDimensions = 3;
        double[][] signal = new double[nSamples][nDimensions];
        Random rand = new Random();

        // 0-100 clean, 100-200 noisy
        for (int i = 0; i < nSamples; i++) {
            double shift = (i < 100) ? 0.0 : 10.0;
            for (int j = 0; j < nDimensions; j++) {
                signal[i][j] = shift + (rand.nextDouble() * 2 - 1);
            }
        }

        //Test Pelt
        Pelt pelt = new Pelt("normal", null, 10, 1, null);
        List<Integer> breakpoints = pelt.fitPredict(signal, 100.0);

        assertTrue(breakpoints.contains(100));
        System.out.println("Breakpoints(Multidimensional): " + breakpoints);
    }

    
    /**
     * It is checked whether  an IllegalStateException is thrown when the input signal length is smaller than minimum size.
     */
    @Test
    public void testShortSignalThrowsException() {
        double[] signal = {1.0, 2.0}; // smaller than minimum size
        Pelt pelt = new Pelt("l2", null, 5, 1, null);
        
        assertThrows(IllegalStateException.class, () -> {
            pelt.fitPredict(signal, 1.0);
        });
    }
    
    @Test
    public void testSyntheticMeanShift() {
        // Breakpoint'ler tam olarak nerede olduğunu biliyoruz
        double[] signal = new double[300];
        for (int i = 0;   i < 100; i++) signal[i] = 0.0;
        for (int i = 100; i < 200; i++) signal[i] = 8.0;
        for (int i = 200; i < 300; i++) signal[i] = 0.0;

        Pelt pelt = new Pelt("l2", null, 2, 1, null);
        List<Integer> breakpoints = pelt.fitPredict(signal, 1.0);

        assertTrue(breakpoints.contains(100), "100. index'te breakpoint olmalı");
        assertTrue(breakpoints.contains(200), "200. index'te breakpoint olmalı");
    }

    
    
    /**
     * Reads Nile CSV file and loads it as signals.
     * @return  Double array containing Nile River flow data.
     * @throws Exception If an error occurs during file reading
     */
	private double[] loadNileData() throws Exception {
	    List<Double> dataList = new ArrayList<>();
	    String path = "src/test/resources/nile.csv"; 
	    
	    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
	        String line;
	        while ((line = br.readLine()) != null) {
	            // For first line
	            String cleanLine = line.replace("\"", "").trim();
	            
	            // Removing the commas because of CSV format
	            String[] parts = cleanLine.split(",");
	            
	            for (String part : parts) {
	                try {
	                    double val = Double.parseDouble(part.trim());
	                    dataList.add(val);
	                } catch (NumberFormatException e) {
	                    continue; 
	                }
	            }
	        }
	    }
	    return dataList.stream().mapToDouble(d -> d).toArray();
	}
}
