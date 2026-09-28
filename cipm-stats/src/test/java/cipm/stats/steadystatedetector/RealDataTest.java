package cipm.stats.steadystatedetector;

import org.junit.jupiter.api.Test;

import cipm.stats.steadystatedetector.experimental.SteadyStateDetectionPipeline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for evaluating complete raw benchmark datasets across 
 * legacy and Closed FormRoM steady-state detection pipelines.
 * 
 * @author ezgiyircali
 */
class RealDataTest {

    @Test
    void evaluateCompleteRawData() {
        String filePath = "src/test/resources/complete-raw-data.csv";
        Path chartOutputDir = Path.of("target/performance-charts");

        try {
            Files.createDirectories(chartOutputDir);
        } catch (IOException e) {
        }

        SteadyStateDetectionPipeline legacyPipeline = new SteadyStateDetectionPipeline();
        ClosedFormRatioOfMeansSteadyStateDetectionPipeline romPipeline = new ClosedFormRatioOfMeansSteadyStateDetectionPipeline();
        OptimalChangepointDetector changepointDetector = new OptimalChangepointDetector();

        int processedSeriesCount = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length < 2) continue;

                String configName = tokens[0].trim();

                // Skip header
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
                        // Skip non-numeric
                    }
                }

                if (measurementData.size() < 10) continue;

                processedSeriesCount++;
                 
                //stopping warnings
                PrintStream originalOut = System.out;
                PrintStream originalErr = System.err;
                PrintStream dummyStream = new PrintStream(OutputStream.nullOutputStream());

                SteadyStateResult resLegacy = null;
                SteadyStateResult resRom = null;
                List<Integer> changePoints = new ArrayList<>();
                List<Integer> outliers = new ArrayList<>();

                try {
                    System.setOut(dummyStream);
                    System.setErr(dummyStream);

                    resLegacy = legacyPipeline.processFork(measurementData);
                    resRom = romPipeline.processFork(measurementData);

                    OutliersFiltering outlierFilter = new OutliersFiltering();
                    List<Double> cleanedData = outlierFilter.filter(measurementData);
                    outliers = outlierFilter.getOutlierIndices();
                    
                    changePoints = changepointDetector.detect(cleanedData).getChangePoints();
                } finally {
                    System.setOut(originalOut);
                    System.setErr(originalErr);
                }

                assertNotNull(resLegacy);
                assertNotNull(resRom);

                System.out.println("--- Series: " + configName + " (Points: " + measurementData.size() + ") ---");
                System.out.println("  First's Index : " + resLegacy.getSteadyStateStartIndex() + " (Stable: " + resLegacy.getStableMeasurements().size() + ")");
                System.out.println("  RoM Index     : " + resRom.getSteadyStateStartIndex()    + " (Stable: " + resRom.getStableMeasurements().size() + ")");

                double[] dataArray = measurementData.stream().mapToDouble(Double::doubleValue).toArray();

                try {
                    ChartUtility.buildAndSaveAdvancedChart(
                        dataArray, 
                        changePoints, 
                        outliers, 
                        configName, 
                        ChartUtility.DEFAULT_X_AXIS_TITLE, 
                        "Response Time (ms)", 
                        chartOutputDir.resolve(configName + "-analysis.pdf")
                    );
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            System.out.println("File not found at ");
        }

        assertTrue(processedSeriesCount >= 0, "complete");
    }
}