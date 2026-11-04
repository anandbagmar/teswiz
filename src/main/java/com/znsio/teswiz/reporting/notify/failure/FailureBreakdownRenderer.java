package com.znsio.teswiz.reporting.notify.failure;

import java.util.List;
import java.util.Map;

import com.znsio.teswiz.reporting.notify.HtmlReport;

/**
 * Renders a {@link FailureBreakdown} for both notification channels, so the Slack message and the HTML email show the
 * same "why did things fail?" block. Exception types appear by simple name only (no package), each with where it came
 * from and how many scenarios it took down, biggest cause first. An empty breakdown renders an empty string, so a green
 * run carries no block at all.
 */
public final class FailureBreakdownRenderer {

    /** How many causes the Slack block lists when no limit is given; the rest are summarised in one line. */
    public static final int DEFAULT_MAX_CAUSES = 10;

    private static final String SLACK_HEADING = "Why did things fail?";
    private static final String HTML_HEADING = "Failure breakdown";
    private static final String CODE_FENCE = "```";
    private static final String COLUMN_GAP = "  ";

    private FailureBreakdownRenderer() {
    }

    /**
     * Renders the Slack block listing up to {@link #DEFAULT_MAX_CAUSES} causes — see
     * {@link #slack(FailureBreakdown, int)}.
     *
     * @param breakdown the failure breakdown
     * @return the Slack text block, or an empty string when there are no failures
     */
    public static String slack(FailureBreakdown breakdown) {
        return slack(breakdown, DEFAULT_MAX_CAUSES);
    }

    /**
     * Renders the Slack block: a bold heading with the failure and cause counts
     * ({@code *Why did things fail?*  4 failures · 3 causes}), then the top causes as aligned rows inside a code block
     * — count (right-aligned), type, place — biggest first. When more causes exist than are listed, one line after the
     * code block says how many more are in the full report. Backticks in a cause become {@code '} so they cannot close
     * the code block.
     *
     * @param breakdown the failure breakdown
     * @param maxCauses the most causes to list
     * @return the Slack text block, or an empty string when there are no failures
     */
    public static String slack(FailureBreakdown breakdown, int maxCauses) {
        if (breakdown.isEmpty()) {
            return "";
        }
        List<Map.Entry<FailureCause, Integer>> shown = breakdown.causes().entrySet().stream().limit(maxCauses).toList();
        return slackHeading(breakdown) + slackRows(shown) + moreCausesLine(breakdown.causes().size() - shown.size());
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

    private static String slackHeading(FailureBreakdown breakdown) {
        return "*" + SLACK_HEADING + "*" + COLUMN_GAP + counted(breakdown.total(), "failure") + " · "
                + counted(breakdown.causes().size(), "cause") + "\n";
    }

    // The rows sit in a code block so Slack renders them monospaced and the padded columns line up.
    private static String slackRows(List<Map.Entry<FailureCause, Integer>> shown) {
        int countWidth = shown.stream().mapToInt(entry -> String.valueOf(entry.getValue()).length()).max().orElse(1);
        int typeWidth = shown.stream().mapToInt(entry -> entry.getKey().type().length()).max().orElse(0);
        StringBuilder rows = new StringBuilder(CODE_FENCE).append('\n');
        for (Map.Entry<FailureCause, Integer> entry : shown) {
            rows.append(slackRow(entry.getKey(), entry.getValue(), countWidth, typeWidth)).append('\n');
        }
        return rows.append(CODE_FENCE).append('\n').toString();
    }

    private static String slackRow(FailureCause cause, int count, int countWidth, int typeWidth) {
        String row = padLeft(String.valueOf(count), countWidth) + COLUMN_GAP + padRight(cause.type(), typeWidth)
                + COLUMN_GAP + cause.location();
        return insideCodeBlock(row.stripTrailing());
    }

    private static String moreCausesLine(int hiddenCauses) {
        return hiddenCauses <= 0 ? "" : "_+" + counted(hiddenCauses, "more cause") + " in the full report_\n";
    }

    private static String counted(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private static String padLeft(String text, int width) {
        return " ".repeat(Math.max(0, width - text.length())) + text;
    }

    private static String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    // A backtick inside a code block would close it early; a plain quote reads the same.
    private static String insideCodeBlock(String text) {
        return text.replace('`', '\'');
    }

    private static String htmlRow(FailureCause cause, int count) {
        return "<tr><td>" + HtmlReport.escape(cause.type()) + "</td><td>" + HtmlReport.escape(cause.location())
                + "</td><td>" + count + "</td></tr>";
    }
}
