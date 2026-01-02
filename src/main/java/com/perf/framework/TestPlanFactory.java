package com.perf.framework;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.control.TransactionController;
import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.control.gui.TransactionControllerGui;
import org.apache.jmeter.control.gui.LogicControllerGui;
import org.apache.jmeter.assertions.ResponseAssertion;
import org.apache.jmeter.assertions.DurationAssertion;
import org.apache.jmeter.assertions.gui.AssertionGui;
import org.apache.jmeter.assertions.gui.DurationAssertionGui;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
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
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.extractor.json.jsonpath.gui.JSONPostProcessorGui;

import com.perf.exceptions.TestExecutionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Factory utilities to build common JMeter test plans programmatically.
 *
 * <p>
 * Provides a simple HTTP plan builder that assembles a minimal TestPlan,
 * ThreadGroup,
 * LoopController, and HTTPSampler into a HashTree suitable for execution.
 * </p>
 */
public class TestPlanFactory {

    /**
     * Immutable configuration for a basic HTTP test plan.
     *
     * @param planName name for the JMeter TestPlan
     * @param domain   target host (without protocol)
     * @param port     target port (null to use default)
     * @param path     request path
     * @param method   HTTP method (e.g., GET, POST)
     * @param threads  number of virtual users
     * @param loops    iterations per thread
     * @param rampUp   ramp-up time in seconds
     */
    public record SimpleTestPlanConfig(
            String planName,
            String domain,
            Integer port,
            String path,
            String method,
            int threads,
            int loops,
            int rampUp) {
    }

    /**
     * Builds a minimal HTTP test plan tree from the provided configuration.
     *
     * <p>
     * The returned {@link HashTree} contains: TestPlan →
     * ThreadGroup(LoopController) → HTTPSampler.
     * </p>
     *
     * @param config simple HTTP plan configuration
     * @return an assembled test plan tree ready for execution
     */
    public static HashTree createSimpleHttpPlan(SimpleTestPlanConfig config) {
        TestPlan testPlan = createTestPlan(config.planName());
        LoopController loopController = createLoopController(config.loops());
        String threadGroupName = config.planName() + " Thread Group";
        ThreadGroup threadGroup = createThreadGroup(threadGroupName, config.threads(), config.rampUp(), loopController);
        HTTPSamplerProxy httpSampler = createHttpSampler("Request " + config.domain() + config.path(),
                config.domain(), config.port(), config.path(), config.method());

        return buildTestPlanTree(testPlan, threadGroup, httpSampler);
    }

    /**
     * Create a TestPlan with the given name.
     *
     * @param planName name for the plan root element
     * @return a new TestPlan instance
     */
    public static TestPlan createTestPlan(String planName) {
        return new TestPlan(planName);
    }

    /**
     * Create and initialize a LoopController for the desired iteration count.
     *
     * <p>
     * Initialization is explicitly invoked to ensure internal state is prepared
     * before attaching to a ThreadGroup.
     * </p>
     *
     * @param loops number of iterations per thread
     * @return an initialized LoopController
     */
    public static LoopController createLoopController(int loops) {
        LoopController loopController = new LoopController();
        loopController.setLoops(loops);
        loopController.setFirst(true);
        loopController.initialize();
        return loopController;
    }

    /**
     * Build a ThreadGroup wired to the provided LoopController.
     *
     * <p>
     * Sets thread count and ramp-up to control concurrency and arrival rate.
     * </p>
     *
     * @param threads        number of virtual users
     * @param rampUp         ramp-up time in seconds
     * @param loopController controller managing per-thread iterations
     * @return configured ThreadGroup
     */
    public static ThreadGroup createThreadGroup(int threads, int rampUp, LoopController loopController) {
        return createThreadGroup("Thread Group", threads, rampUp, loopController);
    }

    /**
     * Build a ThreadGroup with a custom name wired to the provided LoopController.
     *
     * <p>
     * Use this overload when you need a descriptive name to appear in reports
     * and listeners. Sets thread count and ramp-up to control concurrency
     * and arrival rate.
     * </p>
     *
     * @param name           name for the thread group
     * @param threads        number of virtual users
     * @param rampUp         ramp-up time in seconds
     * @param loopController controller managing per-thread iterations
     * @return configured ThreadGroup
     */
    public static ThreadGroup createThreadGroup(String name, int threads, int rampUp, LoopController loopController) {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName(name);
        threadGroup.setNumThreads(threads);
        threadGroup.setRampUp(rampUp);
        threadGroup.setSamplerController(loopController);
        return threadGroup;
    }

    /**
     * Create an HTTP sampler for the provided target and method.
     * Uses default protocol from config.properties (target.protocol).
     *
     * <p>
     * Also sets TEST_CLASS and GUI_CLASS to maintain compatibility with JMeter's
     * serialization and GUI metadata expectations.
     * </p>
     *
     * @param samplerName Name for the sampler
     * @param domain      target host (without protocol)
     * @param path        request path
     * @param method      HTTP method (e.g., GET, POST)
     * @return configured HTTPSamplerProxy
     */
    public static HTTPSamplerProxy createHttpSampler(String samplerName, String domain, String path, String method) {
        return createHttpSampler(samplerName, null, domain, (Integer) null, path, method);
    }

    /**
     * Create an HTTP sampler with explicit protocol override.
     * If protocol is null, uses default from config.properties.
     *
     * @param samplerName Name for the sampler
     * @param protocol    Protocol override (http/https), null to use config default
     * @param domain      target host (without protocol)
     * @param path        request path
     * @param method      HTTP method (e.g., GET, POST)
     * @return configured HTTPSamplerProxy
     */
    public static HTTPSamplerProxy createHttpSampler(String samplerName, String protocol, String domain, String path,
            String method) {
        return createHttpSampler(samplerName, protocol, domain, (Integer) null, path, method);
    }

    /**
     * Create an HTTP sampler with port specification.
     * Uses default protocol from config.properties.
     *
     * @param samplerName Name for the sampler
     * @param domain      target host (without protocol)
     * @param port        target port (null for default)
     * @param path        request path
     * @param method      HTTP method (e.g., GET, POST)
     * @return configured HTTPSamplerProxy
     */
    public static HTTPSamplerProxy createHttpSampler(String samplerName, String domain, Integer port, String path,
            String method) {
        return createHttpSampler(samplerName, null, domain, port, path, method);
    }

    /**
     * Create an HTTP sampler with full control over all parameters.
     * This is the master implementation that all other overloads delegate to.
     *
     * @param samplerName Name for the sampler
     * @param protocol    Protocol override (http/https), null to use config default
     * @param domain      target host (without protocol), null to use config default
     * @param port        target port (null for default)
     * @param path        request path
     * @param method      HTTP method (e.g., GET, POST)
     * @return configured HTTPSamplerProxy
     */
    public static HTTPSamplerProxy createHttpSampler(String samplerName, String protocol, String domain, Integer port,
            String path, String method) {
        String effectiveProtocol = (protocol != null) ? protocol : TestConfiguration.getProperty("target.protocol");
        String effectiveDomain = (domain != null) ? domain : TestConfiguration.getProperty("target.domain");

        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setProtocol(effectiveProtocol);
        httpSampler.setDomain(effectiveDomain);
        int resolvedPort = (port == null) ? -1 : port;
        httpSampler.setPort(resolvedPort);
        httpSampler.setPath(path);
        httpSampler.setMethod(method);
        httpSampler.setName(samplerName);
        httpSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        return httpSampler;
    }

    /**
     * Create an HTTP sampler with request body content.
     * This method is ideal for POST, PUT, PATCH requests that require a request body.
     * The body content supports JMeter functions and variables for dynamic data generation.
     *
     * @param samplerName Name for the sampler
     * @param protocol    Protocol (http/https), null to use config default
     * @param domain      target host (without protocol), null to use config default
     * @param path        request path
     * @param method      HTTP method (POST, PUT, PATCH)
     * @param bodyContent Request body content (typically JSON)
     * @return Configured HTTPSamplerProxy with body data
     */
    public static HTTPSamplerProxy createHttpSamplerWithBody(String samplerName, String protocol, String domain, String path,
            String method, String bodyContent) {
        HTTPSamplerProxy sampler = createHttpSampler(samplerName, protocol, domain, path, method);
        HTTPArgument httpArg = new HTTPArgument();
        httpArg.setAlwaysEncoded(false);
        httpArg.setUseEquals(false);
        httpArg.setValue(bodyContent);
        
        Arguments args = new Arguments();
        args.addArgument(httpArg);
        sampler.setArguments(args);
        
        return sampler;
    }

    /**
     * Create an HTTP sampler with request body loaded from a file.
     * The payload files support JMeter functions for dynamic data generation.
     *
     * @param samplerName Name for the sampler
     * @param protocol    Protocol (http/https), null to use config default
     * @param domain      target host (without protocol), null to use config default
     * @param path        request path
     * @param method      HTTP method (POST, PUT, PATCH)
     * @param payloadFile Filename in src/test/resources/payloads/ directory
     * @return Configured HTTPSamplerProxy with body data from file
     * @throws TestExecutionException if payload file cannot be read
     */
    public static HTTPSamplerProxy createHttpSamplerWithPayloadFile(String samplerName, String protocol, String domain, String path,
            String method, String payloadFile) {
        try {
            String payload = new String(Files.readAllBytes(Paths.get("src/test/resources/payloads/" + payloadFile)));
            return createHttpSamplerWithBody(samplerName, protocol, domain, path, method, payload);
        } catch (IOException e) {
            throw new TestExecutionException("Failed to read payload file: " + payloadFile,  e.getCause());
        }
    }

    /**
     * Assemble the JMeter test plan tree from its core components.
     *
     * <p>
     * Structure: {@code TestPlan → ThreadGroup → HTTPSampler}.
     * </p>
     *
     * @param testPlan    root plan element
     * @param threadGroup group controlling thread execution
     * @param httpSampler sampler to execute
     * @return a HashTree representing the full plan
     */
    private static HashTree buildTestPlanTree(TestPlan testPlan, ThreadGroup threadGroup,
            HTTPSamplerProxy httpSampler) {
        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);
        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(httpSampler);
        return testPlanTree;
    }

    /**
     * Adds a header to an existing HeaderManager without overwriting existing
     * headers.
     *
     * @param headerManager The HeaderManager to add the header to
     * @param name          Header name
     * @param value         Header value
     */
    public static void addHeader(HeaderManager headerManager, String name, String value) {
        headerManager.add(new Header(name, value));
    }

    /**
     * Adds multiple headers to an existing HeaderManager.
     *
     * @param headerManager The HeaderManager to add headers to
     * @param headers       Map of header names to values
     */
    public static void addHeaders(HeaderManager headerManager, java.util.Map<String, String> headers) {
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
    public static JSR223PostProcessor createResponseLogger(String name) {
        JSR223PostProcessor logProcessor = new JSR223PostProcessor();
        logProcessor.setName(name);
        logProcessor.setProperty("scriptLanguage", "groovy");
        String logScript = "log.info('URL: ' + prev.getUrlAsString() + ' | Sample: ' + prev.getSampleLabel() + ' | Code: ' + prev.getResponseCode() + ' | Response: ' + prev.getResponseDataAsString());";
        logProcessor.setProperty("script", logScript);
        configureTestElement(logProcessor, JSR223PostProcessor.class, TestBeanGUI.class);
        return logProcessor;
    }

    /**
     * Creates a JSONPostProcessor to extract values from JSON responses using
     * JSONPath.
     * The extracted value will be stored in a JMeter variable for use in subsequent
     * requests.
     *
     * @param name         Name of the extractor
     * @param variableName Name of the JMeter variable to store the extracted value
     * @param jsonPath     JSONPath expression (e.g., "$.id" or "$.data.userId")
     * @param defaultValue Default value if extraction fails (use "NOT_FOUND" or
     *                     empty string)
     * @return Configured JSONPostProcessor
     */
    public static JSONPostProcessor createJsonExtractor(String name, String variableName, String jsonPath, String defaultValue) {
        JSONPostProcessor extractor = new JSONPostProcessor();
        extractor.setName(name);
        extractor.setRefNames(variableName);
        extractor.setJsonPathExpressions(jsonPath);
        extractor.setDefaultValues(defaultValue);
        extractor.setMatchNumbers("1"); // Extract first match
        configureTestElement(extractor, JSONPostProcessor.class, JSONPostProcessorGui.class);
        return extractor;
    }

    /**
     * Set JMeter serialization and GUI metadata on a TestElement.
     *
     * <p>
     * Populates TEST_CLASS and GUI_CLASS properties to ensure elements can be
     * serialized correctly and displayed in JMeter GUIs when needed.
     * </p>
     *
     * @param element   the element to configure
     * @param testClass class that implements the test element behavior
     * @param guiClass  corresponding GUI class for the element
     */
    public static void configureTestElement(TestElement element, Class<?> testClass, Class<?> guiClass) {
        element.setProperty(TestElement.TEST_CLASS, testClass.getName());
        element.setProperty(TestElement.GUI_CLASS, guiClass.getName());
    }

    /**
     * Creates a TransactionController for grouping samplers into logical
     * transactions.
     * This is useful for measuring end-to-end transaction times in reports.
     *
     * @param name                 Name of the transaction (will appear in reports)
     * @param generateParentSample If true, generates a parent sample for cumulative
     *                             transaction metrics
     * @return Configured TransactionController
     */
    public static TransactionController createTransactionController(String name, boolean generateParentSample) {
        TransactionController controller = new TransactionController();
        controller.setName(name);
        controller.setGenerateParentSample(generateParentSample);
        configureTestElement(controller, TransactionController.class, TransactionControllerGui.class);
        return controller;
    }

    /**
     * Creates a GenericController for basic sequential execution of child samplers.
     * Use this for simple grouping without transaction timing.
     *
     * @param name Name of the controller
     * @return Configured GenericController
     */
    public static GenericController createSimpleController(String name) {
        GenericController controller = new GenericController();
        controller.setName(name);
        configureTestElement(controller, GenericController.class, LogicControllerGui.class);
        return controller;
    }

    /**
     * Creates a ResponseAssertion to validate HTTP response codes.
     *
     * @param name          Name of the assertion
     * @param expectedCodes Expected response codes (e.g., "200", "201", "204")
     * @return Configured ResponseAssertion
     */
    public static ResponseAssertion createResponseCodeAssertion(String name, String... expectedCodes) {
        ResponseAssertion assertion = new ResponseAssertion();
        assertion.setName(name);
        assertion.setTestFieldResponseCode();
        assertion.setToContainsType();

        for (String code : expectedCodes) {
            assertion.addTestString(code);
        }

        configureTestElement(assertion, ResponseAssertion.class, AssertionGui.class);
        return assertion;
    }

    /**
     * Creates a flexible ResponseAssertion with configurable test field and match
     * type.
     *
     * @param name     Name of the assertion
     * @param patterns Patterns to test against
     * @return Configured ResponseAssertion
     */
    public static ResponseAssertion createResponseAssertion(String name, String... patterns) {
        ResponseAssertion assertion = new ResponseAssertion();
        assertion.setName(name);
        assertion.setTestFieldResponseCode();
        assertion.setToContainsType();

        for (String pattern : patterns) {
            assertion.addTestString(pattern);
        }

        configureTestElement(assertion, ResponseAssertion.class, AssertionGui.class);
        return assertion;
    }

    /**
     * Creates a DurationAssertion to validate response time thresholds.
     *
     * @param name          Name of the assertion
     * @param maxDurationMs Maximum allowed duration in milliseconds
     * @return Configured DurationAssertion
     */
    public static DurationAssertion createDurationAssertion(String name, long maxDurationMs) {
        DurationAssertion assertion = new DurationAssertion();
        assertion.setName(name);
        assertion.setAllowedDuration(maxDurationMs);
        configureTestElement(assertion, DurationAssertion.class, DurationAssertionGui.class);
        return assertion;
    }

    /**
     * Creates a global HeaderManager that loads headers from config.properties.
     * Headers should be defined with prefix "global.header." (e.g.,
     * global.header.Authorization).
     *
     * @param name Name of the header manager
     * @return Configured HeaderManager with global headers
     */
    public static HeaderManager createGlobalHeaderManager(String name) {
        HeaderManager headerManager = new HeaderManager();
        headerManager.setName(name);

        String authToken = TestConfiguration.getProperty("global.header.Authorization");
        if (authToken != null && !authToken.isEmpty()) {
            headerManager.add(new Header("Authorization", authToken));
        }

        String contentType = TestConfiguration.getProperty("global.header.Content-Type", "application/json");
        headerManager.add(new Header("Content-Type", contentType));

        String accept = TestConfiguration.getProperty("global.header.Accept", "application/json");
        headerManager.add(new Header("Accept", accept));

        configureTestElement(headerManager, HeaderManager.class, HeaderPanel.class);
        return headerManager;
    }
}
