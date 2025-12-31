package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Realistic performance test scenarios simulating various load patterns
 * against public test APIs (httpbin.org).
 */
@Tag("performance")
class SimulationTestWithLocalDataTest extends BasePerformanceTest {

    private static final String TARGET_DOMAIN = resolveTargetDomain();

    /**
     * Simulates a standard load test:
     * Moderate concurrency (10 users) ramping up over 5 seconds.
     * Each user performs 5 iterations.
     * Target: /get (Standard GET request)
     */
    @Test()
    @DisplayName("Standard Load Test - httpbin.org/get")
    void testStandardLoadProfile() {
        String planName = "Standard Load Test - httpbin.org/get";

        runSimpleHttpPlan(planName, TARGET_DOMAIN, "/get", "GET", 10, 5, 5);
    }

    /**
     * Simulates a Spike Test:
     * Sudden burst of users (20 users) with very short ramp-up (1 second).
     * Useful for testing system behavior under sudden traffic surges.
     * Target: /ip (Returns Origin IP)
     */
    @Test
    @DisplayName("Spike Test - httpbin.org/ip")
    void testSpikeTrafficProfile() {
        String planName = "Spike Test - httpbin.org/ip";

        runSimpleHttpPlan(planName, TARGET_DOMAIN, "/ip", "GET", 20, 1, 1);
    }

    /**
     * Simulates handling of slow responses (Latency Test):
     * The server delays response by 2 seconds.
     * Verifies that the framework and report correctly capture long durations.
     * Target: /delay/2
     */
    @Test
    @DisplayName("Latency Test - httpbin.org/delay/2")
    void testSlowResponseHandling() {
        String planName = "Latency Test - httpbin.org/delay/2";

        runSimpleHttpPlan(planName, TARGET_DOMAIN, "/delay/2", "GET", 5, 1, 2);
    }

    /**
     * Simulates a Stress Test (Miniature):
     * Higher load to push system limits (scaled down for demo).
     * 15 users performing 10 operations each.
     * Target: /bytes/1024 (Generates random bytes)
     */
    @DisplayName("Stress Test - httpbin.org/bytes/1024")
    @Test
    void testStressProfile() {
        String planName = "Stress Test - httpbin.org/bytes/1024";

        runSimpleHttpPlan(planName, TARGET_DOMAIN, "/bytes/1024", "GET", 15, 10, 5);
    }

    private static String resolveTargetDomain() {
        // Prefer an override provided at runtime; otherwise default to local/httpbin.
        String sys = System.getProperty("target.domain");
        if (sys != null && !sys.isEmpty()) {
            return sys;
        }
        return "httpbin.org";
    }
}