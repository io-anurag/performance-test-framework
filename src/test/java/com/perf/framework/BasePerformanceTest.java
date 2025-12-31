package com.perf.framework;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.gui.HeaderPanel;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.extractor.JSR223PostProcessor;
import org.apache.jmeter.testbeans.gui.TestBeanGUI;
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
    protected PerformanceTestRunner runner;

    /**
     * Initializes a base test with a ready-to-use {@link PerformanceTestRunner} and
     * reporting extension.
     * Keeps test classes lightweight by centralizing common setup.
     */
    public BasePerformanceTest() {
        log.info("Initializing BasePerformanceTest...");
        this.runner = new PerformanceTestRunner();
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
        threadGroup.setName("Thread Group");
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
    public HTTPSamplerProxy createHttpSampler(String name, String domain, String path, String method) {
        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setDomain(domain);
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
     * <p>
     * Augments the tree with an ExtentReport listener for unified reporting, then
     * delegates to
     * {@link PerformanceTestRunner} for execution. Wraps any thrown errors to fail
     * the test with context.
     * </p>
     *
     * @param testPlanTree The JMeter test plan tree to execute
     * @param testPlanName Name of the test plan for logging
     * @throws RuntimeException if execution fails
     */
    protected void runTest(HashTree testPlanTree, String testPlanName) {
        runner.runTest(testPlanTree, testPlanName);
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

    /**
     * Executes a simple HTTP performance test using the provided configuration.
     *
     * @param planName Test plan name
     * @param domain   Target domain
     * @param port     Target port
     * @param path     Request path
     * @param method   HTTP method
     * @param threads  Number of threads (users). If null, reads from config.
     * @param loops    Number of loops per thread. If null, reads from config.
     * @param rampUp   Ramp-up time in seconds. If null, reads from config.
     */
    protected void runHttpTest(String planName, String domain, String path, String method, Integer threads,
            Integer loops, Integer rampUp) {
        int finalThreads = (threads != null) ? threads : getIntProperty("threads", 1);
        int finalLoops = (loops != null) ? loops : getIntProperty("loops", 1);
        int finalRampUp = (rampUp != null) ? rampUp : getIntProperty("rampUp", 1);

        TestPlan testPlan = createTestPlan(planName);
        LoopController loopController = createLoopController(finalLoops);
        ThreadGroup threadGroup = createThreadGroup(finalThreads, finalRampUp, loopController);
        HTTPSamplerProxy httpSampler = createHttpSampler("Request " + path, domain, path, method);

        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);
        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(httpSampler);

        runTest(testPlanTree, planName);
    }

    /**
     * Executes a simple HTTP performance test using default configuration from
     * properties.
     *
     * @param planName Test plan name
     * @param path     Request path
     */
    /**
     * Creates a HeaderManager with default headers.
     *
     * @param name  Name of the header manager
     * @param token Auth token to use
     * @return Configured HeaderManager
     */
    protected HeaderManager createHeaderManager(String name, String token) {
        HeaderManager headerManager = new HeaderManager();
        headerManager.setName(name);
        if (token != null && !token.isEmpty()) {
            headerManager.add(new Header("Authorization", "Bearer " + token));
        }
        headerManager.add(new Header("Content-Type", "application/json"));
        headerManager.add(new Header("Accept", "application/json"));
        configureTestElement(headerManager, HeaderManager.class, HeaderPanel.class);
        return headerManager;
    }

    /**
     * Creates a JSR223PostProcessor for logging responses.
     *
     * @param name Name of the logger
     * @return Configured JSR223PostProcessor
     */
    protected JSR223PostProcessor createResponseLogger(String name) {
        JSR223PostProcessor logProcessor = new JSR223PostProcessor();
        logProcessor.setName(name);
        logProcessor.setProperty("scriptLanguage", "groovy");
        String logScript = "log.info('Sample: ' + prev.getSampleLabel() + ' | Code: ' + prev.getResponseCode() + ' | Response: ' + prev.getResponseDataAsString());";
        logProcessor.setProperty("script", logScript);
        configureTestElement(logProcessor, JSR223PostProcessor.class, TestBeanGUI.class);
        return logProcessor;
    }

    /**
     * Configures a TestElement with its test class and GUI class.
     *
     * @param element   The TestElement to configure
     * @param testClass The class of the test element
     * @param guiClass  The GUI class for the test element
     */
    public void configureTestElement(TestElement element, Class<?> testClass, Class<?> guiClass) {
        element.setProperty(TestElement.TEST_CLASS, testClass.getName());
        element.setProperty(TestElement.GUI_CLASS, guiClass.getName());
    }

    protected void startSuite(String suiteName) {
        log.info("Starting new Test Suite: {}", suiteName);
        GlobalSuiteContext.getInstance().initialize(suiteName);
    }

    /**
     * Creates a new Thread Group and attaches it to the current Global Suite.
     * 
     * @param name    Name of this specific Thread Group
     * @param threads Number of users
     * @param rampUp  Ramp-up in seconds
     * @param loops   Number of loops
     * @return A TestContext scoped to this Thread Group (but attached to Global
     *         Plan)
     */
    protected TestContext createSuiteThreadGroup(String name, int threads, int rampUp, int loops) {
        GlobalSuiteContext globalCtx = GlobalSuiteContext.getInstance();
        ListedHashTree testPlanTree = globalCtx.getTestPlanTree();
        TestPlan testPlan = globalCtx.getTestPlan();

        // Create Thread Group Components
        LoopController loopController = createLoopController(loops);
        ThreadGroup threadGroup = createThreadGroup(threads, rampUp, loopController);
        threadGroup.setName(name);

        // Add to Global Tree
        HashTree threadGroupTree = testPlanTree.add(testPlan, threadGroup);

        // Add Standard Components (Header Manager, Logger) strictly to THIS Thread
        // Group to ensure isolation between groups. (Optional: could be global if
        // desired)
        String token = getProperty("target.auth.token");
        HeaderManager headerManager = createHeaderManager(name + " Headers", token);
        threadGroupTree.add(headerManager);

        JSR223PostProcessor logProcessor = createResponseLogger(name + " Logger");
        threadGroupTree.add(logProcessor);

        return new TestContext(testPlanTree, threadGroupTree);
    }

    /**
     * Executes the assembled Global Suite.
     */
    protected void runSuite() {
        log.info("Executing Global Test Suite...");
        ListedHashTree globalTree = GlobalSuiteContext.getInstance().getTestPlanTree();
        String suiteName = GlobalSuiteContext.getInstance().getTestPlan().getName();
        // Use runner to execute
        runner.runTest(globalTree, suiteName);

        // Clean up ThreadLocal
        GlobalSuiteContext.getInstance().clear();
    }
}