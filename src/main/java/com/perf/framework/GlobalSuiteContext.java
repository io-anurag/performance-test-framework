package com.perf.framework;

import org.apache.jmeter.testelement.TestPlan;
import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;

/**
 * Thread-safe singleton context for managing a global JMeter Test Plan usage
 * across
 * a test suite.
 * Use the ThreadLocal pattern to allow safe parallel execution of test classes.
 */
public class GlobalSuiteContext {

    private static final ThreadLocal<GlobalSuiteContext> instance = ThreadLocal.withInitial(GlobalSuiteContext::new);

    private ListedHashTree testPlanTree;
    private TestPlan testPlan;

    private GlobalSuiteContext() {
        // Private constructor to enforce singleton usage
    }

    public static GlobalSuiteContext getInstance() {
        return instance.get();
    }

    public void initialize(String suiteName) {
        this.testPlan = new TestPlan(suiteName);
        this.testPlanTree = new ListedHashTree();
        this.testPlanTree.add(testPlan);
    }

    public TestPlan getTestPlan() {
        return testPlan;
    }

    public ListedHashTree getTestPlanTree() {
        return testPlanTree;
    }

    public void clear() {
        instance.remove();
    }
}
