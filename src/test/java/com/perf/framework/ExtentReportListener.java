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
    private static final ThreadLocal<ExtentTest> test = new ThreadLocal<>();

    /**
     * Returns the current Extent test node for logging.
     *
     * @return the active {@link ExtentTest}, or null if not yet initialized
     */
    public static ExtentTest getTest() {
        return test.get();
    }

    public static void initReport() {
        if (extent == null) {
            String reportPath = TestConfiguration.getProperty("report.path");
            String reportTitle = TestConfiguration.getProperty("report.title");
            String reportName = TestConfiguration.getProperty("report.name");
            String themeStr = TestConfiguration.getProperty("report.theme");
            Theme theme = "DARK".equalsIgnoreCase(themeStr) ? Theme.DARK : Theme.STANDARD;

                // Compact, clean styling for tabular data and badges
                String customCss = """
                    body, .table { font-size: 13px; }
                    .table-sm.table-bordered { border: 1px solid #e0e0e0; }
                    .table-sm.table-bordered td, .table-sm.table-bordered th { padding: 6px 8px; }
                    .table-sm.table-bordered thead tr { background: #f4f6f8; }
                    .table-sm.table-bordered tbody tr:nth-child(even) { background: #fafafa; }
                    .badge-method { color: #fff; padding: 4px 8px; border-radius: 12px; font-weight: 600; font-size: 11px; }
                    .col-url { max-width: 420px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
                    .badge.white-text.green { background: #2e7d32; }
                    .badge.white-text.red { background: #c62828; }
                    .detail-body .table { margin-bottom: 8px; }
                    .test-contents .card, .test-contents .table { box-shadow: none; }
                    .badge.badge-default { background: #e0e0e0; color: #424242; }
                    .badge.badge-success { background: #2e7d32; }
                    .badge.badge-danger { background: #c62828; }
                    .badge.badge-primary { background: #1565c0; }
                    """;

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath);
            spark.config().setTheme(theme);
            spark.config().setDocumentTitle(reportTitle);
            spark.config().setReportName(reportName);
                spark.config().setCss(customCss);

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("OS", System.getProperty("os.name"));
            extent.setSystemInfo("Java Version", System.getProperty("java.version"));
        }
    }

    public static void createTest(String testName) {
        if (extent != null) {
            ExtentTest extentTest = extent.createTest(testName);
            test.set(extentTest);
        }
    }

    public static void flushReport() {
        if (extent != null) {
            extent.flush();
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
        if (test.get() == null) {
            ExtentTest extentTest = extent.createTest(context.getDisplayName());
            test.set(extentTest);
        }
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
        if (test.get() == null) {
            ExtentTest extentTest = extent.createTest(context.getDisplayName());
            test.set(extentTest);
        }
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
        if (test.get() == null) {
            ExtentTest extentTest = extent.createTest(context.getDisplayName());
            test.set(extentTest);
        }
        getTest().log(Status.FAIL, "Test Failed: " + cause.getMessage());
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
