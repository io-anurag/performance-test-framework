package com.perf.reporting;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import com.perf.framework.TestConfiguration;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.assertions.AssertionResult;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * JMeter listener that forwards sample results to ExtentReports.
 * Buffers results to generate a statistical summary at the end of execution.
 * 
 * <p>
 * Thread Safety: Uses a synchronized list to handle concurrent sample events
 * from multiple JMeter threads.
 * </p>
 */
public class ExtentReportJMeterListener extends ResultCollector {

    private final List<SampleResult> resultsBuffer = Collections.synchronizedList(new ArrayList<>());
    private static final String COLOR_GET = "#17a2b8"; // Cyan
    private static final String COLOR_POST = "#28a745"; // Green
    private static final String COLOR_PUT = "#ffc107"; // Yellow
    private static final String COLOR_DELETE = "#dc3545"; // Red
    private static final String COLOR_DEFAULT = "#777"; // Gray
    private static final String METHOD_FALLBACK = "N/A";
    private final boolean showResponseBody;
    private final boolean excludeTransactionControllers;
    
    public ExtentReportJMeterListener() {
        this.showResponseBody = TestConfiguration.getBooleanProperty("report.show.response.body", true);
        this.excludeTransactionControllers = TestConfiguration.getBooleanProperty("report.exclude.transaction.controllers", true);
    }

    /**
     * Receives each sampler event and buffers its result for post-execution reporting.
     * Delegates to the superclass to preserve default ResultCollector behavior.
     *
     * @param e JMeter sample event containing the sample result
     */
    @Override
    public void sampleOccurred(SampleEvent e) {
        super.sampleOccurred(e);
        SampleResult result = e.getResult();
        if (result != null) {
            resultsBuffer.add(result);
        }
    }

    /**
     * Generates and flushes the execution report to ExtentReports.
     * Includes both summary statistics and detailed execution log.
     */
    public void flush() {
        ExtentTest currentTest = ExtentReportListener.getTest();
        if (currentTest == null || resultsBuffer.isEmpty()) {
            return;
        }

        String summaryHtml = generateSummaryHtml();
        String tableHtml = generateTableHtml();

        currentTest.info(MarkupHelper.createLabel("Execution Statistics", ExtentColor.TEAL));
        currentTest.info(summaryHtml);

        currentTest.info(MarkupHelper.createLabel("Detailed Execution Log", ExtentColor.TEAL));
        currentTest.info(tableHtml);

        boolean hasFailure = resultsBuffer.stream().anyMatch(r -> !r.isSuccessful());
        if (hasFailure) {
            currentTest.log(Status.FAIL, "Test Completed with Failures");
        } else {
            currentTest.log(Status.PASS, "Test Completed Successfully");
        }
    }

    /**
     * Checks if any sample result failed (due to assertions or other errors).
     * 
     * @return true if any sample failed, false otherwise
     */
    public boolean hasFailures() {
        return resultsBuffer.stream().anyMatch(r -> !r.isSuccessful());
    }

    /**
     * Generates HTML summary table with aggregate statistics.
     */
    private String generateSummaryHtml() {
        SummaryStats stats = calculateStats();

        return String.format(
                "<table class='table table-sm table-bordered'>%s<tbody><tr>" +
                        "<td>%d</td>" +
                        "<td style='color: green; font-weight: bold;'>%d</td>" +
                        "<td style='color: red; font-weight: bold;'>%d</td>" +
                        "<td>%.2f ms</td>" +
                        "<td>%d ms</td>" +
                        "<td>%d ms</td>" +
                        "</tr></tbody></table>",
                HtmlTemplates.SUMMARY_TABLE_HEADER,
                stats.total, stats.pass, stats.fail, stats.avg, stats.min, stats.max);
    }

    /**
     * Calculates aggregate statistics from buffered results.
     */
    private SummaryStats calculateStats() {
        int total = resultsBuffer.size();
        int pass = 0;
        int fail = 0;
        long totalDuration = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;

        for (SampleResult result : resultsBuffer) {
            if (result.isSuccessful()) {
                pass++;
            } else {
                fail++;
            }
            long time = result.getTime();
            totalDuration += time;
            min = Math.min(min, time);
            max = Math.max(max, time);
        }

        if (total == 0) {
            min = 0;
            max = 0;
        }

        double avg = (total > 0) ? (double) totalDuration / total : 0;

        return new SummaryStats(total, pass, fail, avg, min, max);
    }

    /**
     * Generates detailed HTML table with individual sample results.
     */
    private String generateTableHtml() {
        StringBuilder table = new StringBuilder(512);
        table.append("<table class='table table-sm table-bordered'>");
        table.append(HtmlTemplates.EXECUTION_LOG_TABLE_HEADER);
        table.append("<tbody>");

        List<SampleResult> filteredResults = new ArrayList<>();
        synchronized (resultsBuffer) {
            if (excludeTransactionControllers) {
                // Extract individual HTTP samplers from Transaction Controllers
                for (SampleResult result : resultsBuffer) {
                    if (result instanceof HTTPSampleResult) {
                        // It's already an HTTP sampler, add it
                        filteredResults.add(result);
                    } else if (result.getSubResults() != null && result.getSubResults().length > 0) {
                        // It's a Transaction Controller, extract its sub-results
                        for (SampleResult subResult : result.getSubResults()) {
                            if (subResult instanceof HTTPSampleResult) {
                                filteredResults.add(subResult);
                            }
                        }
                    }
                }
            } else {
                filteredResults = new ArrayList<>(resultsBuffer);
            }
            
            for (SampleResult result : filteredResults) {
                table.append(buildRowHtml(result));
            }
        }

        table.append("</tbody></table>");
        return table.toString();
    }

    /**
     * Builds HTML row for a single sample result.
     */
    private String buildRowHtml(SampleResult result) {
        String url = extractUrl(result);
        String method = extractMethod(result);
        String responseCode = getResponseCode(result);
        long latency = result.getTime();
        boolean isSuccess = result.isSuccessful();

        String methodBadge = String.format(HtmlTemplates.METHOD_BADGE_TEMPLATE,
                getMethodColor(method), method);
        String statusBadge = String.format(HtmlTemplates.STATUS_BADGE_TEMPLATE,
                isSuccess ? "green" : "red",
                isSuccess ? responseCode : responseCode + " FAIL");

        StringBuilder row = new StringBuilder(256);
        row.append("<tr>");
        row.append("<td>").append(methodBadge).append("</td>");
        row.append(String.format("<td class='col-url' title='%s'>%s</td>", url, url));
        row.append("<td>").append(statusBadge).append("</td>");
        row.append(String.format("<td style='text-align: right;'>%d ms</td>", latency));
        row.append("</tr>");

        if (!isSuccess) {
            String errorMsg = extractErrorMessage(result);
            String responseData = extractResponseData(result);
            row.append(String.format(HtmlTemplates.ERROR_ROW_TEMPLATE,
                    errorMsg, responseData));
        } else if (showResponseBody) {
            // Show response body for successful requests if configured
            String responseData = extractResponseData(result);
            if (responseData != null && !"No response body".equals(responseData)) {
                row.append(String.format(
                    "<tr><td colspan='4' style='color: #28a745; padding: 10px; background-color: #f0f8f0;'>" +
                    "<b>Response:</b> <pre style='margin: 5px 0; white-space: pre-wrap; word-wrap: break-word;'>%s</pre></td></tr>",
                    escapeHtml(responseData)));
            }
        }

        return row.toString();
    }

    /**
     * Extracts response data for error display.
     */
    private String extractResponseData(SampleResult result) {
        String responseDataString = result.getResponseDataAsString();
        if (responseDataString != null && !responseDataString.trim().isEmpty()) {
            int maxLength = 500;
            if (responseDataString.length() > maxLength) {
                return responseDataString.substring(0, maxLength) + "... (truncated)";
            }
            return responseDataString;
        }
        return "No response body";
    }

    /**
     * Extracts detailed error message from failed sample result.
     * Checks assertion results first, then falls back to response message.
     * Also checks sub-results for assertions (e.g., in TransactionController
     * samples).
     */
    private String extractErrorMessage(SampleResult result) {
        // First, check for assertion failures on this result
        String msg = extractAssertionFailures(result);
        if (msg != null) {
            return msg;
        }

        // Check sub-results (e.g., when result is from TransactionController)
        SampleResult[] subResults = result.getSubResults();
        if (subResults != null && subResults.length > 0) {
            StringBuilder subErrors = new StringBuilder();
            for (SampleResult subResult : subResults) {
                String subMsg = extractAssertionFailures(subResult);
                if (subMsg != null) {
                    if (!subErrors.isEmpty()) {
                        subErrors.append("; ");
                    }
                    subErrors.append(subMsg);
                }
            }
            if (!subErrors.isEmpty()) {
                return subErrors.toString();
            }
        }

        String responseMsg = result.getResponseMessage();
        if (responseMsg != null && !responseMsg.isEmpty() && !"null".equalsIgnoreCase(responseMsg)) {
            return responseMsg;
        }

        String responseCode = result.getResponseCode();
        if (responseCode != null && !responseCode.isEmpty()) {
            return "Request failed with response code: " + responseCode;
        }
        return "Request failed - check logs for details";
    }

    /**
     * Extracts assertion failure messages from a single SampleResult.
     */
    private String extractAssertionFailures(SampleResult result) {
        AssertionResult[] assertionResults = result.getAssertionResults();
        if (assertionResults != null && assertionResults.length > 0) {
            StringBuilder assertionErrors = new StringBuilder();
            for (AssertionResult assertion : assertionResults) {
                if (assertion.isError() || assertion.isFailure()) {
                    if (!assertionErrors.isEmpty()) {
                        assertionErrors.append("; ");
                    }

                    String failureMsg = assertion.getFailureMessage();
                    String assertionName = assertion.getName();

                    if (failureMsg != null && !failureMsg.trim().isEmpty()) {
                        assertionErrors.append(failureMsg);
                    } else if (assertionName != null && !assertionName.trim().isEmpty()) {
                        assertionErrors.append(assertionName).append(" failed");
                    } else {
                        assertionErrors.append("Assertion failed");
                    }
                }
            }
            if (!assertionErrors.isEmpty()) {
                return assertionErrors.toString();
            }
        }
        return null;
    }

    /**
     * Extracts URL from result, checking sub-results if necessary.
     * Adds null-safety and defensive checks.
     */
    private String extractUrl(SampleResult result) {
        String url = result.getUrlAsString();

        if (isInvalid(url) && hasValidSubResults(result)) {
            SampleResult subResult = result.getSubResults()[0];
            if (subResult != null) {
                url = subResult.getUrlAsString();
            }
        }

        return isInvalid(url) ? "N/A" : url;
    }

    /**
     * Extracts HTTP method from result, checking sub-results if necessary.
     * Adds null-safety and defensive checks.
     */
    private String extractMethod(SampleResult result) {
        String method = METHOD_FALLBACK;

        if (result instanceof HTTPSampleResult httpResult) {
            String httpMethod = httpResult.getHTTPMethod();
            if (httpMethod != null && !httpMethod.isEmpty()) {
                method = httpMethod;
            }
        }

        if (METHOD_FALLBACK.equals(method) && hasValidSubResults(result)) {
            SampleResult subResult = result.getSubResults()[0];
            if (subResult instanceof HTTPSampleResult httpSubResult) {
                String httpMethod = httpSubResult.getHTTPMethod();
                if (httpMethod != null && !httpMethod.isEmpty()) {
                    method = httpMethod;
                }
            }
        }

        return method;
    }

    /**
     * Safely extracts response code with fallback.
     */
    private String getResponseCode(SampleResult result) {
        String code = result.getResponseCode();
        return (code != null && !code.isEmpty()) ? code : "N/A";
    }

    /**
     * Checks if result has valid sub-results.
     * Adds null-safety check for array elements.
     */
    private boolean hasValidSubResults(SampleResult result) {
        SampleResult[] subResults = result.getSubResults();
        return subResults != null && subResults.length > 0 && subResults[0] != null;
    }

    /**
     * Checks if a string value is invalid (null, empty, or "null").
     */
    private boolean isInvalid(String value) {
        return value == null || value.isEmpty() || "null".equals(value);
    }

    /**
     * Returns color code for HTTP method badge.
     */
    private String getMethodColor(String method) {
        if (method == null) {
            return COLOR_DEFAULT;
        }

        return switch (method.toUpperCase()) {
            case "GET" -> COLOR_GET;
            case "POST" -> COLOR_POST;
            case "PUT" -> COLOR_PUT;
            case "DELETE" -> COLOR_DELETE;
            default -> COLOR_DEFAULT;
        };
    }
    
    /**
     * Escapes HTML special characters to prevent XSS and rendering issues.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }

    /**
     * Internal class to hold summary statistics.
     */
    private static class SummaryStats {
        final int total;
        final int pass;
        final int fail;
        final double avg;
        final long min;
        final long max;

        SummaryStats(int total, int pass, int fail, double avg, long min, long max) {
            this.total = total;
            this.pass = pass;
            this.fail = fail;
            this.avg = avg;
            this.min = min;
            this.max = max;
        }
    }
}
