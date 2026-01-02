package com.perf.framework;

import com.perf.reporting.ExtentReportJMeterListener;
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
        ExtentReportJMeterListener extentListener = new ExtentReportJMeterListener();
        // Attach listener to the root element so it receives events from the entire plan
        testPlanTree.add(testPlanTree.getArray()[0], extentListener);

        try {
            log.info("Starting Test Execution: {}", testPlanName);
            driver.runTest(testPlanTree, testPlanName);
            extentListener.flush();

            if (extentListener.hasFailures()) {
                String errorMsg = "Test execution contained failures. Check the report for details.";
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            log.info("Test Execution Finished: {}", testPlanName);
        } catch (Throwable t) {
            log.error("Test Execution Failed: {}", testPlanName, t);
            throw new RuntimeException("JMeter test failed: " + testPlanName, t);
        }
    }
}
