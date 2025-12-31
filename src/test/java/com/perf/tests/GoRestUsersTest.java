package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jmeter.protocol.http.util.HTTPArgument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GoRestUsersTest extends BasePerformanceTest {

    @Test
    @DisplayName("GoRest User Lifecycle Test")
    void testUserLifecycle() {
        startSuite("GoRest User Suite");

        TestContext ctx = createSuiteThreadGroup("GoRest Users", 1, 1, 1);
        String domain = getProperty("target.domain", "gorest.co.in");

        // 1. Create User
        HTTPSamplerProxy createRequest = createHttpSampler("Create User", domain, "/public/v2/users", "POST");
        String email = "test_user_" + System.currentTimeMillis() + "@example.com";
        String payload = "{\"name\":\"Test User\", \"gender\":\"male\", \"email\":\"" + email
                + "\", \"status\":\"active\"}";

        createRequest.setPostBodyRaw(true);
        createRequest.getArguments().addArgument(new HTTPArgument("", payload, "", false));

        ctx.getThreadGroupTree().add(createRequest);

        runSuite();
    }
}
