package com.perf.framework;

import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.markuputils.ExtentColor;
import com.aventstack.extentreports.markuputils.MarkupHelper;
import org.apache.jmeter.reporters.ResultCollector;
import org.apache.jmeter.samplers.SampleEvent;
import org.apache.jmeter.samplers.SampleResult;

/**
 * JMeter listener that forwards sample results to ExtentReports.
 *
 * <p>Formats essential request/response details and marks the current Extent test as
 * PASS/FAIL based on {@link SampleResult#isSuccessful()}.</p>
 */
public class ExtentReportJMeterListener extends ResultCollector {

    /**
     * Intercepts JMeter sample events and logs them into the active Extent test with
     * minimal HTML formatting for readability.
     *
     * @param e sample event emitted by JMeter containing the {@link SampleResult}
     */
    @Override
    public void sampleOccurred(SampleEvent e) {
        super.sampleOccurred(e);
        SampleResult result = e.getResult();
        
        if (ExtentReportListener.getTest() != null) {
            String url = result.getUrlAsString();
            String label = result.getSampleLabel();
            String responseCode = result.getResponseCode();
            long latency = result.getTime();
            boolean isSuccess = result.isSuccessful();

            StringBuilder details = new StringBuilder();
            details.append("<b>Sampler:</b> ").append(label).append("<br>");
            if (url != null && !url.isEmpty()) {
                details.append("<b>URL:</b> ").append(url).append("<br>");
            }
            details.append("<b>Response Code:</b> ").append(responseCode).append("<br>");
            details.append("<b>Duration:</b> ").append(latency).append(" ms<br>");
            
            if (!isSuccess) {
                details.append("<b>Error:</b> ").append(result.getResponseMessage()).append("<br>");
                // details.append("<pre>").append(result.getResponseDataAsString()).append("</pre>"); // Optional: Add response body on failure
                ExtentReportListener.getTest().log(Status.FAIL, MarkupHelper.createLabel(details.toString(), ExtentColor.RED));
            } else {
                ExtentReportListener.getTest().log(Status.PASS, MarkupHelper.createLabel(details.toString(), ExtentColor.GREEN));
            }
        }
    }
}
