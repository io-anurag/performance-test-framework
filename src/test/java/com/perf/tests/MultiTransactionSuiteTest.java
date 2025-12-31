package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestConfiguration;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;
import org.apache.jmeter.control.RandomOrderController;
import org.apache.jmeter.control.TransactionController;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MultiTransactionSuiteTest extends BasePerformanceTest {

    @Test
    @DisplayName("Plan with multiple thread groups and transaction controllers")
    void multiThreadGroupMultiTransactionPlan() {
        startSuite("Transaction Controller Plan");

        String domain = TestConfiguration.getProperty("target.domain", "httpbin.org");

        // Thread Group 1: Browse + Checkout flows
        TestContext browseCtx = createSuiteThreadGroup("TG-Browse-Checkout", 3, 2, 2);
        HashTree tg1 = browseCtx.getThreadGroupTree();
        addTransactionWithRandomSamplers(tg1, "Browse Flow", domain,
                new String[][] { {"Home", "/html", "GET"}, {"Catalog", "/bytes/512", "GET"}, {"Item", "/status/200", "GET"} });
        addTransactionWithRandomSamplers(tg1, "Checkout Flow", domain,
                new String[][] { {"Cart", "/get", "GET"}, {"Payment", "/delay/1", "GET"} });

        // Thread Group 2: Account flows
        TestContext accountCtx = createSuiteThreadGroup("TG-Account", 2, 2, 1);
        HashTree tg2 = accountCtx.getThreadGroupTree();
        addTransactionWithRandomSamplers(tg2, "Profile Flow", domain,
                new String[][] { {"Profile", "/status/200", "GET"}, {"Settings", "/bytes/256", "GET"} });
        addTransactionWithRandomSamplers(tg2, "Auth Flow", domain,
                new String[][] { {"Login", "/status/200", "GET"}, {"Logout", "/status/200", "GET"} });

        runSuite();
    }

    private void addTransactionWithRandomSamplers(HashTree threadGroupTree, String txnName, String domain, String[][] samplers) {
        TransactionController txn = new TransactionController();
        txn.setName(txnName);
        txn.setIncludeTimers(false);
        configureTestElement(txn, TransactionController.class, org.apache.jmeter.control.gui.TransactionControllerGui.class);

        HashTree txnTree = threadGroupTree.add(txn);

        RandomOrderController roc = new RandomOrderController();
        roc.setName(txnName + " Randomizer");
        configureTestElement(roc, RandomOrderController.class, org.apache.jmeter.control.gui.RandomOrderControllerGui.class);

        HashTree rocTree = txnTree.add(roc);
        for (String[] samplerDef : samplers) {
            String name = samplerDef[0];
            String path = samplerDef[1];
            String method = samplerDef[2];
            rocTree.add(TestPlanFactory.createHttpSampler(name, domain, path, method));
        }
    }
}
