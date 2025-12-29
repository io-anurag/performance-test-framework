package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import org.apache.jmeter.control.LoopController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Demonstrates reporting scenarios for ExtentReports integration:
 * passing, failing, sampler-level failure, and skipped tests.
 *
 * <p>Each scenario exercises the BasePerformanceTest helpers and verifies
 * reporting behavior end-to-end.</p>
 */
class ReportingScenariosTest extends BasePerformanceTest {

    /**
     * Should pass and appear as PASSED in the report.
     */
    @Test
    void testPassingScenario() {
        runSimpleTest("Pass Test", "example.com", "/");
    }

    /**
     * Intentionally fails to showcase FAILED reporting.
     */
    @Test
    void testFailingScenario() {
        runSimpleTest("Fail Test", "example.com", "/");
        Assertions.fail("This test is designed to fail to demonstrate reporting capabilities.");
    }

    /**
     * Triggers a sampler failure (404) to demonstrate request-level errors.
     */
    @Test
    void testJMeterSamplerFailure() {
        // Hitting a non-existent page to generate a 404
        runSimpleTest("Sampler Fail Test", "example.com", "/non-existent-page-for-404");
    }

    /**
     * Explicitly skipped test to produce a SKIPPED entry in the report.
     */
    @Test
    @Disabled("This test is skipped to demonstrate reporting capabilities")
    void testSkippedScenario() {
        runSimpleTest("Skipped Test", "example.com", "/");
    }

    /**
     * Builds and runs a minimal HTTP test plan for the given target.
     *
     * @param planName name used for the JMeter TestPlan and reporting
     * @param domain   target hostname (e.g., example.com)
     * @param path     request path (e.g., "/")
     */
    private void runSimpleTest(String planName, String domain, String path) {
        TestPlan testPlan = createTestPlan(planName);
        LoopController loopController = createLoopController(1);
        ThreadGroup threadGroup = createThreadGroup(1, 1, loopController);
        HTTPSamplerProxy httpSampler = createHttpSampler("Request " + domain + path, domain, 80, path, "GET");

        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);
        HashTree threadGroupHashTree = testPlanTree.add(testPlan, threadGroup);
        threadGroupHashTree.add(httpSampler);

        runTest(testPlanTree, planName);
    }
}
