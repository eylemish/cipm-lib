import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import cipm.stats.pelt.Pelt;

public class RedWineTest {

    private static double[] signal;
    private static final String CSV_PATH = "src/test/resources/red_wine.csv";

    /**
     *Loading the CSV.
     */
    @BeforeAll
    static void loadData() throws Exception {
        signal = loadRedWineData();
        System.out.println("Red wine dataset: " + signal.length + " points");
    }

    /**
     * PELT should find at least one breakpoint.
     */
    @Test
    public void testBreakpointExists() {
        Pelt pelt = new Pelt("l2", null, 5, 1, null);
        List<Integer> breakpoints = pelt.fitPredict(signal, 50000.0);

        assertTrue(breakpoints.size() > 0, "At least one breakpoint should be found");
        System.out.println("Breakpoints(l2): " + breakpoints);
        printBreakpointDates(breakpoints);
    }

    /**
     * It is checked whether bigger penalties have less or equal number of break points.
     */
    @Test
    public void testPenalty() {
        Pelt pelt = new Pelt("l2", null, 5, 1, null);

        List<Integer> bp1 = pelt.fitPredict(signal, 1000.0);
        List<Integer> bp2 = pelt.fitPredict(signal, 50000.0);
        List<Integer> bp3 = pelt.fitPredict(signal, 5000000.0);

        System.out.println("Small penalty  (1000):    " + bp1.size() + " breakpoint → " + bp1);
        System.out.println("Middle penalty   (50000):   " + bp2.size() + " breakpoint → " + bp2);
        System.out.println("Large penalty  (5000000): " + bp3.size() + " breakpoint → " + bp3);

        assertTrue(bp1.size() >= bp2.size(), "Small penalty >= middle penalty");
        assertTrue(bp2.size() >= bp3.size(), "Middle penalty >= large penalty");
    }

    /**
     * It is checked whether with normal function normal penalties can be found
     */
    @Test
    public void testWithNormalCost() {
        Pelt pelt = new Pelt("normal", null, 5, 1, null);
        List<Integer> breakpoints = pelt.fitPredict(signal, 20.0);

        assertTrue(breakpoints.size() > 0, "At least one breakpoint should be found");
        System.out.println("Breakpoints(normal): " + breakpoints);
        printBreakpointDates(breakpoints);
    }
    
    @Test
    public void testWithOptimalL2Cost() {
        Pelt pelt = new Pelt("l2", null, 5, 1, null);
        List<Integer> breakpoints = pelt.fitPredict(signal, 1064254.0);

        assertTrue(breakpoints.size() > 0, "At least one breakpoint should be found");
        System.out.println("Breakpoints(optimal l2): " + breakpoints);
        printBreakpointDates(breakpoints);
    }


 
    private static void printBreakpointDates(List<Integer> breakpoints) {
        String[] months = {"January","February","March","April","May","June",
                           "July","August","September","October","November","December"};
        for (int idx : breakpoints) {
            if (idx >= signal.length) continue;
            int year = 1980 + idx / 12;
            String month = months[idx % 12];
            System.out.println("  → index " + idx + " = " + month + " " + year
                               + " (value: " + (int) signal[idx] + ")");
        }
    }

    /**
     *Reads the CSV file and only looks for red wine column.
     */
    private static double[] loadRedWineData() throws Exception {
        List<Double> dataList = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(CSV_PATH))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; } //skipping the titles of the columns
                String[] parts = line.trim().split(",");
                if (parts.length >= 3) {
                    dataList.add(Double.parseDouble(parts[2].trim()));
                }
            }
        }
        return dataList.stream().mapToDouble(d -> d).toArray();
    }
}