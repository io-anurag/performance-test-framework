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
     * @param port     target port
     * @param path     request path
     * @param method   HTTP method (e.g., GET, POST)
     * @param threads  number of virtual users
     * @param loops    iterations per thread
     * @param rampUp   ramp-up time in seconds
     */
    public record SimpleTestPlanConfig(
            String planName,
            String domain,
            int port,
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
        ThreadGroup threadGroup = createThreadGroup(config.threads(), config.rampUp(), loopController);
        HTTPSamplerProxy httpSampler = createHttpSampler(config.domain(), config.port(), config.path(), config.method());

        return buildTestPlanTree(testPlan, threadGroup, httpSampler);
    }

    private static TestPlan createTestPlan(String planName) {
        return new TestPlan(planName);
    }

    private static LoopController createLoopController(int loops) {
        LoopController loopController = new LoopController();
        loopController.setLoops(loops);
        loopController.setFirst(true);
        loopController.initialize();
        return loopController;
    }

    private static ThreadGroup createThreadGroup(int threads, int rampUp, LoopController loopController) {
        ThreadGroup threadGroup = new ThreadGroup();
        threadGroup.setName("Thread Group");
        threadGroup.setNumThreads(threads);
        threadGroup.setRampUp(rampUp);
        threadGroup.setSamplerController(loopController);
        return threadGroup;
    }

    private static HTTPSamplerProxy createHttpSampler(String domain, int port, String path, String method) {
        HTTPSamplerProxy httpSampler = new HTTPSamplerProxy();
        httpSampler.setDomain(domain);
        httpSampler.setPort(port);
        httpSampler.setPath(path);
        httpSampler.setMethod(method);
        httpSampler.setName("Request " + domain + path);
        httpSampler.setProperty(TestElement.TEST_CLASS, HTTPSamplerProxy.class.getName());
        httpSampler.setProperty(TestElement.GUI_CLASS, HttpTestSampleGui.class.getName());
        return httpSampler;
    }

    private static HashTree buildTestPlanTree(TestPlan testPlan, ThreadGroup threadGroup, HTTPSamplerProxy httpSampler) {
        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);
        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(httpSampler);
        return testPlanTree;
    }
}
