package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.junit.jupiter.api.Test;

/**
 * Sample performance test demonstrating HTTP request to a configured target.
 */
class SampleTest extends BasePerformanceTest {

    @Test
    void runJMeterTest() {
        log.info("Reading configuration...");

        // Read test parameters from config
        String testPlanName = getProperty("test.plan.name", "Java_Test_Plan");
        String domain = getProperty("target.domain");
        int port = getIntProperty("target.port", 80);
        String path = getProperty("target.path", "/");
        String method = getProperty("target.method", "GET");
        int threads = getIntProperty("thread.count", 1);
        int loops = getIntProperty("loop.count", 1);
        int rampUp = getIntProperty("ramp.up", 1);

        log.info("Configuration: Domain={}, Port={}, Threads={}, Loops={}", domain, port, threads, loops);

        // Build the test plan using helper methods
        TestPlan testPlan = createTestPlan(testPlanName);
        LoopController loopController = createLoopController(loops);
        ThreadGroup threadGroup = createThreadGroup(threads, rampUp, loopController);
        HTTPSamplerProxy httpSampler = createHttpSampler("Request " + domain, domain, port, path, method);

        // Assemble the test plan tree
        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);
        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(httpSampler);

        // Execute the test
        runTest(testPlanTree, testPlanName);
    }
}
