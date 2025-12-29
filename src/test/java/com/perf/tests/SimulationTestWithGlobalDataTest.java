package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Realistic performance test scenarios simulating various load patterns
 * against public test APIs (httpbin.org).
 */
@Tag("performance")
class SimulationTestWithGlobalDataTest extends BasePerformanceTest {

    /**
     * Simulates a standard load test using Global Configuration:
     * Uses threads, loops, and rampUp from config.properties.
     * Target: /get (Standard GET request)
     */
    @Test()
    @DisplayName("Standard Load Test (Global) - httpbin.org/get")
    void testStandardLoadProfileGlobal() {
        String targetDomain = getProperty("target.domain", "httpbin.org");
        int targetPort = getIntProperty("target.port", 80);
        String planName = "Standard Load Test (Global) - httpbin.org/get";

        runHttpTest(planName, targetDomain, targetPort, "/get", "GET", null, null, null);
    }

    /**
     * Simulates a Spike Test using Global Configuration:
     * Uses threads, loops, and rampUp from config.properties.
     * Target: /ip (Returns Origin IP)
     */
    @Test
    @DisplayName("Spike Test (Global) - httpbin.org/ip")
    void testSpikeTrafficProfileGlobal() {
        String targetDomain = getProperty("target.domain", "httpbin.org");
        int targetPort = getIntProperty("target.port", 80);
        String planName = "Spike Test (Global) - httpbin.org/ip";

        runHttpTest(planName, targetDomain, targetPort, "/ip", "GET", null, null, null);
    }


    /**
     * Simulates handling of slow responses (Latency Test) using Global Configuration:
     * Uses threads, loops, and rampUp from config.properties.
     * Target: /delay/2
     */
    @Test
    @DisplayName("Latency Test (Global) - httpbin.org/delay/2")
    void testSlowResponseHandlingGlobal() {
        String targetDomain = getProperty("target.domain", "httpbin.org");
        int targetPort = getIntProperty("target.port", 80);
        String planName = "Latency Test (Global) - httpbin.org/delay/2";

        runHttpTest(planName, targetDomain, targetPort, "/delay/2", "GET", null, null, null);
    }
    /**
     * Simulates a Stress Test (Miniature) using Global Configuration:
     * Uses threads, loops, and rampUp from config.properties.
     * Target: /bytes/1024 (Generates random bytes)
     */
    @DisplayName("Stress Test (Global) - httpbin.org/bytes/1024")
    @Test
    void testStressProfileGlobal() {
        String targetDomain = getProperty("target.domain", "httpbin.org");
        int targetPort = getIntProperty("target.port", 80);
        String planName = "Stress Test (Global) - httpbin.org/bytes/1024";

        runHttpTest(planName, targetDomain, targetPort, "/bytes/1024", "GET", null, null, null);
    }
}