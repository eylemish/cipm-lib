package cipm.stats.steadystatedetector;

import cipm.stats.pelt.PeltResult;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.StatUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the 5th step of steady-state detection: using Kalibera and Jones (2013) Section 7.3
 * closed-form formula for the ratio of means.
 * 
 * @author ezgiyircali
 */
public class ClosedFormRatioOfMeansSteadyStateDetector {

    public SteadyStateResult evaluate(List<Double> cleanedData, PeltResult peltResult) {
        if (cleanedData == null || cleanedData.isEmpty() || peltResult == null) {
            throw new IllegalArgumentException("Data and PeltResult cannot be null or empty.");
        }

        List<Integer> changePoints = peltResult.getChangePoints();

        if (changePoints == null || changePoints.isEmpty()) {
            return new SteadyStateResult(false, -1, new ArrayList<>());
        }

        // Maintaining segment data and statistics 
        List<List<Double>> segmentDataList = new ArrayList<>();
        List<Double> means = new ArrayList<>();
        List<Double> variances = new ArrayList<>();
        List<Long> lengths = new ArrayList<>();

        int startIndex = 0;
        
        // Reconstructing segments using change points and calculating basic statistics for each segment
        for (int cp : changePoints) {
            if (cp > startIndex && cp <= cleanedData.size()) {
                List<Double> segData = extractValidData(cleanedData, startIndex, cp);
                if (!segData.isEmpty()) {
                    segmentDataList.add(segData);
                    computeAndAddStats(segData, means, variances, lengths);
                }
            }
            startIndex = cp;
        }

        // Adding the final segment
        if (startIndex < cleanedData.size()) {
            List<Double> finalSegData = extractValidData(cleanedData, startIndex, cleanedData.size());
            if (!finalSegData.isEmpty()) {
                segmentDataList.add(finalSegData);
                computeAndAddStats(finalSegData, means, variances, lengths);
            }
        }

        if (means.isEmpty()) {
            return new SteadyStateResult(false, -1, new ArrayList<>());
        }

        int idx = means.size() - 1;
        long oldLen = lengths.get(idx);
        double oldMean = means.get(idx);
        double oldVar = variances.get(idx);
        
        // Calculating Student-t distribution critical value and half-width (H) for the last segment
        double oldT = 0.0;
        if (oldLen > 1) {
            TDistribution oldTDist = new TDistribution(oldLen - 1);
            oldT = oldTDist.inverseCumulativeProbability(0.05 / 2.0);
        }
        double oldH = Math.sqrt(Math.pow(oldT, 2) * oldVar / oldLen);

        // Traversing backwards from the second last segment to the beginning
        idx = idx - 1;
        int steadyStartIndex = changePoints.get(changePoints.size() - 1);

        while (idx >= 0) {
            long currLen = lengths.get(idx);
            double currMean = means.get(idx);
            double currVar = variances.get(idx);

            // Calculating Student-t distribution critical value and half-width (H) for the current segment
            double currT = 0.0;
            if (currLen > 1) {
                TDistribution currTDist = new TDistribution(currLen - 1);
                currT = currTDist.inverseCumulativeProbability(0.05 / 2.0);
            }
            double currH = Math.sqrt(Math.pow(currT, 2) * currVar / currLen);

            // Closed-Form Ratio of Means formulas
            double term1 = Math.pow(oldMean * currMean, 2);
            double term2 = (Math.pow(oldMean, 2) - Math.pow(oldH, 2)) * (Math.pow(currMean, 2) - Math.pow(currH, 2));
            double factor = Math.sqrt(term1 - term2);

            double denominator = Math.pow(oldMean, 2) - Math.pow(oldH, 2);
            
            if (denominator == 0.0) {
                break;
            }

            // Computing lower and upper bounds of the confidence interval for the ratio of means
            double cdLow = (oldMean * currMean - factor) / denominator;
            double cdUp = (oldMean * currMean + factor) / denominator;

            boolean eqToLast = cdLow >= 0.95 && cdUp <= 1.05 && cdLow <= 1.0 && cdUp >= 1.0;

            if (eqToLast) {
            	// If equivalent, expanding the steady-state starting point backwards
                if (idx < changePoints.size()) {
                    steadyStartIndex = changePoints.get(idx);
                }
            } else {
            	// Stopping if a segment fails the equivalence test
                break;
            }

            idx--;
        }

        List<Double> mStable = new ArrayList<>();
        for (int i = steadyStartIndex; i < cleanedData.size(); i++) {
            Double val = cleanedData.get(i);
            if (val != null && !Double.isNaN(val)) {
                mStable.add(val);
            }
        }

        boolean steadyReached = (steadyStartIndex < changePoints.get(changePoints.size() - 1));

        return new SteadyStateResult(steadyReached, steadyStartIndex, mStable);
    }

    // Filtering out null or NaN values from a specific range of the dataset.
    private List<Double> extractValidData(List<Double> data, int start, int end) {
        List<Double> valid = new ArrayList<>();
        for (int i = start; i < end; i++) {
            Double val = data.get(i);
            if (val != null && !Double.isNaN(val)) {
                valid.add(val);
            }
        }
        return valid;
    }

    // Computing the mean, variance and length of a segment and storing them in respective lists.
    private void computeAndAddStats(List<Double> data, List<Double> means, List<Double> variances, List<Long> lengths) {
       
    	double[] values = data.stream().mapToDouble(Double::doubleValue).toArray();
        
        double mean = StatUtils.mean(values);
        double variance = (values.length > 1) ? StatUtils.variance(values) : 0.0;

        means.add(mean);
        variances.add(variance);
        lengths.add((long) values.length);
    }
}