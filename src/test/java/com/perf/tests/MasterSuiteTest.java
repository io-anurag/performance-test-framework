package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MasterSuiteTest extends BasePerformanceTest {

    @Test
    @DisplayName("Global Master Suite")
    void testMasterSuite() {
        startSuite("Master Performance Suite");

        // Scenario 1: Browsing Users
        TestContext browseCtx = createSuiteThreadGroup("Browsing Users", 5, 2, 2);
        com.perf.scenarios.SequentialScenario.apply(browseCtx, this, "httpbin.org");

        // Scenario 2: Random Actions Users
        TestContext randomCtx = createSuiteThreadGroup("Random Users", 5, 2, 2);
        com.perf.scenarios.RandomScenario.apply(randomCtx, this, "httpbin.org");

        runSuite();
    }
}
