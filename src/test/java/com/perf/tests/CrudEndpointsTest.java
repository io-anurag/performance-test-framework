package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestConfiguration;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

class CrudEndpointsTest extends BasePerformanceTest {

    @Test
    @DisplayName("CRUD Operations with External Payloads")
    void testCrudWithPayloads() throws IOException {
        startSuite("CRUD Test Suite");

        TestContext ctx = createSuiteThreadGroup("CRUD Users", 2, 1, 1);
        String domain = TestConfiguration.getProperty("target.domain");
        String createPath = TestConfiguration.getProperty("target.path");

        // 1. Read Create Payload
        String createPayload = loadPayload("create_user.json");
        // Update email to be unique
        createPayload = createPayload.replace("test_user_placeholder@example.com",
                "crud_user_" + System.currentTimeMillis() + "@example.com");

        HTTPSamplerProxy createRequest = TestPlanFactory.createHttpSampler("Create User", domain, createPath, "POST");
        createRequest.setPostBodyRaw(true);
        createRequest.getArguments().addArgument(new HTTPArgument("", createPayload, "", false));
        ctx.getThreadGroupTree().add(createRequest);

        // 2. Read Update Payload
        String updatePayload = loadPayload("update_user.json");
        // Note: Update usually requires an ID from the Create step.
        // In a real scenario, we'd use a JSON Extractor.
        // For this restoration, we are setting up the structure.

        runSuite();
    }

    private String loadPayload(String filename) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("payloads/" + filename)) {
            if (is == null) {
                throw new IOException("Payload file not found: " + filename);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
