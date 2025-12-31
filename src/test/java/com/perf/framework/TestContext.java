package com.perf.framework;

import org.apache.jorphan.collections.HashTree;
import org.apache.jorphan.collections.ListedHashTree;

/**
 * Holds the context for a specific test execution scope (e.g., a Thread Group).
 * Contains the reference to the Test Plan tree and the specific Thread Group
 * tree.
 */
public class TestContext {
    private final ListedHashTree testPlanTree;
    private final HashTree threadGroupTree;

    public TestContext(ListedHashTree testPlanTree, HashTree threadGroupTree) {
        this.testPlanTree = testPlanTree;
        this.threadGroupTree = threadGroupTree;
    }

    public ListedHashTree getTestPlanTree() {
        return testPlanTree;
    }

    public HashTree getThreadGroupTree() {
        return threadGroupTree;
    }
}
