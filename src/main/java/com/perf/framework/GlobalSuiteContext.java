package com.perf.framework;

import org.apache.jmeter.testelement.TestPlan;
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

    /**
     * Get the thread-local GlobalSuiteContext for the current thread.
     *
     * @return the context instance bound to the current thread
     */
    public static GlobalSuiteContext getInstance() {
        return instance.get();
    }

    /**
     * Initialize this thread's context with a new JMeter TestPlan and plan tree.
     * Subsequent calls replace any previously initialized state for this thread.
     *
     * @param suiteName name to assign to the JMeter TestPlan
     */
    public void initialize(String suiteName) {
        this.testPlan = new TestPlan(suiteName);
        this.testPlanTree = new ListedHashTree();
        this.testPlanTree.add(testPlan);
    }

    /**
     * Get the current JMeter TestPlan.
     *
     * @return the TestPlan, or null if initialize(String) has not been called
     */
    public TestPlan getTestPlan() {
        return testPlan;
    }

    /**
     * Get the JMeter test plan tree associated with the current TestPlan.
     *
     * @return the test plan tree, or null if initialize(String) has not been called
     */
    public ListedHashTree getTestPlanTree() {
        return testPlanTree;
    }

    /**
     * Remove and clear the context bound to the current thread.
     * Call at the end of a suite to avoid leaking thread-local state.
     */
    public void clear() {
        instance.remove();
    }
}
