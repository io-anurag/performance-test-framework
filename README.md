# JMeter Java Performance Testing Framework

A robust, code-first performance testing framework that enables you to define, configure, and execute Apache JMeter test plans programmatically using Java and JUnit 5.

## 📚 Documentation

Detailed documentation is available in the `docs/` directory:

- 📖 **[Step-by-Step Test Creation Guide](docs/test_creation_guide.md)**: Start here! A complete tutorial for writing your first test.
- 🏗️ **[Architecture Overview](docs/architecture.md)**: Deep dive into the framework's design, components, and execution flow.

---

## 🚀 Key Features

- **Code-First Testing:** Write performance tests as standard Java code (JUnit 5) without needing the JMeter GUI.
- **Dynamic Configuration:** Fully configurable via `config.properties` with support for global headers, environment switching, and overrides.
- **Thread-Safe Context:** Built on `GlobalSuiteContext` to support parallel execution and complex thread group hierarchies.
- **Rich Reporting:** Integrated **ExtentReports** generation with detailed breakdown of requests, assertions, and errors.
- **Powerful Assertions:** Built-in helpers for response code and duration assertions (SLA enforcement).
- **Dynamic Payload Support:** Load request bodies from external files and extract values (e.g., JSONPath) for correlation (Chaining requests).
- **Clean Logging:** Automatic log rotation and JTL result generation.

---

## 🛠️ Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Apache JMeter 5.6.3** (Managed automatically via Maven dependencies)
- **IDE:** IntelliJ IDEA or Eclipse (Recommended)

---

## 📂 Project Structure

```
jmeter-java-framework/
├── docs/                                    # Detailed documentation
│   ├── architecture.md                      # Framework architecture design
│   └── test_creation_guide.md               # Step-by-step usage guide
├── src/
│   ├── main/
│   │   ├── java/com/perf/framework/
│   │   │   ├── BasePerformanceTest.java         # Base class for all tests
│   │   │   ├── GlobalSuiteContext.java          # Thread-safe test context
│   │   │   ├── JMeterDriver.java                # Engine wrapper
│   │   │   ├── TestConfiguration.java           # Config reader
│   │   │   └── TestPlanFactory.java             # Factory for JMeter elements
│   │   └── resources/
│   │       ├── config.properties                # Main configuration file
│   │       └── logback.xml                      # Logging settings
│   └── test/
│       ├── java/com/perf/tests/
│       │   ├── GoRestAPITest.java               # 🏆 Flagship example (CRUD Workflow)
│   │   │   ├── AssertionFailureTest.java        # Demo of assertion handling
│   │   │   └── HierarchyDemoTest.java           # Demo of complex hierarchies
│       └── resources/
│           └── payloads/                        # JSON request bodies (e.g. create_user.json)
├── logs/                                    # Execution logs & JTL results
├── report/                                  # Generated HTML reports
└── pom.xml
```

---

## ⚙️ Configuration

The framework is driven by `src/main/resources/config.properties`.

### Core Settings

```properties
# Test Plan Identity
test.plan.name=Performance Test Plan

# Target API (Environment specific)
target.protocol=https
target.domain=gorest.co.in
target.path=/public/v2

# Global Headers (Applied to ALL requests)
# Configure tokens here to apply them to every request automatically
global.header.Authorization=Bearer Your_Token_Here
global.header.Content-Type=application/json
global.header.Accept=application/json
```

### Load Profile (Default Defaults)

These can be overridden per test method.

```properties
thread.count=5          # Virtual Users
loop.count=1            # Iterations
ramp.up=2               # Ramp-up time (seconds)
```

### Reporting

```properties
report.path=report/performance-tests/test-report.html
report.title=Performance Test Report
report.theme=STANDARD   # STANDARD or DARK
jtl.path=logs/test_result.jtl
```

---

## 🏃 Running Tests

You can run tests using standard Maven commands.

### Run All Tests

```bash
mvn clean test
```

### Run a Specific Test Class

```bash
mvn test -Dtest=GoRestAPITest
```

### Run with Custom Parameters (Override Config)

You can override any property from the command line:

```bash
mvn test -Dtest=GoRestAPITest -Dthread.count=50 -Dtarget.domain=staging-api.com
```

---

## 📝 Writing Tests

Tests extend `BasePerformanceTest` and rely on `TestPlanFactory`.

### Example: User Lifecycle Test

*(Simplified from `GoRestAPITest.java`)*

```java
@ExtendWith(ExtentReportListener.class)
class MyApiTest extends BasePerformanceTest {

    @Test
    void testUserWorkflow() {
        startSuite("User Workflow Suite");

        // 1. Define Load Profile
        TestContext ctx = createSuiteThreadGroup("API Users", 10, 5, 2);
        HashTree threadGroup = ctx.getThreadGroupTree();

        // 2. Add Transaction Controller
        HashTree txn = addTransactionController(threadGroup, "Create User Flow", true);

        // 3. Create Request (POST)
        HTTPSamplerProxy createUser = TestPlanFactory.createHttpSampler(
            "POST Create User", 
            "gorest.co.in", 
            "/public/v2/users", 
            "POST"
        );
        
        // 4. Add Payload & Assertions
        // (See GoRestAPITest.java for full payload handling example)
        HashTree createTree = txn.add(createUser);
        addResponseCodeAssertion(createTree, "201");
        
        // 5. Extract ID for next steps
        TestPlanFactory.createJsonExtractor("Extract ID", "userId", "$.id", "0");

        // 6. Run
        runSuite();
    }
}
```

See [Test Creation Guide](docs/test_creation_guide.md) for full details on:

- Adding Payloads (JSON files)
- Dynamic Data Extraction (JSONPath)
- Transaction Controllers
- Assertions

---

## 📊 Reporting

### HTML Report

Located at `report/performance-tests/test-report.html`.

- **Summary Dashboard:** Pass/Fail rates, Execution time.
- **Detailed Logs:** Request/Response details for failures.
- **Statistics:** Min/Max/Avg response times.

### JTL Results

Located at `logs/test_result.jtl`.

- Standard JMeter CSV format.
- Can be imported into the JMeter GUI for advanced analysis.

---

## 🤝 Contributing

1. Fork the repo.
2. Create a feature branch.
3. Commit changes.
4. Push and create a Pull Request.

---

## 📄 License

MIT License.
