package com.perf.framework;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for all performance tests.
 * Provides common JMeter setup, driver initialization, and helper methods.
 */
@ExtendWith(ExtentReportListener.class)
public abstract class BasePerformanceTest {
    protected static final Logger log = LoggerFactory.getLogger(BasePerformanceTest.class);
    protected JMeterDriver driver;

    /**
     * Initializes a base test with a ready-to-use {@link JMeterDriver} and reporting extension.
     * Keeps test classes lightweight by centralizing common setup.
     */
    public BasePerformanceTest() {
        log.info("Initializing BasePerformanceTest...");
        this.driver = new JMeterDriver();
    }

    /**
     * Creates a JMeter TestPlan with the specified name.
     */
    protected TestPlan createTestPlan(String name) {
        return new TestPlan(name);
    }

    /**
     * Creates a ThreadGroup with the specified configuration.
     *
     * @param threads        number of virtual users
     * @param rampUp         ramp-up time in seconds
     * @param loopController loop controller for the group
     * @return configured thread group
     */
    protected ThreadGroup createThreadGroup(int threads, int rampUp, LoopController loopController) {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setNumThreads(threads);
        threadGroup.setRampUp(rampUp);
        threadGroup.setSamplerController(loopController);
        return threadGroup;
    }

    /**
     * Creates a LoopController with the specified number of loops.
     *
     * @param loops number of iterations per thread
     * @return initialized loop controller
     */
    protected LoopController createLoopController(int loops) {
        LoopController loopController = new LoopController();
        loopController.setLoops(loops);
        loopController.setFirst(true);
        loopController.initialize();
        return loopController;
    }

    /**
     * Creates an HTTP Sampler with the specified configuration.
     *
     * @param name   sampler name for reporting
     * @param domain target host (without protocol)
     * @param port   target port
     * @param path   request path
     * @param method HTTP method (e.g., GET, POST)
     * @return configured HTTP sampler
     */
    protected HTTPSamplerProxy createHttpSampler(String name, String domain, int port, String path, String method) {
        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setDomain(domain);
        httpSampler.setPort(port);
        httpSampler.setPath(path);
        httpSampler.setMethod(method);
        httpSampler.setName(name);

        // Set required properties for the sampler
        httpSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSampler.setProperty(TestElement.GUI_CLASS,
                org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui.class.getName());

        return httpSampler;
    }

    /**
     * Executes the test plan and handles errors appropriately.
     *
     * <p>Augments the tree with an ExtentReport listener for unified reporting, then delegates to
     * {@link JMeterDriver} for execution. Wraps any thrown errors to fail the test with context.</p>
     *
     * @param testPlanTree The JMeter test plan tree to execute
     * @param testPlanName Name of the test plan for logging
     * @throws RuntimeException if execution fails
     */
    protected void runTest(HashTree testPlanTree, String testPlanName) {
        // Add Extent Report Listener
        ExtentReportJMeterListener extentListener = new ExtentReportJMeterListener();
        testPlanTree.add(testPlanTree.getArray()[0], extentListener);

        try {
            log.info("Starting Test Execution: {}", testPlanName);
            driver.runTest(testPlanTree);
            log.info("Test Execution Finished: {}", testPlanName);
        } catch (Throwable t) {
            log.error("Test Execution Failed: {}", testPlanName, t);
            throw new RuntimeException("JMeter test failed: " + testPlanName, t);
        }
    }

    /**
     * Reads an integer property from config, with a default value.
     *
     * @param key          property key
     * @param defaultValue value to use when the property is absent
     * @return integer value or the default
     */
    protected int getIntProperty(String key, int defaultValue) {
        return TestConfiguration.getIntProperty(key, defaultValue);
    }

    /**
     * Reads a string property from config, with a default value.
     *
     * @param key          property key
     * @param defaultValue value to use when the property is absent
     * @return configured value or the default
     */
    protected String getProperty(String key, String defaultValue) {
        return TestConfiguration.getProperty(key, defaultValue);
    }

    /**
     * Reads a string property from config.
     *
     * @param key property key
     * @return configured value or {@code null} if missing
     */
    protected String getProperty(String key) {
        return TestConfiguration.getProperty(key);
    }
}
