package cipm.stats.pelt;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import java.util.ArrayList;
import java.util.List;

public class SegmentStatisticsCalculator {
	
	public static class SegmentSummary {
        public int segmentIndex;
        public int startIndex;
        public int endIndex;
        public double mean;
        public double median;
        public double stdDev;

        @Override
        public String toString() {
            return String.format("Segment %d [%d - %d] -> Mean: %.2f, Median: %.2f, StandartDeviation: %.2f",
                    segmentIndex, startIndex, endIndex, mean, median, stdDev);
        }
    }
	
	public List<SegmentSummary> calculateSegmentStats(double[] signal, List<Integer> changePoints) {
        List<SegmentSummary> summaries = new ArrayList<>();
        
        //Ensuring the break points are in order and adding the boundaries
        List<Integer> boundaries = new ArrayList<>(changePoints);
        java.util.Collections.sort(boundaries);
        
        int start = 0;
        int segmentIdx = 0;

        // Iterating through all segments using a loop
        for (int i = 0; i <= boundaries.size(); i++) { //to avoid missing the last segment, use <= boundaries.size()
            int end = (i < boundaries.size()) ? boundaries.get(i) : signal.length;
            
            // Calculate the statistics if the segment is not empty
            if (end > start) {
                DescriptiveStatistics stats = new DescriptiveStatistics();
                
                for (int j = start; j < end; j++) {
                    stats.addValue(signal[j]);
                }

                // Statistics
                SegmentSummary summary = new SegmentSummary();
                summary.segmentIndex = segmentIdx++;
                summary.startIndex = start;
                summary.endIndex = end - 1;
                summary.mean = stats.getMean();
                summary.median = stats.getPercentile(50); // %50 percentile = Median
                summary.stdDev = stats.getStandardDeviation();

                summaries.add(summary);
            }
            start = end; // The start of the next segment is the end of this segment.
        }

        return summaries;
    }

}
