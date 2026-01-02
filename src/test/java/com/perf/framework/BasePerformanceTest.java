package com.perf.framework;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;

import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import com.perf.reporting.ExtentReportListener;
import org.apache.jmeter.extractor.JSR223PostProcessor;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.assertions.DurationAssertion;
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
     * Builds and runs a simple single-sampler HTTP plan using provided or defaulted
     * concurrency settings.
     *
     * @param planName name of the plan (used for root TestPlan and reporting)
     * @param domain   target host (without protocol)
     * @param path     request path
     * @param method   HTTP method (e.g., GET, POST)
     * @param threads  number of virtual users (null to use config default)
     * @param loops    iterations per thread (null to use config default)
     * @param rampUp   ramp-up time in seconds (null to use config default)
     */
    protected void runSimpleHttpPlan(String planName, String domain, String path, String method, Integer threads,
            Integer loops, Integer rampUp) {
        HashTree planTree = buildSimpleHttpPlan(planName, domain, path, method, threads, loops, rampUp);
        runTest(planTree, planName);
    }

    /**
     * Build a minimal TestPlan → ThreadGroup → HTTP Sampler tree for a single
     * request.
     *
     * @param planName name of the plan (used for root TestPlan and reporting)
     * @param domain   target host (without protocol)
     * @param path     request path
     * @param method   HTTP method (e.g., GET, POST)
     * @param threads  number of virtual users (null to use config default)
     * @param loops    iterations per thread (null to use config default)
     * @param rampUp   ramp-up time in seconds (null to use config default)
     * @return HashTree representing the plan ready for execution
     */
    protected HashTree buildSimpleHttpPlan(String planName, String domain, String path, String method, Integer threads,
            Integer loops, Integer rampUp) {
        int finalThreads = (threads != null) ? threads : TestConfiguration.getIntProperty("threads");
        int finalLoops = (loops != null) ? loops : TestConfiguration.getIntProperty("loops");
        int finalRampUp = (rampUp != null) ? rampUp : TestConfiguration.getIntProperty("rampUp");

        TestPlan testPlan = TestPlanFactory.createTestPlan(planName);
        LoopController loopController = TestPlanFactory.createLoopController(finalLoops);
        
        ThreadGroup threadGroup = TestPlanFactory.createThreadGroup(planName + " Thread Group", finalThreads, finalRampUp, loopController);
        HTTPSamplerProxy httpSampler = TestPlanFactory.createHttpSampler("Request " + path, domain, path, method);
        JSR223PostProcessor logProcessor = isResponseLoggingEnabled()
                ? TestPlanFactory.createResponseLogger("Response Logger")
                : null;

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
     * Initialize a new global suite context and attach global headers.
     *
     * <p>
     * Creates a thread-local GlobalSuiteContext with the provided suite name and
     * adds a HeaderManager populated from config.properties so headers apply to
     * all requests in the suite.
     * </p>
     *
     * @param suiteName logical name for the suite (appears in reports)
     */
    protected void startSuite(String suiteName) {
        log.info("Starting new Test Suite: {}", suiteName);
        GlobalSuiteContext globalCtx = GlobalSuiteContext.getInstance();
        globalCtx.initialize(suiteName);

        ListedHashTree testPlanTree = globalCtx.getTestPlanTree();
        TestPlan testPlan = globalCtx.getTestPlan();

        HeaderManager globalHeaders = TestPlanFactory.createGlobalHeaderManager("Global Headers");
        testPlanTree.add(testPlan, globalHeaders);

        log.info("Global headers attached to test plan from config.properties");
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
        if (isResponseLoggingEnabled()) {
            JSR223PostProcessor logProcessor = TestPlanFactory.createResponseLogger(name + " Logger");
            threadGroupTree.add(logProcessor);
        }
    }

    private boolean isResponseLoggingEnabled() {
        return Boolean.parseBoolean(TestConfiguration.getProperty("response.logging.enabled", "false"));
    }

    /**
     * Adds a TransactionController to the parent tree and returns its HashTree for
     * adding child samplers.
     *
     * @param parentTree     The parent HashTree (typically a thread group)
     * @param name           Name of the transaction (appears in reports)
     * @param generateParent If true, generates a parent sample for transaction
     *                       metrics
     * @return HashTree for the controller, to which samplers can be added
     */
    protected HashTree addTransactionController(HashTree parentTree, String name, boolean generateParent) {
        TransactionController controller = TestPlanFactory.createTransactionController(name, generateParent);
        return parentTree.add(controller);
    }

    /**
     * Adds a GenericController (SimpleController) to the parent tree for basic
     * grouping.
     *
     * @param parentTree The parent HashTree (typically a thread group)
     * @param name       Name of the controller
     * @return HashTree for the controller, to which samplers can be added
     */
    protected HashTree addSimpleController(HashTree parentTree, String name) {
        GenericController controller = TestPlanFactory.createSimpleController(name);
        return parentTree.add(controller);
    }

    /**
     * Generic method to add any controller to a parent tree.
     *
     * @param parentTree The parent HashTree
     * @param controller The controller to add
     * @return HashTree for the controller
     */
    protected HashTree addController(HashTree parentTree, org.apache.jmeter.control.GenericController controller) {
        return parentTree.add(controller);
    }

    /**
     * Adds a ResponseCodeAssertion to a sampler to validate HTTP response codes.
     *
     * @param samplerTree   The sampler's HashTree
     * @param expectedCodes Expected response codes (e.g., "200", "201")
     */
    protected void addResponseCodeAssertion(HashTree samplerTree, String... expectedCodes) {
        ResponseAssertion assertion = TestPlanFactory.createResponseCodeAssertion("Response Code Assertion", expectedCodes);
        samplerTree.add(assertion);
    }
    
    /**
     * Adds a ResponseCodeAssertion using default codes from config.properties.
     * Falls back to "200" if not configured.
     *
     * @param samplerTree The sampler's HashTree
     */
    protected void addResponseCodeAssertionFromConfig(HashTree samplerTree) {
        String codesStr = TestConfiguration.getProperty("assertion.expected.response.codes", "200");
        String[] codes = codesStr.split(",");
        for (int i = 0; i < codes.length; i++) {
            codes[i] = codes[i].trim();
        }
        addResponseCodeAssertion(samplerTree, codes);
    }

    /**
     * Adds a DurationAssertion to a sampler to validate response time thresholds.
     *
     * @param samplerTree   The sampler's HashTree
     * @param maxDurationMs Maximum allowed duration in milliseconds
     */
    protected void addDurationAssertion(HashTree samplerTree, long maxDurationMs) {
        DurationAssertion assertion = TestPlanFactory.createDurationAssertion("Duration Assertion", maxDurationMs);
        samplerTree.add(assertion);
    }

    /**
     * Adds a named DurationAssertion to a sampler.
     *
     * @param samplerTree   The sampler's HashTree
     * @param name          Name of the assertion
     * @param maxDurationMs Maximum allowed duration in milliseconds
     */
    protected void addDurationAssertion(HashTree samplerTree, String name, long maxDurationMs) {
        DurationAssertion assertion = TestPlanFactory.createDurationAssertion(name, maxDurationMs);
        samplerTree.add(assertion);
    }
    
    /**
     * Adds a DurationAssertion using default duration from config.properties.
     * Falls back to 3000ms if not configured.
     *
     * @param samplerTree The sampler's HashTree
     * @param name        Name of the assertion
     */
    protected void addDurationAssertionFromConfig(HashTree samplerTree, String name) {
        int maxDuration = TestConfiguration.getIntProperty("assertion.max.duration.ms", 3000);
        addDurationAssertion(samplerTree, name, maxDuration);
    }
    
    /**
     * Adds a DurationAssertion using default duration from config.properties.
     * Falls back to 3000ms if not configured.
     *
     * @param samplerTree The sampler's HashTree
     */
    protected void addDurationAssertionFromConfig(HashTree samplerTree) {
        int maxDuration = TestConfiguration.getIntProperty("assertion.max.duration.ms", 3000);
        addDurationAssertion(samplerTree, maxDuration);
    }
}