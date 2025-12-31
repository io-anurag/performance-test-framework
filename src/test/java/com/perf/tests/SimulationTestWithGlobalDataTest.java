package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Realistic performance test scenarios simulating various load patterns
 * against public test APIs (httpbin.org).
 */
@Tag("performance")
class SimulationTestWithGlobalDataTest extends BasePerformanceTest {

    /**
     * Parameterized simulation for various load profiles using Global
     * Configuration.
     * Arguments provided via CSV Source: Description, Endpoint Path
     */
    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "Standard Load Test (Global) - httpbin.org/get, /get",
            "Spike Test (Global) - httpbin.org/ip, /ip",
            "Latency Test (Global) - httpbin.org/delay/2, /delay/2",
            "Stress Test (Global) - httpbin.org/bytes/1024, /bytes/1024"
    })
    void testGlobalSimulation(String planName, String path) {
        String targetDomain = getProperty("target.domain", "httpbin.org");
        runHttpTest(planName, targetDomain, path, "GET", null, null, null);
    }
}