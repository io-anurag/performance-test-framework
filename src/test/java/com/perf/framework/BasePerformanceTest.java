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
     * @param testPlanTree The JMeter test plan tree to execute
     * @param testPlanName Name of the test plan for logging
     */
    protected void runTest(HashTree testPlanTree, String testPlanName) {
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
     */
    protected int getIntProperty(String key, int defaultValue) {
        return TestConfiguration.getIntProperty(key, defaultValue);
    }

    /**
     * Reads a string property from config, with a default value.
     */
    protected String getProperty(String key, String defaultValue) {
        return TestConfiguration.getProperty(key, defaultValue);
    }

    /**
     * Reads a string property from config.
     */
    protected String getProperty(String key) {
        return TestConfiguration.getProperty(key);
    }
}
