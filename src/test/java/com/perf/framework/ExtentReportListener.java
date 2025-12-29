package com.perf.framework;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

import java.util.Optional;

/**
 * JUnit 5 extension that wires ExtentReports into the test lifecycle.
 *
 * <p>Initializes a single Extent report per run, creates a test node per method, and
 * logs pass/fail/skip outcomes. Report appearance and path can be configured via
 * {@link TestConfiguration} properties (e.g., report.path, report.title, report.name, report.theme).</p>
 */
public class ExtentReportListener implements BeforeAllCallback, AfterAllCallback, TestWatcher, BeforeEachCallback {

    private static ExtentReports extent;
    private static ExtentTest test;

    /**
     * Returns the current Extent test node for logging.
     *
     * @return the active {@link ExtentTest}, or null if not yet initialized
     */
    public static ExtentTest getTest() {
        return test;
    }

    /**
     * Initializes the Extent report once per test run using configurable settings.
     *
     * @param context JUnit extension context
     */
    @Override
    public void beforeAll(ExtensionContext context) {
        if (extent == null) {
            String reportPath = TestConfiguration.getProperty("report.path", "target/extent-report.html");
            String reportTitle = TestConfiguration.getProperty("report.title", "JMeter Test Report");
            String reportName = TestConfiguration.getProperty("report.name", "Performance Test Execution");
            String themeStr = TestConfiguration.getProperty("report.theme", "STANDARD");
            Theme theme = "DARK".equalsIgnoreCase(themeStr) ? Theme.DARK : Theme.STANDARD;

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(theme);
            spark.config().setDocumentTitle(reportTitle);
            spark.config().setReportName(reportName);

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        }
    }

    /**
     * Creates an Extent test node for the current test method.
     *
     * @param context JUnit extension context
     */
    @Override
    public void beforeEach(ExtensionContext context) {
        test = extent.createTest(context.getDisplayName());
    }

    /**
     * Logs a disabled test with an optional reason.
     *
     * @param context JUnit extension context
     * @param reason  optional reason for disabling
     */
    @Override
    public void testDisabled(ExtensionContext context, Optional<String> reason) {
        test = extent.createTest(context.getDisplayName());
        test.log(Status.SKIP, "Test Disabled: " + reason.orElse("No reason"));
    }

    /**
     * Marks the current test as passed.
     *
     * @param context JUnit extension context
     */
    @Override
    public void testSuccessful(ExtensionContext context) {
        if (test == null) {
            test = extent.createTest(context.getDisplayName());
        }
        test.log(Status.PASS, "Test Passed");
    }

    /**
     * Logs an aborted test with the abort cause.
     *
     * @param context JUnit extension context
     * @param cause   reason for abort
     */
    @Override
    public void testAborted(ExtensionContext context, Throwable cause) {
        if (test == null) {
            test = extent.createTest(context.getDisplayName());
        }
        test.log(Status.SKIP, "Test Aborted: " + cause.getMessage());
    }

    /**
     * Marks the current test as failed and attaches the throwable.
     *
     * @param context JUnit extension context
     * @param cause   failure cause
     */
    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        if (test == null) {
            test = extent.createTest(context.getDisplayName());
        }
        test.log(Status.FAIL, "Test Failed: " + cause.getMessage());
        test.fail(cause);
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

        // Also run the hook command if configured (preserving previous functionality)
        String postTestCommand = TestConfiguration.getProperty("post.test.command");
        if (postTestCommand != null && !postTestCommand.isEmpty()) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder();
                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                    processBuilder.command("cmd.exe", "/c", postTestCommand);
                } else {
                    processBuilder.command("sh", "-c", postTestCommand);
                }
                processBuilder.start().waitFor();
            } catch (Exception e) {
                // Ignore silent failure for hooks in this listener
            }
        }
    }
}
