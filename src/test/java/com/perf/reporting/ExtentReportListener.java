package com.perf.reporting;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.perf.framework.TestConfiguration;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * JUnit 5 extension that wires ExtentReports into the test lifecycle.
 *
 * <p>
 * Initializes a single Extent report per run, creates a test node per method,
 * and logs pass/fail/skip outcomes. Report appearance and path can be
 * configured
 * via {@link TestConfiguration} properties (e.g., report.path, report.title,
 * report.name, report.theme).
 * </p>
 * 
 * <p>
 * Thread Safety: Uses ThreadLocal to maintain test context in multi-threaded
 * scenarios,
 * ensuring each test thread has its own ExtentTest instance.
 * </p>
 */
public class ExtentReportListener implements BeforeAllCallback, AfterAllCallback, TestWatcher, BeforeEachCallback {
    protected static final Logger log = LoggerFactory.getLogger(ExtentReportListener.class);
    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> test = new ThreadLocal<>();

    /**
     * Returns the current Extent test node for logging.
     * This method is thread-safe and returns the test instance for the current
     * thread.
     *
     * @return the active {@link ExtentTest}, or null if not yet initialized
     */
    public static ExtentTest getTest() {
        return test.get();
    }

    /**
     * Initializes the ExtentReports instance if not already initialized.
     * This method is idempotent and can be called multiple times safely.
     */
    public static void initReport() {
        if (extent == null) {
            String reportPath = TestConfiguration.getProperty("report.path");
            String reportTitle = TestConfiguration.getProperty("report.title");
            String reportName = TestConfiguration.getProperty("report.name");
            String themeStr = TestConfiguration.getProperty("report.theme");
            Theme theme = "DARK".equalsIgnoreCase(themeStr) ? Theme.DARK : Theme.STANDARD;

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(theme);
            spark.config().setDocumentTitle(reportTitle);
            spark.config().setReportName(reportName);
            spark.config().setCss(HtmlTemplates.CUSTOM_CSS);

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        }
    }

    /**
     * Creates a new test node in the report.
     *
     * @param testName the name of the test to create
     */
    public static void createTest(String testName) {
        if (extent != null) {
            ExtentTest extentTest = extent.createTest(testName);
            test.set(extentTest);
        }
    }

    /**
     * Flushes the report to disk.
     */
    public static void flushReport() {
        if (extent != null) {
            extent.flush();
        }
    }

    /**
     * Ensures a test node exists for the current context.
     * Creates a new test if one doesn't exist in the current thread.
     *
     * @param context JUnit extension context
     */
    private void ensureTestExists(ExtensionContext context) {
        if (test.get() == null) {
            test.set(extent.createTest(context.getDisplayName()));
        }
    }

    /**
     * Executes a post-test command if configured.
     * Logs errors to System.err instead of silently swallowing them.
     *
     * @param command the command to execute
     */
    private void executePostTestCommand(String command) {
        try {
            SystemUtils.executeShellCommand(command);
        } catch (Exception e) {
            log.error("Failed to execute post-test command {} {}", command, e.getMessage());
        }
    }

    /**
     * Initializes the Extent report once per test run using configurable settings.
     *
     * @param context JUnit extension context
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        initReport();
    }

    /**
     * Creates an Extent test node for the current test method.
     *
     * @param context JUnit extension context
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        createTest(context.getDisplayName());
    }

    /**
     * Logs a disabled test with an optional reason.
     *
     * @param context JUnit extension context
     * @param reason  optional reason for disabling
     */
    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        ExtentTest extentTest = extent.createTest(context.getDisplayName());
        test.set(extentTest);
        getTest().log(Status.SKIP, "Test Disabled: " + reason.orElse("No reason"));
    }

    /**
     * Marks the current test as passed.
     *
     * @param context JUnit extension context
     */
    @Override
    public void testSuccessful(ExtensionContext context) {
        ensureTestExists(context);
        getTest().log(Status.PASS, "Test Passed");
    }

    /**
     * Logs an aborted test with the abort cause.
     *
     * @param context JUnit extension context
     * @param cause   reason for abort
     */
    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        ensureTestExists(context);
        getTest().log(Status.SKIP, "Test Aborted: " + cause.getMessage());
    }

    /**
     * Marks the current test as failed and attaches the throwable.
     *
     * @param context JUnit extension context
     * @param cause   failure cause
     */
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        ensureTestExists(context);

        String errorMsg = cause.getMessage();
        if (errorMsg == null || errorMsg.trim().isEmpty()) {
            errorMsg = cause.getClass().getSimpleName();
            Throwable rootCause = cause.getCause();
            if (rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().trim().isEmpty()) {
                errorMsg += ": " + rootCause.getMessage();
            }
        }

        getTest().log(Status.FAIL, "Test Failed: " + errorMsg);
        getTest().fail(cause);
    }

    /**
     * Flushes the Extent report at the end of the run and optionally executes a
     * post-test hook command if configured via {@code post.test.command}.
     *
     * @param context JUnit extension context
     */
    @Override
    public void afterAll(ExtensionContext context) {
        if (extent != null) {
            extent.flush();
        }

        String postTestCommand = TestConfiguration.getProperty("post.test.command");
        if (postTestCommand != null && !postTestCommand.isEmpty()) {
            executePostTestCommand(postTestCommand);
        }
    }
}
