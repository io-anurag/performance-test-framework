package com.perf.reporting;

/**
 * HTML template constants for ExtentReports formatting.
 * Centralizes all HTML strings used in report generation.
 */
final class HtmlTemplates {

    private HtmlTemplates() {
        // Utility class - prevent instantiation
    }

    /**
     * Custom CSS for compact, clean styling of tabular data and badges.
     */
    static final String CUSTOM_CSS = """
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

    /**
     * Table header for detailed execution log.
     */
    static final String EXECUTION_LOG_TABLE_HEADER = "<thead><tr class='bg-gray'><th>Method</th><th>URL</th><th>Status</th><th>Duration</th></tr></thead>";

    /**
     * Error row template for failed requests.
     * Format parameters: error message, response body
     */
    static final String ERROR_ROW_TEMPLATE = "<tr><td colspan='4' style='color: red; padding: 10px; background-color: #330000;'>"
            +
            "<b>Error:</b> %s<br/><b>Response:</b> %s</td></tr>";

    /**
     * Summary statistics table header.
     */
    static final String SUMMARY_TABLE_HEADER = "<thead><tr><th>Total</th><th>Pass</th><th>Fail</th><th>Avg Duration</th><th>Min</th><th>Max</th></tr></thead>";

    /**
     * Method badge HTML template.
     * Format parameters: background color, method name
     */
    static final String METHOD_BADGE_TEMPLATE = "<span class='badge-method' style='background-color: %s'>%s</span>";

    /**
     * Status badge HTML template.
     * Format parameters: CSS class (green/red), status text
     */
    static final String STATUS_BADGE_TEMPLATE = "<span class='badge white-text %s'>%s</span>";
}
