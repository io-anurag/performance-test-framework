# GitHub Copilot Instructions for JMeter Java Performance Testing Framework

This document provides architectural context, development standards, patterns, and conventions for AI coding assistants working in the `performance-test-framework` repository.

---

## 1. Project Overview & Philosophy

- **Repository:** `performance-test-framework`
- **Core Technology:** Java 17+, Apache JMeter 5.6.3, JUnit 5 (Jupiter), Maven 3.6+, ExtentReports 5.1.1.
- **Purpose:** A pure **code-first, programmatic performance testing framework**. It completely replaces the JMeter GUI with standard Java test classes, providing version-controlled, modular, refactorable performance test suites that run directly from IDEs (IntelliJ IDEA, VS Code, Eclipse) and CI/CD pipelines.
- **Key Concepts:**
  - **No GUI Required:** Tests are plain Java classes written with JUnit 5.
  - **Programmatic JMeter Trees:** JMeter elements (`TestPlan`, `ThreadGroup`, `HTTPSamplerProxy`, `TransactionController`, `ResponseAssertion`, `JSONPostProcessor`) are constructed via `TestPlanFactory` and assembled into JMeter's `ListedHashTree`.
  - **Thread-Safe Context:** `GlobalSuiteContext` encapsulates test plans within `ThreadLocal` storage, enabling parallel test execution.
  - **Integrated Reporting & Graphing:** Automatic ExtentReports HTML generation (`report/performance-tests/test-report.html`) and JTL analysis graphs (`report/graphs/*.png`) via `jmeter-graph-tool-maven-plugin`.

---

## 2. Technology Stack & Key Dependencies

| Component | Library / Tool | Version | Purpose |
|---|---|---|---|
| **Language** | Java | 17 | Core programming language |
| **Test Runner** | JUnit 5 Jupiter | 5.10.0 | Test lifecycle, runner, annotations (`@Test`, `@DisplayName`, `@ExtendWith`) |
| **Engine** | Apache JMeter (`core`, `http`, `java`, `functions`) | 5.6.3 | Core load testing engine executed headlessly |
| **Reporting** | ExtentReports | 5.1.1 | Modern interactive HTML test execution reports |
| **Graphing** | `jmeter-graph-tool-maven-plugin` | 1.2 | Generates PNG latency, TPS, percentile graphs during Maven `verify` |
| **Logging** | SLF4J + Logback Classic | 1.4.14 | Test framework logging |
| **Build Tool** | Apache Maven | 3.6+ | Build, dependency management, execution lifecycle |

---

## 3. Directory Structure & Key Components

```
performance-test-framework/
├── .github/
│   └── copilot-instructions.md              # This file
├── docs/
│   ├── architecture.md                      # In-depth architectural design document
│   └── test_creation_guide.md               # Step-by-step test authoring tutorial
├── pom.xml                                  # Maven POM (dependencies, plugins, verify phase graphs)
├── logs/
│   └── test_result.jtl                      # Raw JMeter sample execution results
├── report/
│   ├── performance-tests/
│   │   └── test-report.html                 # ExtentReports dashboard
│   └── graphs/                              # Generated PNG performance graphs and CSV summary
├── src/
│   ├── main/
│   │   ├── java/com/perf/framework/
│   │   │   ├── GlobalSuiteContext.java      # ThreadLocal holder for TestPlan and HashTree
│   │   │   ├── JMeterDriver.java            # Configures headless JMeter environment & engine
│   │   │   ├── TestConfiguration.java       # Config loader (config.properties + CLI overrides)
│   │   │   └── TestPlanFactory.java         # Static factory for all JMeter test elements
│   │   └── resources/
│   │       ├── config.properties            # Central test environment & assertion configuration
│   │       └── logback.xml                  # Logback logging configuration
│   └── test/
│       ├── java/com/perf/
│       │   ├── framework/
│       │   │   ├── BasePerformanceTest.java # Mandatory base class for all tests
│       │   │   ├── PerformanceTestRunner.java # Runner coordinating JMeter execution
│       │   │   └── TestContext.java         # Wrapper around HashTree & ThreadGroup
│       │   ├── reporting/
│       │   │   ├── ExtentReportListener.java # JUnit 5 lifecycle listener
│       │   │   ├── ExtentReportJMeterListener.java # JMeter sample result collector for Extent
│       │   │   ├── HtmlTemplates.java       # HTML formatting helpers for report logs
│       │   │   └── SystemUtils.java         # System metrics utility
│       │   └── tests/                       # Concrete test implementations
│       │       ├── GoRestAPITest.java       # Reference CRUD workflow with payloads & correlation
│       │       ├── AssertionFailureTest.java # Assertion error handling demonstration
│       │       └── HierarchyDemoTest.java   # Full JMeter hierarchy demonstration
│       └── resources/
│           ├── junit-platform.properties    # JUnit 5 configuration
│           └── payloads/                    # Reusable JSON request body files
│               ├── create_user.json
│               └── update_user.json
```

---

## 4. Test Class Architecture & Authoring Rules

Every performance test class **must** comply with these mandatory rules:

### 4.1. Class Definition Requirements
1. **Location:** Place test classes under `src/test/java/com/perf/tests/`.
2. **Inheritance:** **Must extend** `com.perf.framework.BasePerformanceTest`.
3. **Annotation:** **Must be annotated with** `@ExtendWith(ExtentReportListener.class)`.
4. **Naming:** Must follow standard test naming conventions: `<Feature>Test.java` (e.g., `UserLifecycleTest.java`).

### 4.2. Standard Test Method Structure (The 7-Step Pattern)

Always follow this exact structure when generating or modifying tests:

```java
package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import com.perf.framework.TestConfiguration;
import com.perf.framework.TestContext;
import com.perf.framework.TestPlanFactory;
import com.perf.reporting.ExtentReportListener;
import org.apache.jmeter.extractor.json.jsonpath.JSONPostProcessor;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.HashTree;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ExtentReportListener.class)
class UserLifecycleTest extends BasePerformanceTest {

    @Test
    @DisplayName("User Lifecycle Performance Test")
    void testUserLifecycle() {
        // Step 1: Read runtime configurations (never hardcode hosts or credentials)
        String protocol = TestConfiguration.getProperty("target.protocol", "https");
        String domain = TestConfiguration.getProperty("target.domain", "api.example.com");
        String basePath = TestConfiguration.getProperty("target.path", "/api/v1");
        int threads = TestConfiguration.getIntProperty("thread.count", 5);
        int rampUp = TestConfiguration.getIntProperty("ramp.up", 2);
        int loops = TestConfiguration.getIntProperty("loop.count", 1);

        // Step 2: Initialize Suite (sets up TestPlan and automatically attaches global headers)
        startSuite("User Lifecycle Performance Suite");

        // Step 3: Create Thread Group (load profile)
        TestContext ctx = createSuiteThreadGroup("API Users", threads, rampUp, loops);
        HashTree threadGroupTree = ctx.getThreadGroupTree();

        // Step 4: Add Transaction Controller for logical grouping
        HashTree createFlowTxn = addTransactionController(threadGroupTree, "Create User Flow", true);

        // Step 5: Create and attach HTTP Sampler
        HTTPSamplerProxy createUser = TestPlanFactory.createHttpSamplerWithPayloadFile(
                "POST /users",
                protocol,
                domain,
                basePath + "/users",
                "POST",
                "create_user.json"
        );
        HashTree createUserTree = createFlowTxn.add(createUser);

        // Step 6: Attach Assertions and Extractors to the specific sampler tree
        addResponseCodeAssertion(createUserTree, "201");
        addDurationAssertionFromConfig(createUserTree, "Create User SLA");

        JSONPostProcessor idExtractor = TestPlanFactory.createJsonExtractor(
                "Extract User ID",
                "created_user_id",
                "$.id",
                "NOT_FOUND"
        );
        createUserTree.add(idExtractor);

        // Step 7: Execute the assembled test plan
        runSuite();
    }
}
```

---

## 5. JMeter Element Assembly & HashTree Hierarchy

In JMeter, the tree hierarchy dictates scoping. Always attach elements to the correct node level:

```
ListedHashTree (Root)
└── TestPlan (Created by startSuite())
    ├── HeaderManager (Global headers auto-attached by startSuite())
    └── ThreadGroup (Created by createSuiteThreadGroup())
        ├── TransactionController (Optional, created by addTransactionController())
        │   └── HTTPSamplerProxy (Created by TestPlanFactory.createHttpSampler*)
        │       ├── ResponseAssertion (addResponseCodeAssertion(samplerTree, ...))
        │       ├── DurationAssertion (addDurationAssertion(samplerTree, ...))
        │       ├── JSONPostProcessor (samplerTree.add(extractor))
        │       └── JSR223PostProcessor (Response logger, if enabled in config)
        └── SimpleController (Optional, created by addSimpleController())
            └── HTTPSamplerProxy
```

### Hierarchy Rules:
- **`startSuite(String name)`**: Initializes `GlobalSuiteContext` with a new `TestPlan` and attaches `Global Headers` loaded from `config.properties`.
- **`createSuiteThreadGroup(name, threads, rampUp, loops)`**: Returns a `TestContext` whose `getThreadGroupTree()` represents the ThreadGroup branch.
- **Attaching Samplers:**
  - Under Thread Group directly: `HashTree samplerTree = threadGroupTree.add(sampler);`
  - Under Transaction Controller: `HashTree samplerTree = txnTree.add(sampler);`
- **Scoping Assertions & Extractors:**
  - Always add assertions and post-processors to the `samplerTree` returned when adding the sampler.
  - **Do NOT** add request-specific assertions to `threadGroupTree` unless you want the assertion to validate every single sampler in the thread group.
- **`runSuite()`**: **Mandatory.** Triggers execution via `PerformanceTestRunner`, runs JMeter's engine, aggregates listener metrics, and clears `GlobalSuiteContext`.

---

## 6. Factory Methods (`TestPlanFactory`) Cheat Sheet

Use `TestPlanFactory` static methods rather than constructing JMeter objects directly:

### Samplers
- `TestPlanFactory.createHttpSampler(name, domain, path, method)`: Default protocol from config.
- `TestPlanFactory.createHttpSampler(name, protocol, domain, path, method)`: Protocol override.
- `TestPlanFactory.createHttpSampler(name, protocol, domain, port, path, method)`: Full parameter control (`port = null` or `-1` uses protocol default).
- `TestPlanFactory.createHttpSamplerWithBody(name, protocol, domain, path, method, bodyContent)`: For POST/PUT/PATCH with raw JSON string.
- `TestPlanFactory.createHttpSamplerWithPayloadFile(name, protocol, domain, path, method, payloadFile)`: Reads JSON body from `src/test/resources/payloads/<payloadFile>`.

### Controllers
- `TestPlanFactory.createTransactionController(name, generateParentSample)`: Groups requests into a composite transaction. If `generateParentSample = true`, aggregated timing is reported.
- `TestPlanFactory.createSimpleController(name)`: Basic sequential grouping without synthetic parent metrics.
- `BasePerformanceTest` helper shortcuts: `addTransactionController(...)`, `addSimpleController(...)`.

### Assertions & Post-Processors
- `addResponseCodeAssertion(samplerTree, "200", "201")`: Checks HTTP status code matches any listed code.
- `addResponseCodeAssertionFromConfig(samplerTree)`: Validates against `assertion.expected.response.codes` in `config.properties`.
- `addDurationAssertion(samplerTree, maxDurationMs)`: Enforces SLA response time threshold.
- `addDurationAssertion(samplerTree, name, maxDurationMs)`: Named duration assertion.
- `addDurationAssertionFromConfig(samplerTree, name)`: Validates against `assertion.max.duration.ms` in `config.properties`.
- `TestPlanFactory.createJsonExtractor(name, variableName, jsonPath, defaultValue)`: Extracts JSONPath value from response into a JMeter variable.
- `TestPlanFactory.createResponseLogger(name)`: Groovy JSR223 post-processor logging URL, status, and payload.

---

## 7. Dynamic Payloads, JMeter Functions & Correlation

### 7.1. Storing Payloads
- Place JSON payload files in `src/test/resources/payloads/`.
- Load them with `TestPlanFactory.createHttpSamplerWithPayloadFile(...)`.

### 7.2. Using Built-in JMeter Functions in Payloads
JMeter functions inside JSON payload files are evaluated dynamically per thread and loop iteration:
- **Unique Random Numbers:** `${__Random(10000, 99999,)}`
- **Current Timestamp:** `${__time(,)}` or `${__time(yyyy-MM-dd'T'HH:mm:ss'Z',)}`
- **Thread Number:** `${__threadNum}`
- **UUID:** `${__UUID()}`

*Example (`src/test/resources/payloads/create_user.json`):*
```json
{
  "name": "User_${__Random(10000,99999,)}",
  "gender": "male",
  "email": "user${__threadNum}${__Random(1000,9999,)}${__time(,)}@example.com",
  "status": "active"
}
```

### 7.3. Chaining & Correlation (Extract and Reuse)
To pass data between requests (e.g., extracting an `id` to update or delete):
1. **Extract:**
   ```java
   JSONPostProcessor idExtractor = TestPlanFactory.createJsonExtractor(
       "Extract User ID",
       "created_user_id",
       "$.id",
       "NOT_FOUND"
   );
   createSamplerTree.add(idExtractor);
   ```
2. **Reference in Later Requests:**
   Use JMeter syntax `${<variable_name>}` in URLs, headers, or payloads:
   ```java
   HTTPSamplerProxy deleteUser = TestPlanFactory.createHttpSampler(
       "DELETE /users/:id",
       protocol,
       domain,
       basePath + "/users/${created_user_id}",
       "DELETE"
   );
   ```

---

## 8. Configuration (`config.properties`) & CLI Overrides

Settings are defined in `src/main/resources/config.properties` and read using `TestConfiguration`:

```properties
# Target Environment
target.protocol=https
target.domain=gorest.co.in
target.path=/public/v2

# Global Headers (applied automatically at TestPlan level by startSuite())
global.header.Authorization=Bearer YOUR_ACCESS_TOKEN_HERE
global.header.Content-Type=application/json
global.header.Accept=application/json

# Load Profile Defaults
thread.count=5
loop.count=1
ramp.up=2

# Assertions
assertion.expected.response.codes=200,201,202
assertion.max.duration.ms=3000

# Reporting & Logging
report.path=report/performance-tests/test-report.html
report.title=Performance Test Report
report.theme=STANDARD
jtl.path=logs/test_result.jtl
response.logging.enabled=false
```

### CLI Overrides
Any property can be overridden on the command line via `-Dkey=value`. `TestConfiguration` prioritizes System properties over `config.properties`.

---

## 9. Common Maven Execution Commands

```bash
# 1. Run all tests without generating graphs (fastest)
mvn clean test

# 2. Run a single test class
mvn test -Dtest=GoRestAPITest

# 3. Run all tests AND generate PNG performance graphs (verify phase)
mvn clean verify

# 4. Run a single test AND generate PNG performance graphs
mvn clean verify -Dtest=GoRestAPITest

# 5. Run test with dynamic load override from command line
mvn clean verify -Dtest=GoRestAPITest -Dthread.count=50 -Dramp.up=10 -Dloop.count=5

# 6. Override target environment & authentication via CLI
mvn clean test -Dtarget.domain=staging-api.example.com -Dglobal.header.Authorization="Bearer token123"
```

---

## 10. Best Practices and Common Pitfalls

### ✅ DOs:
- **Always call `startSuite(...)`** at the beginning of each `@Test` method.
- **Always call `runSuite()`** at the end of each `@Test` method to trigger execution and cleanup.
- **Use `TestConfiguration`** for all URLs, paths, tokens, and load settings.
- **Name all components clearly** (`HTTPSamplerProxy`, `TransactionController`, assertions) because these exact names appear in ExtentReports and JMeter graphs.
- **Attach assertions and extractors to the specific sampler's tree** (`samplerTree.add(...)`).
- **Store request bodies in `src/test/resources/payloads/`** to keep test classes clean and readable.
- **Use `generateParentSample = true`** in `addTransactionController` when measuring aggregate workflow SLAs.

### ❌ DON'Ts:
- **NEVER instantiate `StandardJMeterEngine` manually** inside test classes. Use `runSuite()`.
- **NEVER hardcode API keys or secrets** in test classes; use `config.properties` or `-Dglobal.header.Authorization`.
- **DO NOT create samplers with protocols in the domain string** (e.g., use `"gorest.co.in"`, not `"https://gorest.co.in"`).
- **DO NOT forget `runSuite()`** — omitting it means the test passes instantly without actually executing any JMeter traffic.
- **DO NOT modify `GlobalSuiteContext` directly** unless implementing framework-level infrastructure.
