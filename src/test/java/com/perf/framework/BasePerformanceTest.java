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
        log.info("Initializing Base Performance Test...");
        this.runner = new PerformanceTestRunner();
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
     * Builds and runs a simple single-sampler HTTP plan using provided or defaulted concurrency settings.
     */
    protected void runSimpleHttpPlan(String planName, String domain, String path, String method, Integer threads,
            Integer loops, Integer rampUp) {
        HashTree planTree = buildSimpleHttpPlan(planName, domain, path, method, threads, loops, rampUp);
        runTest(planTree, planName);
    }

    /**
     * Build a minimal TestPlan → ThreadGroup → HTTP Sampler tree for a single request.
     */
    protected HashTree buildSimpleHttpPlan(String planName, String domain, String path, String method, Integer threads,
            Integer loops, Integer rampUp) {
        int finalThreads = (threads != null) ? threads : TestConfiguration.getIntProperty("threads", 1);
        int finalLoops = (loops != null) ? loops : TestConfiguration.getIntProperty("loops", 1);
        int finalRampUp = (rampUp != null) ? rampUp : TestConfiguration.getIntProperty("rampUp", 1);

        TestPlan testPlan = TestPlanFactory.createTestPlan(planName);
        LoopController loopController = TestPlanFactory.createLoopController(finalLoops);
        ThreadGroup threadGroup = TestPlanFactory.createThreadGroup(planName + " Thread Group", finalThreads, finalRampUp, loopController);
        HTTPSamplerProxy httpSampler = TestPlanFactory.createHttpSampler("Request " + path, domain, path, method);
        JSR223PostProcessor logProcessor = isResponseLoggingEnabled() ? createResponseLogger("Response Logger") : null;

        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);
        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(httpSampler);
        if (logProcessor != null) {
            threadGroupHashTree.add(logProcessor);
        }
        return testPlanTree;
    }
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
            String authValue = token.startsWith("Bearer ") ? token : "Bearer " + token;
            headerManager.add(new Header("Authorization", authValue));
        }
        headerManager.add(new Header("Content-Type", "application/json"));
        headerManager.add(new Header("Accept", "application/json"));
        configureTestElement(headerManager, HeaderManager.class, HeaderPanel.class);
        return headerManager;
    }

    /**
     * Gets an existing HeaderManager from the tree or creates a new one.
     * This prevents overwriting previously configured headers.
     *
     * @param threadGroupTree The HashTree to search for existing HeaderManager
     * @param name           Name for the header manager if creating new
     * @param token          Auth token to use if creating new
     * @return Existing or newly created HeaderManager
     */
    protected HeaderManager getOrCreateHeaderManager(HashTree threadGroupTree, String name, String token) {
        // Check if HeaderManager already exists in the tree
        for (Object key : threadGroupTree.list()) {
            if (key instanceof HeaderManager) {
                return (HeaderManager) key;
            }
        }
        // If not found, create a new one and add it to the tree
        HeaderManager headerManager = createHeaderManager(name, token);
        threadGroupTree.add(headerManager);
        return headerManager;
    }

    /**
     * Adds a header to an existing HeaderManager without overwriting existing headers.
     *
     * @param headerManager The HeaderManager to add the header to
     * @param name         Header name
     * @param value        Header value
     */
    protected void addHeader(HeaderManager headerManager, String name, String value) {
        headerManager.add(new Header(name, value));
    }

    /**
     * Adds multiple headers to an existing HeaderManager.
     *
     * @param headerManager The HeaderManager to add headers to
     * @param headers      Map of header names to values
     */
    protected void addHeaders(HeaderManager headerManager, java.util.Map<String, String> headers) {
        if (headers != null) {
            headers.forEach((name, value) -> headerManager.add(new Header(name, value)));
        }
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
        String logScript = "log.info('URL: ' + prev.getUrlAsString() + ' | Sample: ' + prev.getSampleLabel() + ' | Code: ' + prev.getResponseCode() + ' | Response: ' + prev.getResponseDataAsString());";
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

        LoopController loopController = TestPlanFactory.createLoopController(loops);
        ThreadGroup threadGroup = TestPlanFactory.createThreadGroup(name, threads, rampUp, loopController);

        HashTree threadGroupTree = testPlanTree.add(testPlan, threadGroup);
        attachStandardComponents(threadGroupTree, name);

        return new TestContext(testPlanTree, threadGroupTree);
    }

    /**
     * Executes the assembled Global Suite.
     */
    protected void runSuite() {
        log.info("Executing Global Test Suite...");
        ListedHashTree globalTree = GlobalSuiteContext.getInstance().getTestPlanTree();
        String suiteName = GlobalSuiteContext.getInstance().getTestPlan().getName();
        runner.runTest(globalTree, suiteName);
        GlobalSuiteContext.getInstance().clear();
    }

    private void attachStandardComponents(HashTree threadGroupTree, String name) {
        String token = TestConfiguration.getProperty("target.auth.token");
        // Use getOrCreateHeaderManager to avoid overwriting existing headers
        getOrCreateHeaderManager(threadGroupTree, name + " Headers", token);

        if (isResponseLoggingEnabled()) {
            JSR223PostProcessor logProcessor = createResponseLogger(name + " Logger");
            threadGroupTree.add(logProcessor);
        }
    }

    private boolean isResponseLoggingEnabled() {
        return Boolean.parseBoolean(TestConfiguration.getProperty("response.logging.enabled", "false"));
    }
}