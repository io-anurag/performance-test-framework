package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import com.perf.framework.TestConfiguration;
import com.perf.framework.TestPlanFactory;
import com.perf.reporting.ExtentReportListener;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Comprehensive GoRest API Test Suite demonstrating proper JMeter hierarchy:
 * - Single TestPlan with global headers from config
 * - Single ThreadGroup for user simulation
 * - Two TransactionControllers for logical grouping:
 * 1. User Creation Flow (List + Create)
 * 2. User Management Flow (Update + Delete)
 * - Dynamic payloads loaded from JSON files with timestamp-based values
 * - All configuration read from config.properties
 */
@ExtendWith(ExtentReportListener.class)
class GoRestAPITest extends BasePerformanceTest {

    /**
     * Comprehensive API lifecycle test covering List → Create → Update → Delete.
     * Uses dynamic payloads and proper transaction grouping.
     * Reads all configuration from config.properties.
     */
    @Test
    @DisplayName("GoRest User Lifecycle Test")
    void testUserLifecycleWorkflow() throws IOException {
        // Read configuration from config.properties
        String protocol = TestConfiguration.getProperty("target.protocol");
        String domain = TestConfiguration.getProperty("target.domain");
        String basePath = TestConfiguration.getProperty("target.path");
        int threads = TestConfiguration.getIntProperty("thread.count");
        int rampUp = TestConfiguration.getIntProperty("ramp.up");
        int loops = TestConfiguration.getIntProperty("loop.count");

        // Initialize test suite (automatically attaches global headers from config)
        startSuite("GoRest User Lifecycle Test");

        // Create single thread group using values from config
        TestContext ctx = createSuiteThreadGroup("API Users", threads, rampUp, loops);
        HashTree threadGroupTree = ctx.getThreadGroupTree();

        // ==================== Transaction 1: User Creation Flow ====================
        HashTree createFlowTxn = addTransactionController(
                threadGroupTree,
                "User Creation Flow",
                true);

        // Step 1: List Users (GET)
        HTTPSamplerProxy listUsers = TestPlanFactory.createHttpSampler(
                "GET /users",
                protocol,
                domain,
                basePath + "/users",
                "GET");
        HashTree listTree = createFlowTxn.add(listUsers);
        addResponseCodeAssertion(listTree, "200");
        addDurationAssertion(listTree, "List Users Response", 2000);

        // Step 2: Create User (POST with dynamic payload)
        HTTPSamplerProxy createUser = createHttpSamplerWithPayload(
                "POST /users",
                protocol,
                domain,
                basePath + "/users",
                "POST",
                "create_user.json");
        HashTree createTree = createFlowTxn.add(createUser);
        addResponseCodeAssertion(createTree, "201"); // Created
        addDurationAssertion(createTree, "Create User Response", 2000);

        // Extract user ID from CREATE response for use in UPDATE and DELETE
        org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor idExtractor = TestPlanFactory.createJsonExtractor(
                "Extract User ID",
                "created_user_id",
                "$.id",
                "NOT_FOUND");
        createTree.add(idExtractor);

        // ==================== Transaction 2: User Management Flow ====================
        HashTree managementFlowTxn = addTransactionController(
                threadGroupTree,
                "User Management Flow",
                true);

        // Step 3: Update User (PATCH with dynamic payload)
        // Using dynamically extracted user ID from CREATE response
        HTTPSamplerProxy updateUser = createHttpSamplerWithPayload(
                "PATCH /users/:id",
                protocol,
                domain,
                basePath + "/users/${created_user_id}", // Dynamic ID from extractor
                "PATCH",
                "update_user.json");
        HashTree updateTree = managementFlowTxn.add(updateUser);
        addResponseCodeAssertion(updateTree, "200"); // OK
        addDurationAssertion(updateTree, "Update User Response", 1500);

        // Step 4: Delete User (DELETE)
        // Using dynamically extracted user ID from CREATE response
        HTTPSamplerProxy deleteUser = TestPlanFactory.createHttpSampler(
                "DELETE /users/:id",
                protocol,
                domain,
                basePath + "/users/${created_user_id}", // Dynamic ID from extractor
                "DELETE");
        HashTree deleteTree = managementFlowTxn.add(deleteUser);
        addResponseCodeAssertion(deleteTree, "204"); // No Content
        addDurationAssertion(deleteTree, "Delete User Response", 1500);

        // Execute the complete test plan
        runSuite();
    }

    /**
     * Creates an HTTP sampler with request body loaded from a JSON payload file.
     * The payload files support JMeter functions for dynamic data generation.
     * 
     * @param name        Sampler display name
     * @param protocol    Protocol (http/https)
     * @param domain      Target domain (without protocol)
     * @param path        Request path
     * @param method      HTTP method (POST, PUT, PATCH)
     * @param payloadFile Filename in src/test/resources/payloads/
     * @return Configured HTTPSamplerProxy with body data
     * @throws IOException if payload file cannot be read
     */
    private HTTPSamplerProxy createHttpSamplerWithPayload(
            String name,
            String protocol,
            String domain,
            String path,
            String method,
            String payloadFile) throws IOException {

        HTTPSamplerProxy sampler = TestPlanFactory.createHttpSampler(name, protocol, domain, path, method);

        // Load payload from file
        String payloadPath = "src/test/resources/payloads/" + payloadFile;
        String payload = new String(Files.readAllBytes(Paths.get(payloadPath)));

        // Use HTTPArgument for raw JSON body - this is the correct way to preserve HTTP
        // method
        org.apache.jmeter.protocol.http.util.HTTPArgument httpArg = new org.apache.jmeter.protocol.http.util.HTTPArgument();
        httpArg.setAlwaysEncoded(false);
        httpArg.setUseEquals(false);
        httpArg.setValue(payload);

        org.apache.jmeter.config.Arguments args = new org.apache.jmeter.config.Arguments();
        args.addArgument(httpArg);
        sampler.setArguments(args);

        return sampler;
    }
}
