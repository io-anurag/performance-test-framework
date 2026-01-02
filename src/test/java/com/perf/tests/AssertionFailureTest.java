package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test to verify that assertion failures properly fail the JUnit test.
 */
class AssertionFailureTest extends BasePerformanceTest {

    @Test
    void testAssertionFailureShouldFailTest() {
        // This test should FAIL because we're asserting for response code 999 which
        // won't happen
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            startSuite("Assertion Failure Test");

            TestContext ctx = createSuiteThreadGroup("Test TG", 1, 1, 1);
            HashTree txn = addTransactionController(ctx.getThreadGroupTree(), "Failing Transaction", true);

            // Create a sampler that will succeed (200) but we'll assert for 999
            HTTPSamplerProxy sampler = TestPlanFactory.createHttpSampler(
                    "GET /users",
                    "gorest.co.in",
                    "/public/v2/users",
                    "GET");
            HashTree samplerTree = txn.add(sampler);

            // This assertion will FAIL - expecting 999 but will get 200
            addResponseCodeAssertion(samplerTree, "999");

            runSuite();
        });

        // Verify the exception message
        log.info("✅ Test correctly failed with: {}", exception.getMessage());
        assert exception.getMessage().contains("Test execution contained failures");
    }

    @Test
    void testSuccessfulAssertionShouldPass() {
        // This test should PASS because assertion matches actual response
        startSuite("Successful Assertion Test");

        TestContext ctx = createSuiteThreadGroup("Test TG", 1, 1, 1);
        HashTree txn = addTransactionController(ctx.getThreadGroupTree(), "Passing Transaction", true);

        HTTPSamplerProxy sampler = TestPlanFactory.createHttpSampler(
                "GET /users",
                "gorest.co.in",
                "/public/v2/users",
                "GET");
        HashTree samplerTree = txn.add(sampler);

        // This assertion will PASS - expecting 301 (the API redirects)
        addResponseCodeAssertion(samplerTree, "301");

        runSuite();

        log.info("✅ Test passed as expected!");
    }
}
