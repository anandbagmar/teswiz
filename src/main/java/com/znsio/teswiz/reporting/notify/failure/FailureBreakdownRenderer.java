package com.znsio.teswiz.reporting.notify.failure;

import java.util.Map;

import com.znsio.teswiz.reporting.notify.HtmlReport;

/**
 * Renders a {@link FailureBreakdown} for both notification channels, so the Slack message and the HTML email show the
 * same "why did things fail?" block. Exception types appear by simple name only (no package), each with where it came
 * from and how many scenarios it took down, biggest cause first. An empty breakdown renders an empty string, so a green
 * run carries no block at all.
 */
public final class FailureBreakdownRenderer {

    private static final String SLACK_HEADING = "Why did things fail?";
    private static final String HTML_HEADING = "Failure breakdown";

    private FailureBreakdownRenderer() {
    }

    /**
     * Renders the Slack block: a bold heading carrying the total, then one line per cause —
     * {@code Type @ Class.method:line  count}.
     *
     * @param breakdown the failure breakdown
     * @return the Slack text block, or an empty string when there are no failures
     */
    public static String slack(FailureBreakdown breakdown) {
        if (breakdown.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        text.append("*").append(SLACK_HEADING).append(" (").append(breakdown.total()).append(")*\n");
        for (Map.Entry<FailureCause, Integer> entry : breakdown.causes().entrySet()) {
            text.append("  ").append(entry.getKey().label()).append("  ").append(entry.getValue()).append('\n');
        }
        return text.toString();
    }

    /**
     * Renders the HTML block: a three-column table (exception type, where, count) headed by the breakdown title, with
     * a total row.
     *
     * @param breakdown the failure breakdown
     * @return the HTML block, or an empty string when there are no failures
     */
    public static String html(FailureBreakdown breakdown) {
        if (breakdown.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        html.append("<h3>").append(HTML_HEADING).append("</h3><table>")
                .append("<tr><th>Exception type</th><th>Where</th><th>Count</th></tr>");
        for (Map.Entry<FailureCause, Integer> entry : breakdown.causes().entrySet()) {
            html.append(htmlRow(entry.getKey(), entry.getValue()));
        }
        html.append("<tr><th colspan=\"2\">Total failed</th><td>").append(breakdown.total()).append("</td></tr>")
                .append("</table>");
        return html.toString();
    }

    private static String htmlRow(FailureCause cause, int count) {
        return "<tr><td>" + HtmlReport.escape(cause.type()) + "</td><td>" + HtmlReport.escape(cause.location())
                + "</td><td>" + count + "</td></tr>";
    }
}
