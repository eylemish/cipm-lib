package cipm.stats.steadystatedetector;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.markers.SeriesMarkers;

public final class ChartUtility {

	public final static String DEFAULT_X_AXIS_TITLE = "# Measurement";
	private final static String UNIT_MILLISECONDS = "ms";
	private final static String UNIT_SECONDS = "s";
	private final static String UNIT_MINUTES = "min";
	private final static String UNIT_HOURS = "h";
	private final static double MILLISECONDS_OF_ONE_SECOND = 1000;
	private final static double SECONDS_OF_ONE_MINUTE = 60;
	private final static double MINUTES_OF_ONE_HOUR = 60;
	private final static double MILLISECONDS_OF_ONE_MINUTE = MILLISECONDS_OF_ONE_SECOND * SECONDS_OF_ONE_MINUTE;
	private final static double MILLISECONDS_OF_ONE_HOUR = MILLISECONDS_OF_ONE_MINUTE * MINUTES_OF_ONE_HOUR;

	private ChartUtility() {
	}

	public static void buildAndSaveAdvancedChart(double[] data, List<Integer> changePoints, List<Integer> outliers,
			String title, String xAxisTitle, String yAxisTitle, Path chartFile) throws IOException {

		XYChart chart = new XYChartBuilder().title(title).xAxisTitle(xAxisTitle).yAxisTitle(yAxisTitle).width(800)
				.height(500).build();

		chart.getStyler().setLegendVisible(true);
		chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line);

		// X axis
		double[] xData = new double[data.length];
		for (int i = 0; i < data.length; i++) {
			xData[i] = i;
		}

		double minY = Arrays.stream(data).min().orElse(0.0);
		double maxY = Arrays.stream(data).max().orElse(1.0);

		// Line Graph for cleaned data
		XYSeries mainSeries = chart.addSeries("Performance Data", xData, data);
		mainSeries.setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);
		mainSeries.setMarker(SeriesMarkers.NONE);

		// Circles for outliers
		if (outliers != null && !outliers.isEmpty()) {
			double[] outX = new double[outliers.size()];
			double[] outY = new double[outliers.size()];
			for (int i = 0; i < outliers.size(); i++) {
				int idx = outliers.get(i);
				if (idx < data.length) {
					outX[i] = idx;
					outY[i] = data[idx];
				}
			}
			XYSeries outlierSeries = chart.addSeries("Outliers", outX, outY);
			outlierSeries.setXYSeriesRenderStyle(XYSeriesRenderStyle.Scatter);
			outlierSeries.setMarker(SeriesMarkers.CIRCLE);
		}

		// Vertical Lines for change-points
		if (changePoints != null) {
			int cpIndex = 1;
			for (int cp : changePoints) {
				if (cp > 0 && cp < data.length) {
					double[] vertX = new double[] { cp, cp };
					double[] vertY = new double[] { minY, maxY };
					XYSeries cpSeries = chart.addSeries("Segment Bound " + cpIndex++, vertX, vertY);
					cpSeries.setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);
					cpSeries.setMarker(SeriesMarkers.NONE);
				}
			}
		}

		// Segment means
		List<Integer> bounds = new ArrayList<>();
		bounds.add(0);
		if (changePoints != null) {
			for (int cp : changePoints) {
				if (cp > 0 && cp < data.length) {
					bounds.add(cp);
				}
			}
		}
		bounds.add(data.length);

		for (int i = 0; i < bounds.size() - 1; i++) {
			int start = bounds.get(i);
			int end = bounds.get(i + 1);

			double sum = 0;
			int count = 0;
			for (int j = start; j < end; j++) {
				sum += data[j];
				count++;
			}
			double mean = count > 0 ? sum / count : 0;

			double[] meanX = new double[] { start, end - 1 };
			double[] meanY = new double[] { mean, mean };

			XYSeries meanSeries = chart.addSeries("Segment Mean " + (i + 1), meanX, meanY);
			meanSeries.setXYSeriesRenderStyle(XYSeriesRenderStyle.Line);
			meanSeries.setMarker(SeriesMarkers.NONE);
		}

	}

	public static void buildAndSaveChart(double[] data, String title, String xAxisTitle, String yAxisTitle,
			Path chartFile) throws IOException {
		buildAndSaveChart(null, data, title, xAxisTitle, yAxisTitle, chartFile);
	}

	public static void buildAndSaveChart(double[] xData, double[] yData, String title, String xAxisTitle,
			String yAxisTitle, Path chartFile) throws IOException {
		var chart = new XYChartBuilder().title(title).xAxisTitle(xAxisTitle).yAxisTitle(yAxisTitle).build();
		chart.getStyler().setDefaultSeriesRenderStyle(XYSeriesRenderStyle.Line).setLegendVisible(false);

		if (xData == null) {
			chart.addSeries(yAxisTitle, yData);
		} else {
			chart.addSeries(yAxisTitle, xData, yData);
		}

	}

	public static String adjustTimeMillisecondsUnit(double[] data) {
		var min = Arrays.stream(data).min().getAsDouble();
		var minMaxDiff = Arrays.stream(data).max().getAsDouble() - min;
		double unitAdjustingFactor = 0.0;
		String unitName = UNIT_MILLISECONDS;

		if (min > MILLISECONDS_OF_ONE_SECOND && minMaxDiff < MILLISECONDS_OF_ONE_MINUTE / 2) {
			unitAdjustingFactor = MILLISECONDS_OF_ONE_SECOND;
			unitName = UNIT_SECONDS;
		} else if (min > MILLISECONDS_OF_ONE_MINUTE && minMaxDiff < MILLISECONDS_OF_ONE_HOUR / 2) {
			unitAdjustingFactor = MILLISECONDS_OF_ONE_MINUTE;
			unitName = UNIT_MINUTES;
		} else if (minMaxDiff >= MILLISECONDS_OF_ONE_HOUR / 2) {
			unitAdjustingFactor = MILLISECONDS_OF_ONE_HOUR;
			unitName = UNIT_HOURS;
		} else {
			return unitName;
		}

		for (int index = 0; index < data.length; index++) {
			data[index] = data[index] / unitAdjustingFactor;
		}
		return unitName;
	}
}