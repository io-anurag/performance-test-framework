package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.Test;

/**
 * Demonstration test showcasing the complete JMeter hierarchy:
 * TestPlan → ThreadGroup → TransactionController → Sampler → Assertion
 * 
 * All components are properly named for tracking in reports and logs.
 */
class HierarchyDemoTest extends BasePerformanceTest {

    @Test
    void testFullJMeterHierarchy() {
        // Start a new test suite (creates the TestPlan)
        startSuite("Full Hierarchy Demo Suite");

        // Create Thread Group 1: User Operations
        TestContext userOpsContext = createSuiteThreadGroup("User Operations Group", 2, 1, 1);
        HashTree userOpsTree = userOpsContext.getThreadGroupTree();

        // Transaction 1: Login Flow
        HashTree loginTransaction = addTransactionController(userOpsTree, "Login Transaction", true);

        HTTPSamplerProxy loginSampler = TestPlanFactory.createHttpSampler(
                "POST /login",
                "api.example.com",
                "/api/v1/auth/login",
                "POST");
        HashTree loginSamplerTree = loginTransaction.add(loginSampler);
        addResponseCodeAssertion(loginSamplerTree, "200");
        addDurationAssertion(loginSamplerTree, "Login Duration Check", 2000);

        // Transaction 2: Get User Profile
        HashTree profileTransaction = addTransactionController(userOpsTree, "Get Profile Transaction", true);

        HTTPSamplerProxy profileSampler = TestPlanFactory.createHttpSampler(
                "GET /profile",
                "api.example.com",
                "/api/v1/user/profile",
                "GET");
        HashTree profileSamplerTree = profileTransaction.add(profileSampler);
        addResponseCodeAssertion(profileSamplerTree, "200", "201");
        addDurationAssertion(profileSamplerTree, 1500);

        // Create Thread Group 2: Admin Operations
        TestContext adminOpsContext = createSuiteThreadGroup("Admin Operations Group", 1, 1, 1);
        HashTree adminOpsTree = adminOpsContext.getThreadGroupTree();

        // SimpleController: Admin Dashboard Checks
        HashTree dashboardController = addSimpleController(adminOpsTree, "Dashboard Controller");

        HTTPSamplerProxy dashboardSampler = TestPlanFactory.createHttpSampler(
                "GET /admin/dashboard",
                "api.example.com",
                "/api/v1/admin/dashboard",
                "GET");
        HashTree dashboardSamplerTree = dashboardController.add(dashboardSampler);
        addResponseCodeAssertion(dashboardSamplerTree, "200");

        HTTPSamplerProxy statsSampler = TestPlanFactory.createHttpSampler(
                "GET /admin/stats",
                "api.example.com",
                "/api/v1/admin/stats",
                "GET");
        HashTree statsSamplerTree = dashboardController.add(statsSampler);
        addResponseCodeAssertion(statsSamplerTree, "200");
        addDurationAssertion(statsSamplerTree, "Stats Query Duration", 3000);

        // Execute the complete hierarchy
        runSuite();

        log.info("✅ Full hierarchy test completed successfully!");
        log.info("Check report for:");
        log.info("  - Test Plan: 'Full Hierarchy Demo Suite'");
        log.info("  - Thread Groups: 'User Operations Group', 'Admin Operations Group'");
        log.info("  - Transactions: 'Login Transaction', 'Get Profile Transaction'");
        log.info("  - Controllers: 'Dashboard Controller'");
        log.info("  - Samplers with proper names and assertions");
    }

    @Test
    void testSimpleHierarchyWithRealAPI() {
        // Test with actual API endpoint from config
        startSuite("Simple API Test");

        TestContext ctx = createSuiteThreadGroup("API Users Thread Group", 1, 1, 1);
        HashTree threadTree = ctx.getThreadGroupTree();

        // Transaction for user operations
        HashTree transaction = addTransactionController(threadTree, "Get Users Transaction", true);

        // Create sampler using config properties
        HTTPSamplerProxy sampler = TestPlanFactory.createHttpSampler(
                "GET /users",
                "gorest.co.in",
                "/public/v2/users",
                "GET");
        HashTree samplerTree = transaction.add(sampler);

        // Add assertions
        addResponseCodeAssertion(samplerTree, "200");
        addDurationAssertion(samplerTree, 5000); // 5 second max

        runSuite();

        log.info("✅ Simple API test with hierarchy completed!");
    }
}
