# JMeter Java Performance Testing Framework

A robust, configuration-driven performance testing framework that allows you to run Apache JMeter tests programmatically using Java and JUnit 5.

## Features

- **Programmatic JMeter Test Creation**: Build and execute JMeter test plans entirely in Java code
- **JUnit 5 Integration**: Run performance tests as standard JUnit tests with IDE support
- **ExtentReports Integration**: Beautiful, interactive HTML reports with detailed test execution information
- **Configuration-Driven**: Externalize test parameters via properties files for easy environment switching
- **Base Test Class Architecture**: Reusable base class with helper methods for rapid test development
- **Test Lifecycle Hooks**: Execute custom commands before/after test execution
- **Automatic Report Generation**: Reports are generated automatically after test execution
- **Theme Support**: Standard and Dark theme options for reports
- **Comprehensive Logging**: Logback-based logging with file output to `logs/` directory
- **Clean Log Management**: Log files are automatically overwritten on each test run for clean results

## Prerequisites

- **Java 17+**
- **Maven 3.6+**
- **Apache JMeter 5.6.3** (managed via Maven dependencies)

## Project Structure

```
jmeter-java-framework/
├── src/
│   ├── main/
│   │   ├── java/com/perf/framework/
│   │   │   ├── JMeterDriver.java           # Core JMeter execution engine
│   │   │   ├── JMeterFrameworkException.java
│   │   │   └── TestConfiguration.java      # Properties reader utility
│   │   └── resources/
│   │       ├── config.properties           # Test configuration
│   │       └── logback.xml                 # Logging configuration
│   └── test/
│       ├── java/com/perf/
│       │   ├── framework/
│       │   │   ├── BasePerformanceTest.java      # Abstract base class for tests
│       │   │   └── ExtentReportListener.java     # Report generation listener
│       │   └── tests/
│       │       └── SampleTest.java               # Example performance test
├── logs/                                    # Test execution logs (auto-created)
│   ├── test-execution.log                  # Framework and JMeter logs
│   └── test_result.jtl                     # JMeter test results (CSV)
├── pom.xml
└── README.md
```

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd jmeter-java-framework
```

### 2. Build the Project

```bash
mvn clean compile
```

### 3. Run Sample Test

```bash
mvn test
```

### 4. View the Report

After running tests, open the generated report:
- **Location**: `report/performance-tests/test-report.html`
- Simply double-click the file to open in your browser (no server required)

## Configuration

Edit `src/main/resources/config.properties` to customize your tests:

### Test Configuration
```properties
# Target Application
target.domain=google.com
target.port=80
target.path=/
target.method=GET

# Load Parameters
thread.count=1          # Number of virtual users
loop.count=1            # Iterations per user
ramp.up=1              # Ramp-up time in seconds
```

### Report Configuration
```properties
report.path=report/performance-tests/test-report.html
report.title=Performance Test Report
report.name=Performance Test Execution
report.theme=STANDARD   # Options: STANDARD, DARK
```

### Test Hooks
```properties
post.test.command=echo "Tests finished!"
```

## Writing Tests

### Basic Test Structure

Tests extend `BasePerformanceTest` which provides helper methods and automatic reporting:

```java
package com.perf.tests;

import com.perf.framework.BasePerformanceTest;
import org.apache.jmeter.testelement.TestPlan;
import org.apache.jmeter.threads.ThreadGroup;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy;
import org.apache.jorphan.collections.ListedHashTree;
import org.junit.jupiter.api.Test;

class MyPerformanceTest extends BasePerformanceTest {

    @Test
    void testApiEndpoint() {
        // Read configuration
        String testPlanName = getProperty("test.plan.name", "API Test");
        String domain = getProperty("target.domain");
        int threads = getIntProperty("thread.count", 10);
        
        // Build test plan using helper methods
        TestPlan testPlan = createTestPlan(testPlanName);
        LoopController loopController = createLoopController(5);
        ThreadGroup threadGroup = createThreadGroup(threads, 10, loopController);
        HTTPSamplerProxy httpSampler = createHttpSampler(
            "API Request", 
            domain, 
            443, 
            "/api/endpoint", 
            "POST"
        );
        
        // Assemble test plan tree
        ListedHashTree testPlanTree = new ListedHashTree();
        testPlanTree.add(testPlan);
        testPlanTree.add(testPlan, threadGroup).add(httpSampler);
        
        // Execute test (with automatic error handling)
        runTest(testPlanTree, testPlanName);
    }
}
```

### Available Helper Methods

The `BasePerformanceTest` class provides:

| Method | Description |
|--------|-------------|
| `createTestPlan(name)` | Creates a JMeter TestPlan |
| `createLoopController(loops)` | Creates a loop controller |
| `createThreadGroup(threads, rampUp, controller)` | Creates a thread group |
| `createHttpSampler(name, domain, port, path, method)` | Creates an HTTP sampler |
| `runTest(testPlanTree, testName)` | Executes test with error handling |
| `getProperty(key, default)` | Reads string property from config |
| `getIntProperty(key, default)` | Reads integer property from config |

## Running Tests

### Run All Tests
```bash
mvn clean test
```

### Run Specific Test
```bash
mvn test -Dtest=SampleTest
```

### With Custom Configuration
You can override properties via system properties:
```bash
mvn test -Dtarget.domain=example.com -Dthread.count=50
```

## Reports

### ExtentReports

The framework automatically generates ExtentReports after test execution:

- **Location**: Configured via `report.path` in `config.properties`
- **Default**: `report/performance-tests/test-report.html`
- **Features**:
  - Test execution summary
  - Pass/Fail status with details
  - System information
  - Execution timeline
  - Theme customization (Standard/Dark)

### JMeter Results

Raw JMeter results are saved to:
- **Location**: `logs/test_result.jtl` (CSV format)
- This file contains detailed sampler results and can be imported into JMeter GUI for visualization

## Logging

### Log Files

The framework uses Logback for comprehensive logging:

- **Framework Logs**: `logs/test-execution.log`
  - Contains all framework and JMeter execution logs
  - Includes timestamps, thread information, and log levels
  - **Automatically overwritten** on each test run for clean results
  
- **JMeter Results**: `logs/test_result.jtl`
  - CSV format with detailed test execution metrics
  - Can be imported into JMeter GUI for analysis
  - **Automatically overwritten** on each test run

### Log Configuration

Logging is configured in `src/main/resources/logback.xml`:

```xml
<configuration>
    <!-- Console output -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- File output (overwrites on each run) -->
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/test-execution.log</file>
        <append>false</append>  <!-- Overwrite mode -->
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

### Customizing Logs

To modify logging behavior:

1. **Change log level**: Edit `<root level="INFO">` to `DEBUG`, `WARN`, or `ERROR`
2. **Change log file location**: Modify `<file>logs/test-execution.log</file>`
3. **Enable append mode**: Change `<append>false</append>` to `<append>true</append>`
4. **Customize format**: Edit the `<pattern>` element

## Advanced Features

### Custom Test Hooks

Execute commands after test completion by configuring `post.test.command`:

```properties
post.test.command=./scripts/notify-team.sh
```

The hook is executed automatically by the `ExtentReportListener`.

### Adding More Samplers

You can add other JMeter samplers (JDBC, FTP, etc.) by creating helper methods in your test classes or extending `BasePerformanceTest`.

### CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Performance Tests

on: [push]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Run Tests
        run: mvn clean test
      - name: Upload Report
        uses: actions/upload-artifact@v2
        with:
          name: extent-report
          path: report/performance-tests/
```

## Troubleshooting

### Tests Not Running
- Ensure JUnit dependencies are in `pom.xml`
- Verify test classes are in `src/test/java`
- Check that test methods are annotated with `@Test`

### Report Not Generated
- Check `report.path` in `config.properties`
- Ensure the directory exists or can be created
- Verify `ExtentReportListener` is registered on test class

### JMeter Initialization Errors
- The framework automatically handles JMeter home directory creation and property initialization
- JMeter properties are loaded before the engine is created to prevent initialization warnings
- Ensure all JMeter dependencies are downloaded by Maven

### Missing Log Files
- Log files are created in the `logs/` directory automatically
- If the directory doesn't exist, it will be created on first test run
- Check file permissions if logs aren't being written

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Apache JMeter - https://jmeter.apache.org/
- ExtentReports - https://www.extentreports.com/
- JUnit 5 - https://junit.org/junit5/

## Contact

For questions or support, please open an issue in the repository.
