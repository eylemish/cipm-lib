package cipm.stats.pelt;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;

import cipm.stats.pelt.SegmentStatisticsCalculator.SegmentSummary;

/**
 * This class groups the time series segments found by PELT into clusters.
 * It uses the K-Means++ algorithm from Apache Commons Math.
 * 
 * @author ezgiyircali
 *
 */
public class SegmentClusterer {
	
	/**
	 * Clusters the generated segment summaries based on their mean and standard deviation).
	 * Each segment is treated as a 2D point (x = Mean, y = StdDev).
	 * 
	 * @param summaries The list of segment summaries calculated after change-point detection
	 * @param k The number of clusters chosen by the user
	 * @return A list of centroid clusters containing the grouped segment points
	 */
    public List<CentroidCluster<DoublePoint>> clusterSegments(List<SegmentSummary> summaries, int k) {
        List<DoublePoint> points = new ArrayList<>();

        // Every segment becomes a 2D point in space: [Mean, StdDev]
        for (SegmentSummary summary : summaries) {
            points.add(new DoublePoint(new double[]{summary.mean, summary.stdDev}));
        }

        // Initializing Apache Commons K-Means++ 
        KMeansPlusPlusClusterer<DoublePoint> clusterer = new KMeansPlusPlusClusterer<>(k, 100);
        
        // Executing the clustering
        return clusterer.cluster(points);
    }
}
