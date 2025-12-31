package com.perf.framework;

import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

/**
 * JMeter listener that forwards sample results to ExtentReports.
 * Buffers results to generate a statistical summary at the end of execution.
 */
public class ExtentReportJMeterListener extends ResultCollector {

    private final List<SampleResult> resultsBuffer = Collections.synchronizedList(new ArrayList<>());
    private static final String TABLE_HEADER = "<thead><tr class='bg-gray'><th>Method</th><th>URL</th><th>Status</th><th>Duration</th></tr></thead>";
    private static final String ERROR_ROW_TEMPLATE = "<tr><td colspan='4' style='color: red; padding: 10px; background-color: #330000;'><b>Error:</b> %s<br/><b>Response:</b> %s</td></tr>";

    @Override
    public void sampleOccurred(SampleEvent e) {
        super.sampleOccurred(e);
        SampleResult result = e.getResult();
        resultsBuffer.add(result);
    }

    public void flush() {
        if (ExtentReportListener.getTest() == null || resultsBuffer.isEmpty()) {
            return;
        }

        String summaryHtml = generateSummaryHtml();
        String tableHtml = generateTableHtml();

        ExtentReportListener.getTest().info(MarkupHelper.createLabel("Execution Statistics", ExtentColor.TEAL));
        ExtentReportListener.getTest().info(summaryHtml);

        ExtentReportListener.getTest().info(MarkupHelper.createLabel("Detailed Execution Log", ExtentColor.TEAL));
        ExtentReportListener.getTest().info(tableHtml);

        // Log final status based on failures
        boolean hasFailure = resultsBuffer.stream().anyMatch(r -> !r.isSuccessful());
        if (hasFailure) {
            ExtentReportListener.getTest().log(Status.FAIL, "Test Completed with Failures");
        } else {
            ExtentReportListener.getTest().log(Status.PASS, "Test Completed Successfully");
        }
    }

    private String generateSummaryHtml() {
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
            if (time < min)
                min = time;
            if (time > max)
                max = time;
        }

        if (total == 0) {
            min = 0;
            max = 0;
        }

        double avg = (total > 0) ? (double) totalDuration / total : 0;

        StringBuilder summary = new StringBuilder();
        summary.append("<table class='table table-sm table-bordered'>");
        summary.append(
                "<thead><tr><th>Total</th><th>Pass</th><th>Fail</th><th>Avg Duration</th><th>Min</th><th>Max</th></tr></thead>");
        summary.append("<tbody><tr>");
        summary.append(String.format("<td>%d</td>", total));
        summary.append(String.format("<td style='color: green; font-weight: bold;'>%d</td>", pass));
        summary.append(String.format("<td style='color: red; font-weight: bold;'>%d</td>", fail));
        summary.append(String.format("<td>%.2f ms</td>", avg));
        summary.append(String.format("<td>%d ms</td>", min));
        summary.append(String.format("<td>%d ms</td>", max));
        summary.append("</tr></tbody></table>");

        return summary.toString();
    }

    private String generateTableHtml() {
        StringBuilder table = new StringBuilder();
        table.append("<table class='table table-sm table-bordered'>");
        table.append(TABLE_HEADER);
        table.append("<tbody>");

        synchronized (resultsBuffer) {
            for (SampleResult result : resultsBuffer) {
                table.append(buildRowHtml(result));
            }
        }

        table.append("</tbody></table>");
        return table.toString();
    }

    private String buildRowHtml(SampleResult result) {
        String url = extractUrl(result);
        String method = extractMethod(result);
        String responseCode = result.getResponseCode();
        long latency = result.getTime();
        boolean isSuccess = result.isSuccessful();

        StringBuilder row = new StringBuilder();
        row.append("<tr>");
        row.append(String.format("<td><span class='badge-method' style='background-color: %s'>%s</span></td>",
                getMethodColor(method), method));
        row.append(String.format("<td class='col-url' title='%s'>%s</td>", url, url));
        row.append(String.format("<td><span class='badge white-text %s'>%s</span></td>", isSuccess ? "green" : "red",
                isSuccess ? responseCode : responseCode + " FAIL"));
        row.append(String.format("<td style='text-align: right;'>%d ms</td>", latency));
        row.append("</tr>");

        if (!isSuccess) {
            row.append(
                    String.format(ERROR_ROW_TEMPLATE, result.getResponseMessage(), "Check Logs"));
        }

        return row.toString();
    }

    private String extractUrl(SampleResult result) {
        String url = result.getUrlAsString();
        if (isInvalid(url) && hasSubResults(result)) {
            url = result.getSubResults()[0].getUrlAsString();
        }
        return url;
    }

    private String extractMethod(SampleResult result) {
        String method = "N/A";
        if (result instanceof HTTPSampleResult) {
            method = ((HTTPSampleResult) result).getHTTPMethod();
        }
        if ("N/A".equals(method) && hasSubResults(result) && result.getSubResults()[0] instanceof HTTPSampleResult) {
            method = ((HTTPSampleResult) result.getSubResults()[0]).getHTTPMethod();
        }
        return method;
    }

    private boolean hasSubResults(SampleResult result) {
        return result.getSubResults() != null && result.getSubResults().length > 0;
    }

    private boolean isInvalid(String value) {
        return value == null || value.isEmpty() || "null".equals(value);
    }

    private String getMethodColor(String method) {
        if ("GET".equalsIgnoreCase(method))
            return "#17a2b8";
        if ("POST".equalsIgnoreCase(method))
            return "#28a745";
        if ("PUT".equalsIgnoreCase(method))
            return "#ffc107";
        if ("DELETE".equalsIgnoreCase(method))
            return "#dc3545";
        return "#777";
    }
}
