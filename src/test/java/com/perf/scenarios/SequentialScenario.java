package com.perf.scenarios;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;

public class SequentialScenario {

    public static void apply(TestContext context, BasePerformanceTest test, String domain) {
        HashTree threadGroupTree = context.getThreadGroupTree();

        // 1. Home Page (Simulate with /html)
        HTTPSamplerProxy home = TestPlanFactory.createHttpSampler("Home Page", domain, "/html", "GET");
        threadGroupTree.add(home);

        // 2. Login Page (Simulate with /basic-auth/user/passwd)
        // Note: This passes because we simply request the URL, even if auth fails it's
        // not a 404 (it's 401),
        // or we use /status/200 to fake a login page load.
        HTTPSamplerProxy login = TestPlanFactory.createHttpSampler("Login Page (Fake)", domain, "/status/200", "GET");
        threadGroupTree.add(login);
    }
}
