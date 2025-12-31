# How to Create New Test Cases

This guide walks you through the process of adding a new performance test to the framework using the **Global Suite Pattern**.

## Prerequisites

1. Target API endpoint is known.
2. Target environment configuration (host, port, auth) is set in `src/main/resources/config.properties`.

## Step 1: Create Test Class

Create a new Java class in `src/test/java/com/perf/tests/`.
Must extend `BasePerformanceTest`.

```java
package com.perf.tests;
import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestContext;
import org.junit.jupiter.api.Test;

class ProductLoadTest extends BasePerformanceTest {
    // ...
}
```

## Step 2: Implement Test Logic using Suite Pattern

Inside your `@Test` method:

### A. Initialize Suite Context

Use `startSuite` to initialize the global context for this test execution.

```java
@Test
void testProductCreationLoad() {
    // 1. Start the suite context
    startSuite("Product Creation Suite");

    // 2. Create a Thread Group (e.g., 5 users, 2s ramp-up, 1 iteration)
    TestContext ctx = createSuiteThreadGroup("Product Users", 5, 2, 1);
    
    // 3. Add Samplers via the context tree
    ctx.getThreadGroupTree().add(
        createHttpSampler("Create Product", "api.example.com", 443, "/v1/products", "POST")
    );

    // 4. (Optional) Add more Thread Groups for mixed scenarios
    
    // 5. Execute
    runSuite();
}
```

## Checklist

- [ ] Class extends `BasePerformanceTest`.
- [ ] `startSuite()` is called first.
- [ ] `createSuiteThreadGroup()` is used for user profiles.
- [ ] `runSuite()` is called at the end.
- [ ] Assertions are added to verify success.
