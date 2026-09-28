# CIPM Stats Library

This repository contains statistical analysis, change-point detection, and steady-state detection algorithms developed for performance evaluation and simulation output analysis.

## Package Structure & Components

### 1. `cipm.stats.bootstrapconfidenceintervals`
This package provides advanced statistical tools for estimating confidence intervals on both independent and dependent (autocorrelated) time series data, applying bootstrap strategies, and generating synthetic time series.

* **`ArmaProcessGenerator`**: Generates synthetic time series for ARMA processes; includes `generateAR1`, `generateARMA`, and `generateAR1WithBreak` methods for algorithm testing and regime-shift scenarios.
* **`BcaBootstrapStrategy`**: Implements the advanced Bias-Corrected and Accelerated (BCa) bootstrap method to calculate confidence intervals; supports parallel processing and stationary block bootstrapping.
* **`ConfidenceIntervalStrategy`**: Defines a common interface to standardize confidence interval calculation strategies for various data structures.
* **`PercentileBootstrapStrategy`**: Applies the classical Percentile Bootstrap method to determine confidence interval boundaries.
* **`StationaryParameterOptimizer`**: Calculates the optimal p parameter for Stationary Bootstrap using the Politis-White formula via autocorrelation function and bandwidth detection.
* **`StationaryResampler`**: Constructs pseudo time series for Stationary Bootstrap by extracting data blocks with random lengths based on a geometric distribution.

### 2. `cipm.stats.pelt`
Implements exact and efficient change-point detection algorithms along with various statistical cost functions and automated parameter optimization.

* **`BaseCost`**: Defines the base interface for statistical cost calculations on a given dataset, managing segment size limits, model fitting, and segment error evaluation.
* **`CostNormal`**: Implements the Gaussian (Normal) distribution assumption cost function using determinant-based covariance matrix calculations and regularization for multivariate signals.
* **`Crops`**: Implements the CROPS (Changepoint Range Over a Penalty Spectrum) algorithm to find all optimal segmentations across a penalty interval, using the Kneedle algorithm to detect the optimal penalty elbow point.
* **`L2Cost`**: Implements the L2 (Least Squares) cost function for structural change detection by minimizing the sum of squared deviations from the mean within each segment.
* **`Pelt`**: Implements the Pruned Exact Linear Time (PELT) algorithm for change-point detection in univariate or multivariate time series data using configurable cost functions.
* **`PeltResult`**: Encapsulates the output of a PELT analysis by storing both the detected change-point indices and the overall penalized cost value.
* **`PeltSegment`**: Represents an individual data segment defined by inclusive start and exclusive end indices.
* **`SegmentClusterer`**: Groups time series segments identified by PELT into clusters using the K-Means++ algorithm based on their mean and standard deviation properties.
* **`SegmentStatisticsCalculator`**: Computes descriptive statistics (mean, median, standard deviation) for each distinct segment produced by change-point detection boundaries.

### 3.1 `cipm.stats.steadystatedetector`
Provides comprehensive pipelines, algorithms, and visualization tools for detecting steady-state performance in benchmark measurements through outlier filtering, change-point detection, and closed-form ratio of means evaluation.

* **`ChartUtility`**: Generates and saves advanced charts visualizing performance data, outliers, change-point boundaries, and segment means, alongside unit adjustment utilities.
* **`ClosedFormRatioOfMeansSteadyStateDetectionPipeline`**: Steady-state detection pipeline by combining outlier filtering, change-point detection, and ratio of means evaluation.
* **`ClosedFormRatioOfMeansSteadyStateDetector`**: Implements Kalibera and Jones (2013) Section 7.3 closed-form ratio of means approach over PELT segments to evaluate and extract stable data ranges.
* **`OptimalChangepointDetector`**: Combines CROPS, Kneedle and PELT change-point detection on cleaned time series data.
* **`OutliersFiltering`**: Implements Tukey's method with a sliding window to identify and filter anomalous execution times and warm-up periods.
* **`SteadyStateResult`**: Encapsulates the steady-state detection output for a single fork, including status flags, start indices, and stable measurement subsets.

### 3.2 `cipm.stats.steadystatedetector.experimental`
Provides experimental implementations for steady-state detection and ratio of means calculations using hierarchical bootstrapping and advanced confidence interval strategies.

* **`RatioOfMeansCalculator`**: Computes confidence intervals for the ratio of means between hierarchical datasets through recursive resampling and bootstrap simulation based on Kalibera and Jones Section 7.3.
* **`RatioOfMeansSteadyStateDetectionPipeline`**: An experimental pipeline that integrates outlier filtering, optimal change-point detection, and hierarchical bootstrap ratio of means evaluation.
* **`RatioOfMeansSteadyStateDetector`**: Implements steady-state evaluation over PELT segments using Kalibera and Jones Section 7.3 Ratio of Means via hierarchical bootstrap simulation and BCa confidence intervals.
* **`SteadyStateDetectionPipeline`**: An alternative steady-state detection pipeline combining Tukey outlier filtering, PELT/CROPS change-point detection, and 5% tolerance rule evaluation.
* **`SteadyStateDetector`**: Evaluates PELT segments using Kalibera and Jones 5% tolerance rule via BCa bootstrap confidence intervals to determine the steady-state starting time.

### Visualization
* **`RealDataTest`**: A test class that processes complete raw benchmark datasets from CSV files, compares the (legacy) steady-state detection pipeline against the Closed-Form Ratio of Means pipeline, outputs comparative metrics (start indices and stable counts) to the console, and automatically generates and saves advanced visual charts (showing raw data, Tukey outliers, PELT change-point boundaries, and segment means) into the `target/performance-charts` directory.

---