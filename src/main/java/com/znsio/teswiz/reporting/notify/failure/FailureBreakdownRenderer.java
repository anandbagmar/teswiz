package com.znsio.teswiz.reporting.notify.failure;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.znsio.teswiz.reporting.notify.HtmlReport;

/**
 * Renders a {@link FailureBreakdown} for both notification channels, so the Slack message and the HTML email show the
 * same "why did things fail?" block. Causes are grouped by exception type and code location; when one location was hit
 * by several Gherkin steps, each step is a nested row with its own count under the shared group header, biggest group
 * first. An empty breakdown renders an empty string, so a green run carries no block at all.
 * <p>
 * The exception type is shown through a <b>friendly label</b> — a {@link Function} from the simple class name to a
 * human-readable phrase ({@code PoolExhaustedException} &rarr; {@code No test user available}). The overloads without
 * one default to {@link #HUMANIZED_TYPE}, which turns a {@code CamelCase} class name into words and drops a trailing
 * {@code Exception}/{@code Error} ({@code FreeSpinNotAwardedException} &rarr; {@code Free spin not awarded}); a
 * consumer passes its own function to override specific types. The raw class name is always shown too — below the code
 * location in the HTML "Where" cell, and after it on the Slack location line — so an engineer still has the exact type
 * to search for.
 */
public final class FailureBreakdownRenderer {

    /** How many cause groups the Slack block lists when no limit is given; the rest are summarised in one line. */
    public static final int DEFAULT_MAX_CAUSES = 10;

    /** The simple exception class name unchanged — the raw label, kept for a consumer that wants no humanising. */
    public static final Function<String, String> SIMPLE_TYPE = type -> type;

    /**
     * The default friendly label: a humanised exception class name. A clean {@code CamelCase} class name has a trailing
     * {@code Exception}/{@code Error} dropped, is split into words and sentence-cased, so
     * {@code FreeSpinNotAwardedException} reads {@code Free spin not awarded}. A name that does not look like a plain
     * class identifier (it carries a digit, or a {@code &}, {@code <}, {@code >}, backtick or other non-letter) is left
     * exactly as-is — humanising it would be guesswork and could disturb escaping — as are a blank type and
     * {@code Unclassified}. A consumer overrides specific types by composing its own function on top of this one.
     */
    public static final Function<String, String> HUMANIZED_TYPE = FailureBreakdownRenderer::humanize;

    private static final String SLACK_HEADING = "Why did things fail?";
    private static final String HTML_HEADING = "Failure breakdown";
    private static final String CODE_FENCE = "```";
    private static final String COLUMN_GAP = "  ";
    private static final String STEP_INDENT = "    \u2514 ";
    private static final String GROUP_INDENT = "    ";
    private static final String DASH = " \u00b7 ";
    // Labels a count ("×3") in both channels, so a trailing number on a step row cannot be read as part of the step
    // text and a count reads unmistakably as a count. The HTML "Total failed" row keeps a plain number.
    private static final String COUNT_MARK = "\u00d7";
    // Opens the breakdown table full-width with a fixed layout declared INLINE (not only via the <style> class),
    // because Outlook's Word engine ignores <style>-block table widths and would otherwise shrink the table to its
    // content. The colgroup pins the four columns as percentages (not pixels), so they fill the email card and scale
    // with the viewport: Failure 22%, Where 33%, Failing step 33%, a narrow Count 12%.
    private static final String BREAKDOWN_TABLE_OPEN = "<table class=\"breakdown\" width=\"100%\" "
            + "style=\"width:100%;table-layout:fixed\"><colgroup><col style=\"width:22%\"><col style=\"width:33%\">"
            + "<col style=\"width:33%\"><col style=\"width:12%\"></colgroup>";

    private FailureBreakdownRenderer() {
    }

    /**
     * Renders the Slack block listing up to {@link #DEFAULT_MAX_CAUSES} groups, with the simple class name as the label
     * — see {@link #slack(FailureBreakdown, int, Function)}.
     *
     * @param breakdown the failure breakdown
     * @return the Slack text block, or an empty string when there are no failures
     */
    public static String slack(FailureBreakdown breakdown) {
        return slack(breakdown, DEFAULT_MAX_CAUSES, HUMANIZED_TYPE);
    }

    /**
     * Renders the Slack block listing up to {@code maxCauses} groups, with the simple class name as the label.
     *
     * @param breakdown the failure breakdown
     * @param maxCauses the most cause groups to list
     * @return the Slack text block, or an empty string when there are no failures
     */
    public static String slack(FailureBreakdown breakdown, int maxCauses) {
        return slack(breakdown, maxCauses, HUMANIZED_TYPE);
    }

    /**
     * Renders the Slack block: a bold heading with the failure and cause counts, then up to {@code maxCauses} groups.
     * Each group is a header line — count, friendly label — and a second line with the code location and raw class
     * ({@code SessionBL.leaseUser:88 · PoolExhaustedException}); the failing steps of that group are nested rows with
     * their own counts. Splitting the location and class across two lines keeps each line short so a narrow Slack
     * client does not wrap and misalign them. When more groups exist than are listed, one line after the block says how
     * many more are in the full report. Slack markup is neutralised: {@code & < >} become their entities (so a
     * constructor-frame place like {@code Widget.<init>:7} is not read as a mention) and a backtick becomes {@code '}
     * so it cannot close the code block.
     *
     * @param breakdown the failure breakdown
     * @param maxCauses the most cause groups to list
     * @param label     the friendly label for an exception type (its simple class name); must not return null
     * @return the Slack text block, or an empty string when there are no failures
     */
    public static String slack(FailureBreakdown breakdown, int maxCauses, Function<String, String> label) {
        if (breakdown.isEmpty()) {
            return "";
        }
        List<Group> groups = groups(breakdown);
        List<Group> shown = groups.stream().limit(Math.max(0, maxCauses)).toList();
        return slackHeading(breakdown, groups.size()) + slackBlock(shown, label)
                + moreCausesLine(groups.size() - shown.size());
    }

    /**
     * Renders the HTML block with the simple class name as the label — see {@link #html(FailureBreakdown, Function)}.
     *
     * @param breakdown the failure breakdown
     * @return the HTML block, or an empty string when there are no failures
     */
    public static String html(FailureBreakdown breakdown) {
        return html(breakdown, HUMANIZED_TYPE);
    }

    /**
     * Renders the HTML block: a table whose columns are the friendly label (the "Failure"), the code location with the
     * raw class name beside it ({@code BalanceTracking.assertDelta:182 (AssertJMultipleFailuresError)}), the failing
     * step and the count. Causes are grouped by type and location: a group header carries the label, the location and
     * the group subtotal, and each failing step of that group is a nested row with its own count. A total row ends the
     * table.
     *
     * @param breakdown the failure breakdown
     * @param label     the friendly label for an exception type (its simple class name); must not return null
     * @return the HTML block, or an empty string when there are no failures
     */
    public static String html(FailureBreakdown breakdown, Function<String, String> label) {
        if (breakdown.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        html.append("<h3>").append(HTML_HEADING).append("</h3>").append(BREAKDOWN_TABLE_OPEN)
                .append("<tr><th>Failure</th><th>Where</th><th>Failing step</th><th class=\"count\">Count</th></tr>");
        for (Group group : groups(breakdown)) {
            html.append(htmlGroup(group, label));
        }
        html.append("<tr><th colspan=\"3\">Total failed</th><td class=\"count\">").append(breakdown.total())
                .append("</td></tr>").append("</table>");
        return html.toString();
    }

    // Groups the causes by (type, location) keeping the breakdown's biggest-first order; within a group the steps keep
    // that order too. A group's count is the sum of its steps' counts.
    private static List<Group> groups(FailureBreakdown breakdown) {
        Map<String, Group> byTypeAndLocation = new LinkedHashMap<>();
        breakdown.causes()
                .forEach((cause, count) -> byTypeAndLocation.computeIfAbsent(cause.type() + '\u0000' + cause.location(),
                        key -> new Group(cause.type(), cause.location())).add(cause.step(), count));
        return List.copyOf(byTypeAndLocation.values());
    }

    private static String slackHeading(FailureBreakdown breakdown, int groupCount) {
        return "*" + SLACK_HEADING + "*" + COLUMN_GAP + counted(breakdown.total(), "failure") + DASH
                + counted(groupCount, "cause") + "\n";
    }

    private static String slackBlock(List<Group> shown, Function<String, String> label) {
        StringBuilder rows = new StringBuilder(CODE_FENCE).append('\n');
        for (Group group : shown) {
            rows.append(slackGroup(group, label));
        }
        return rows.append(CODE_FENCE).append('\n').toString();
    }

    private static String slackGroup(Group group, Function<String, String> label) {
        StringBuilder block = new StringBuilder();
        block.append(slackSafe(COUNT_MARK + group.count() + COLUMN_GAP + label.apply(group.type()))).append('\n');
        block.append(GROUP_INDENT).append(slackSafe(group.locationAndType())).append('\n');
        int stepWidth = group.steps().keySet().stream().mapToInt(String::length).max().orElse(0);
        // Pad each step to the widest in the group so the per-step counts line up in a column; pad on the raw text
        // then escape, since an entity renders as one glyph and escaping first would miscount the width.
        group.steps().forEach((step, count) -> block.append(STEP_INDENT).append(slackSafe(padRight(step, stepWidth)))
                .append(COLUMN_GAP).append(COUNT_MARK).append(count).append('\n'));
        return block.toString();
    }

    // Slack reads & < > as markup everywhere, even inside a code block, so a place like Widget.<init>:7 would lose its
    // <init>; a backtick would also close the code block. Replace & first, or the & in &lt;/&gt; is double-escaped.
    private static String slackSafe(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('`', '\'');
    }

    private static String moreCausesLine(int hiddenCauses) {
        return hiddenCauses <= 0 ? ""
                : "_+" + slackSafe(counted(hiddenCauses, "more cause")) + " in the full report_\n";
    }

    private static String counted(int count, String noun) {
        return count + " " + noun + (count == 1 ? "" : "s");
    }

    private static String padRight(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    // Turns a clean CamelCase exception class name into a human phrase: drop a trailing Exception/Error, split on the
    // CamelCase boundaries, and sentence-case the result, so FreeSpinNotAwardedException reads "Free spin not awarded".
    // A name that is not a plain letters-only identifier (it carries a digit or any non-letter such as & < > or a
    // backtick) is left exactly as-is — humanising it would be guesswork and could disturb the channel escaping — and
    // so are a blank type and the special "Unclassified" marker.
    private static String humanize(String type) {
        if (type == null || type.isEmpty() || "Unclassified".equals(type)
                || !type.chars().allMatch(Character::isLetter)) {
            return type;
        }
        String core = type.replaceAll("(Exception|Error)$", "");
        if (core.isEmpty()) {
            return type;
        }
        String spaced = core.replaceAll("(?<=[a-z])(?=[A-Z])", " ").replaceAll("(?<=[A-Z])(?=[A-Z][a-z])", " ");
        String lower = spaced.toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private static String htmlGroup(Group group, Function<String, String> label) {
        StringBuilder rows = new StringBuilder();
        rows.append("<tr><td>").append(HtmlReport.escape(label.apply(group.type()))).append("</td><td>")
                .append(group.whereHtml()).append("</td><td></td><td class=\"count\">").append(COUNT_MARK)
                .append(group.count()).append("</td></tr>");
        group.steps().forEach((step, count) -> rows.append("<tr><td></td><td></td><td>").append(HtmlReport.escape(step))
                .append("</td><td class=\"count\">").append(COUNT_MARK).append(count).append("</td></tr>"));
        return rows.toString();
    }

    // One (type, location) group: its failing steps counted, in insertion (biggest-first) order, and the subtotal.
    private static final class Group {

        private final String type;
        private final String location;
        private final Map<String, Integer> steps = new LinkedHashMap<>();
        private int count;

        private Group(String type, String location) {
            this.type = type;
            this.location = location;
        }

        private void add(String step, int stepCount) {
            steps.merge(step, stepCount, Integer::sum);
            count += stepCount;
        }

        private String type() {
            return type;
        }

        private int count() {
            return count;
        }

        private Map<String, Integer> steps() {
            return steps;
        }

        // The "Where" as one Slack line: the location then the raw class name (no parentheses); just the location when
        // the type is blank, and only the type when there is no location.
        private String locationAndType() {
            if (location.isEmpty()) {
                return type;
            }
            return type.isEmpty() ? location : location + " " + type;
        }

        // The "Where" as an escaped HTML cell: the location on the first line and the raw class on the second (a <br>),
        // no parentheses; just the location when the type is blank, and only the type when there is no location.
        private String whereHtml() {
            if (location.isEmpty()) {
                return HtmlReport.escape(type);
            }
            if (type.isEmpty()) {
                return HtmlReport.escape(location);
            }
            return HtmlReport.escape(location) + "<br>" + HtmlReport.escape(type);
        }
    }
}
