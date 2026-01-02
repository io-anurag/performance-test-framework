package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import com.perf.framework.TestConfiguration;
import com.perf.framework.TestPlanFactory;
import com.perf.reporting.ExtentReportListener;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
        void testUserLifecycleWorkflow() {
                // Read configuration from config.properties
                String protocol = TestConfiguration.getProperty("target.protocol");
                String domain = TestConfiguration.getProperty("target.domain");
                String basePath = TestConfiguration.getProperty("target.path");
                int threads = TestConfiguration.getIntProperty("thread.count");
                int rampUp = TestConfiguration.getIntProperty("ramp.up");
                int loops = TestConfiguration.getIntProperty("loop.count");                

                // Initialize test suite (automatically attaches global headers from config)
                startSuite("Go Rest User Lifecycle Test");

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
                addDurationAssertionFromConfig(listTree, "List Users Response");

                // Step 2: Create User (POST with dynamic payload)
                HTTPSamplerProxy createUser = TestPlanFactory.createHttpSamplerWithPayloadFile(
                                "POST /users",
                                protocol,
                                domain,
                                basePath + "/users",
                                "POST",
                                "create_user.json");
                HashTree createTree = createFlowTxn.add(createUser);
                addResponseCodeAssertion(createTree, "201");
                addDurationAssertionFromConfig(createTree, "Create User Response");

                // Extract user ID from CREATE response for use in UPDATE and DELETE
                JSONPostProcessor idExtractor = TestPlanFactory
                                .createJsonExtractor(
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

                // Step 3: Update User (PATCH with dynamic payload) Using dynamically extracted user ID from CREATE response
                HTTPSamplerProxy updateUser = TestPlanFactory.createHttpSamplerWithPayloadFile(
                                "PATCH /users/:id",
                                protocol,
                                domain,
                                basePath + "/users/${created_user_id}",
                                "PATCH",
                                "update_user.json");
                HashTree updateTree = managementFlowTxn.add(updateUser);
                addResponseCodeAssertion(updateTree, "200");
                addDurationAssertionFromConfig(updateTree, "Update User Response");

                // Step 4: Delete User (DELETE) Using dynamically extracted user ID from CREATE response
                HTTPSamplerProxy deleteUser = TestPlanFactory.createHttpSampler(
                                "DELETE /users/:id",
                                protocol,
                                domain,
                                basePath + "/users/${created_user_id}",
                                "DELETE");
                HashTree deleteTree = managementFlowTxn.add(deleteUser);
                addResponseCodeAssertion(deleteTree, "204");
                addDurationAssertionFromConfig(deleteTree, "Delete User Response");

                // Execute the complete test plan
                runSuite();
        }
}
