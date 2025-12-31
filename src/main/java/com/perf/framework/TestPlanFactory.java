package com.perf.framework;

import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;

/**
 * Factory utilities to build common JMeter test plans programmatically.
 *
 * <p>Provides a simple HTTP plan builder that assembles a minimal TestPlan, ThreadGroup,
 * LoopController, and HTTPSampler into a HashTree suitable for execution.</p>
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
            int rampUp
    ) {}

    /**
     * Builds a minimal HTTP test plan tree from the provided configuration.
     *
     * <p>The returned {@link HashTree} contains: TestPlan → ThreadGroup(LoopController) → HTTPSampler.</p>
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
     * <p>Initialization is explicitly invoked to ensure internal state is prepared
     * before attaching to a ThreadGroup.</p>
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
     * <p>Sets thread count and ramp-up to control concurrency and arrival rate.</p>
     *
     * @param threads       number of virtual users
     * @param rampUp        ramp-up time in seconds
     * @param loopController controller managing per-thread iterations
     * @return configured ThreadGroup
     */
    public static ThreadGroup createThreadGroup(int threads, int rampUp, LoopController loopController) {
        return createThreadGroup("Thread Group", threads, rampUp, loopController);
    }

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
     *
     * <p>Also sets TEST_CLASS and GUI_CLASS to maintain compatibility with JMeter's
     * serialization and GUI metadata expectations.</p>
     *
     * @param domain target host (without protocol)
     * @param port   target port
     * @param path   request path
     * @param method HTTP method (e.g., GET, POST)
     * @return configured HTTPSamplerProxy
     */
    public static HTTPSamplerProxy createHttpSampler(String samplerName, String domain, String path, String method) {
        return createHttpSampler(samplerName, domain, (Integer) null, path, method);
    }

    public static HTTPSamplerProxy createHttpSampler(String samplerName, String domain, Integer port, String path, String method) {
        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setDomain(domain);
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
     * Assemble the JMeter test plan tree from its core components.
     *
     * <p>Structure: {@code TestPlan → ThreadGroup → HTTPSampler}.</p>
     *
     * @param testPlan    root plan element
     * @param threadGroup group controlling thread execution
     * @param httpSampler sampler to execute
     * @return a HashTree representing the full plan
     */
    private static HashTree buildTestPlanTree(TestPlan testPlan, ThreadGroup threadGroup, HTTPSamplerProxy httpSampler) {
        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);
        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(httpSampler);
        return testPlanTree;
    }
}
