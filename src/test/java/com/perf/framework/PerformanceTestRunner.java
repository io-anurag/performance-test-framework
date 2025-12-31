package com.perf.framework;

import org.apache.jorphan.collections.HashTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight runner that executes a prepared JMeter test plan with
 * ExtentReports integration.
 *
 * <p>
 * Adds the {@link ExtentReportJMeterListener} to the provided plan tree,
 * delegates execution
 * to {@link JMeterDriver}, and wraps failures to surface meaningful test
 * errors.
 * </p>
 */
public class PerformanceTestRunner {
    private static final Logger log = LoggerFactory.getLogger(PerformanceTestRunner.class);
    private final JMeterDriver driver;

    /**
     * Creates a runner with a default {@link JMeterDriver} instance.
     */
    public PerformanceTestRunner() {
        this.driver = new JMeterDriver();
    }

    /**
     * Executes the given test plan and records results via ExtentReports.
     *
     * @param testPlanTree fully assembled JMeter test plan tree
     * @param testPlanName name used for logging context
     * @throws RuntimeException if execution fails
     */
    public void runTest(HashTree testPlanTree, String testPlanName) {
        // Add Extent Report Listener
        ExtentReportJMeterListener extentListener = new ExtentReportJMeterListener();
        testPlanTree.add(testPlanTree.getArray()[0], extentListener);

        try {
            log.info("Starting Test Execution: {}", testPlanName);
            driver.runTest(testPlanTree);

            // Allow time for async results if any, then flush
            extentListener.flush();
            log.info("Test Execution Finished: {}", testPlanName);
        } catch (Throwable t) {
            log.error("Test Execution Failed: {}", testPlanName, t);
            throw new RuntimeException("JMeter test failed: " + testPlanName, t);
        }
    }
}
