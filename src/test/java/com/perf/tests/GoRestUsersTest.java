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

class GoRestUsersTest extends BasePerformanceTest {

    @Test
    @DisplayName("GoRest User Lifecycle Test")
    void testUserLifecycle() throws IOException {
        startSuite("GoRest User Suite");

        TestContext ctx = createSuiteThreadGroup("GoRest Users", 1, 1, 1);
        String domain = TestConfiguration.getProperty("target.domain");
        String path = TestConfiguration.getProperty("target.path");

        // 1. Create User
        HTTPSamplerProxy createRequest = TestPlanFactory.createHttpSampler("Create User", domain, path, "POST");

        String payloadTemplate = loadPayload("create_user.json");
        // Let JMeter generate a unique email using its built-in UUID function
        String dynamicEmail = "${__UUID}@example.com";
        String payload = payloadTemplate.replace("test_user_placeholder@example.com", dynamicEmail);

        createRequest.setPostBodyRaw(true);
        createRequest.getArguments().addArgument(new HTTPArgument("", payload, "", false));

        ctx.getThreadGroupTree().add(createRequest);

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
