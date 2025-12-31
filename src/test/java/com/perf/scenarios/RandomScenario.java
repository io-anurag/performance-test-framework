package com.perf.scenarios;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;
import org.apache.jmeter.control.RandomOrderController;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;

public class RandomScenario {

    public static void apply(TestContext context, BasePerformanceTest test, String domain) {
        // Create Random Order Controller
        RandomOrderController randomController = new RandomOrderController();
        randomController.setName("Random User Actions");
        test.configureTestElement(randomController, RandomOrderController.class,
                org.apache.jmeter.control.gui.RandomOrderControllerGui.class);

        // Add Controller to Thread Group
        HashTree controllerTree = context.getThreadGroupTree().add(randomController);

        // Add Samplers to Controller
        // Use /status/200 to simulate a successful search
        HTTPSamplerProxy search = TestPlanFactory.createHttpSampler("Search (Simulated)", domain, "/status/200", "GET");
        controllerTree.add(search);

        // Use /bytes/500 to simulate a product page load
        HTTPSamplerProxy browse = TestPlanFactory.createHttpSampler("Browse Product", domain, "/bytes/500", "GET");
        controllerTree.add(browse);

        // Use /get to simulate viewing cart
        HTTPSamplerProxy cart = TestPlanFactory.createHttpSampler("View Cart", domain, "/get", "GET");
        controllerTree.add(cart);
    }
}
