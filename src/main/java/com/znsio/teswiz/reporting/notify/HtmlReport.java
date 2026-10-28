package com.znsio.teswiz.reporting.notify;

import java.util.List;
import java.util.Map;

/**
 * Generic, self-contained HTML + Slack rendering helpers for a notification. "Self-contained" means the HTML carries
 * only an inline {@code <style>} and no external asset references (no {@code <link>}, {@code <script>} or
 * {@code src=}), so it renders in any mail client without fetching anything. These helpers know nothing about what the
 * message means — a consuming project supplies the title, body fragments and link labels. The run-context footer and
 * the Slack separator are provided so every teswiz consumer renders provenance consistently.
 */
public final class HtmlReport {

    private static final String INLINE_STYLE = "<style>"
            + "body{font-family:Arial,Helvetica,sans-serif;color:#1d1d1d;font-size:14px;margin:0;padding:0;"
            + "background:#f4f5f7}"
            + ".card{max-width:680px;margin:16px auto;background:#fff;border:1px solid #e0e0e0;border-radius:8px;"
            + "overflow:hidden}" + ".banner{padding:16px 20px;color:#fff;font-size:18px;font-weight:bold}"
            + ".banner.pass{background:#2e7d32}.banner.fail{background:#b00020}" + ".content{padding:16px 20px}"
            + "h3{margin:18px 0 8px;font-size:15px}" + ".fail{color:#b00020}.pass{color:#2e7d32}.muted{color:#666}"
            + "table{border-collapse:collapse;width:100%;margin:4px 0 12px}"
            + "td,th{border:1px solid #e0e0e0;padding:6px 10px;text-align:left;font-size:13px}"
            + "th{background:#fafafa;width:40%;color:#555;font-weight:600}"
            + "ul{margin:4px 0 12px 20px}a{color:#1565c0}"
            + "code{background:#f4f5f7;border:1px solid #e0e0e0;border-radius:4px;padding:2px 6px;"
            + "font-family:Menlo,Consolas,monospace;font-size:12px;display:inline-block;word-break:break-all}"
            + "table.scenarios th{width:auto}"
            + "table.scenarios td.status,table.scenarios th.status{width:64px;text-align:center}"
            + "table.scenarios td.time,table.scenarios th.time{width:64px;text-align:right;color:#666;"
            + "white-space:nowrap}"
            + ".pill{display:inline-block;padding:2px 8px;border-radius:10px;font-size:11px;font-weight:700;"
            + "color:#fff}"
            + ".pill.pass{background:#2e7d32}.pill.fail{background:#b00020}.pill.skip{background:#9e9e9e}"
            + ".reason{color:#b00020;font-size:12px;display:block;margin-top:2px}"
            + ".footer{padding:12px 20px;background:#fafafa;border-top:1px solid #e0e0e0;color:#666;font-size:12px}"
            + "</style>";

    /** The trailing line separator a Slack message ends with, so consecutive messages read as distinct blocks. */
    private static final String SLACK_SEPARATOR = "\n━━━━━━━━━━━━━━━━━━━━";

    private HtmlReport() {
    }

    /**
     * Wraps pre-escaped body fragments in a self-contained HTML document with an inline style, a coloured status banner
     * and a card layout; no external references.
     *
     * @param title       the banner text (escaped here)
     * @param failed      whether this reports a failure (red banner) or a pass (green banner)
     * @param bodyContent the pre-escaped inner HTML fragments
     * @param footer      the pre-rendered footer (may be empty)
     * @return a complete HTML document string
     */
    public static String document(String title, boolean failed, String bodyContent, String footer) {
        String bannerClass = failed ? "fail" : "pass";
        return "<html><head><meta charset=\"utf-8\">" + INLINE_STYLE + "</head><body><div class=\"card\">"
                + "<div class=\"banner " + bannerClass + "\">" + escape(title) + "</div>" + "<div class=\"content\">"
                + bodyContent + "</div>" + footer + "</div></body></html>";
    }

    /**
     * Renders the run-context footer: the CI pipeline / job trace for a CI run (labelled with the detected provider),
     * or the host + user for a local run, plus the reconstructed reproduce command. Returns an empty string when no
     * environment is supplied.
     *
     * @param environment      the resolved run provenance (may be {@code null})
     * @param reproduceCommand the client-supplied "how to re-run this" command (may be blank → the Reproduce line is
     *                         omitted). teswiz does not know which run inputs matter, so the consumer composes this
     *                         (e.g. via {@link #reproduceCommand(Map)}).
     * @return the footer HTML, or an empty string
     */
    public static String footer(RunEnvironment environment, String reproduceCommand) {
        if (environment == null) {
            return "";
        }
        String rows = environment.isCi() ? ciRows(environment) : localRows(environment);
        String reproduce = (reproduceCommand == null || reproduceCommand.isBlank()) ? ""
                : "<p class=\"muted\">Reproduce with:<br><code>" + escape(reproduceCommand) + "</code></p>";
        return "<div class=\"footer\"><strong>Run context</strong><table>" + rows + "</table>" + reproduce + "</div>";
    }

    /**
     * Renders drill-down links as an HTML paragraph — only on a CI run, where those URLs exist; on a local run the
     * links would be empty, so the block is omitted.
     *
     * @param labelToUrl  ordered label → URL pairs (e.g. "Full report" → reportUrl)
     * @param environment the run provenance (links shown only when {@link RunEnvironment#isCi()})
     * @return an HTML paragraph of links, or an empty string on a local run
     */
    public static String links(Map<String, String> labelToUrl, RunEnvironment environment) {
        if (environment == null || !environment.isCi() || labelToUrl == null || labelToUrl.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<p class=\"muted\">");
        boolean first = true;
        for (Map.Entry<String, String> entry : labelToUrl.entrySet()) {
            if (!first) {
                html.append(" &middot; ");
            }
            html.append("<a href=\"").append(escape(entry.getValue())).append("\">").append(escape(entry.getKey()))
                    .append("</a>");
            first = false;
        }
        return html.append("</p>").toString();
    }

    /**
     * Renders the plain-text run-context footer for a Slack message, mirroring {@link #footer(RunEnvironment)}.
     *
     * @param environment      the resolved run provenance (may be {@code null})
     * @param reproduceCommand the client-supplied "how to re-run this" command (may be blank → the Reproduce line is
     *                         omitted); the consumer composes it (e.g. via {@link #reproduceCommand(Map)})
     * @return the Slack footer text (leading newline included), or an empty string
     */
    public static String slackFooter(RunEnvironment environment, String reproduceCommand) {
        if (environment == null) {
            return "";
        }
        StringBuilder text = new StringBuilder("\n_Run context_\n");
        if (environment.isCi()) {
            appendLine(text, "CI", environment.ciProvider());
            appendLine(text, "Pipeline", "#" + environment.pipelineId() + " " + environment.pipelineUrl());
            appendLine(text, "Job", environment.jobName() + " (#" + environment.jobId() + ") " + environment.jobUrl());
            appendLine(text, "Branch", environment.branch());
            appendLine(text, "Commit", environment.commit());
            appendLine(text, "Triggered by", environment.triggeredBy());
            appendLine(text, "Runner", environment.runner());
        } else {
            appendLine(text, "Branch", environment.branch());
            appendLine(text, "Ran on", localLine(environment));
        }
        if (reproduceCommand != null && !reproduceCommand.isBlank()) {
            text.append("\n*Reproduce:*\n`").append(reproduceCommand).append('`');
        }
        return text.toString();
    }

    /**
     * Builds a copy-paste-safe {@code K=V … ./gradlew run} command from the client's ordered run inputs. teswiz owns
     * only the shell-safety (and the {@code ./gradlew run} suffix), never which keys matter — the caller decides that
     * (e.g. a casino composer passes its {@code REGION/MARKET/BRAND/INTEGRATION/GAME/TAG/CONFIG} tuple), so the domain
     * vocabulary stays out of teswiz. Blank values are skipped; each value is POSIX-shell-quoted — left bare when it
     * has no shell metacharacters, single-quoted (with embedded single quotes escaped the standard {@code '\''} way)
     * when it contains whitespace or specials such as the spaces and parentheses in a Cucumber TAG.
     *
     * @param orderedInputs the run inputs in the order they should appear (use an insertion-ordered map); may be empty
     * @return the reproduce command string (never {@code null}); just {@code ./gradlew run} when no inputs are given
     */
    public static String reproduceCommand(Map<String, String> orderedInputs) {
        StringBuilder command = new StringBuilder();
        if (orderedInputs != null) {
            for (Map.Entry<String, String> input : orderedInputs.entrySet()) {
                String value = input.getValue();
                if (value != null && !value.isBlank()) {
                    command.append(input.getKey()).append('=').append(shellQuote(value.trim())).append(' ');
                }
            }
        }
        return command.append("./gradlew run").toString();
    }

    private static String shellQuote(String value) {
        if (!needsQuoting(value)) {
            return value;
        }
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static boolean needsQuoting(String value) {
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            boolean safe = Character.isLetterOrDigit(character) || "@_-./=:,".indexOf(character) >= 0;
            if (!safe) {
                return true;
            }
        }
        return false;
    }

    /**
     * Appends the trailing line separator to a composed Slack message so it reads as a distinct block in the channel.
     *
     * @param slackText the composed Slack message
     * @return the message with the trailing separator appended
     */
    public static String withSlackSeparator(String slackText) {
        return slackText + SLACK_SEPARATOR;
    }

    /**
     * Renders the full per-scenario breakdown as HTML — one {@code
     *
    <h3>} heading and a {@code status / scenario /
       * time} table per feature, in document order. A failed scenario shows its reason under the name; a scenario with
     * no recorded time shows an em dash. Returns an empty string when there are no scenarios.
     *
     * @param scenariosByFeature every scenario grouped by feature (from {@link JUnitResults#scenariosByFeature()})
     * @return the scenario-breakdown HTML, or an empty string
     */
    public static String scenarioTable(List<JUnitResults.FeatureScenarios> scenariosByFeature) {
        if (scenariosByFeature == null || scenariosByFeature.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<h3>Scenarios</h3>");
        for (JUnitResults.FeatureScenarios feature : scenariosByFeature) {
            html.append("<p><strong>").append(escape(feature.feature())).append("</strong></p>")
                    .append("<table class=\"scenarios\"><tr><th class=\"status\">Status</th><th>Scenario</th>")
                    .append("<th class=\"time\">Time</th></tr>");
            for (JUnitResults.Scenario scenario : feature.scenarios()) {
                html.append(scenarioRow(scenario));
            }
            html.append("</table>");
        }
        return html.toString();
    }

    /**
     * Renders the full per-scenario breakdown as Slack mrkdwn — a bold feature name then one blockquoted glyph line per
     * scenario (✅ pass, 🔴 fail, ⚪ skip), with the failure reason indented beneath a failed scenario and, when
     * requested, the per-scenario time appended. Returns an empty string when there are no scenarios.
     *
     * @param scenariosByFeature every scenario grouped by feature (from {@link JUnitResults#scenariosByFeature()})
     * @param showTime           whether to append each scenario's run time
     * @return the scenario-breakdown mrkdwn, or an empty string
     */
    public static String slackScenarios(List<JUnitResults.FeatureScenarios> scenariosByFeature, boolean showTime) {
        if (scenariosByFeature == null || scenariosByFeature.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (JUnitResults.FeatureScenarios feature : scenariosByFeature) {
            text.append("*").append(slackEscape(feature.feature())).append("*\n");
            for (JUnitResults.Scenario scenario : feature.scenarios()) {
                appendSlackScenario(text, scenario, showTime);
            }
        }
        return text.toString();
    }

    /**
     * Formats a duration in seconds for display — whole seconds under a minute (e.g. {@code 42.5s}), otherwise minutes
     * and seconds (e.g. {@code 3m 05s}). A non-positive duration renders as an em dash.
     *
     * @param seconds the duration in seconds
     * @return the formatted duration
     */
    public static String formatDuration(double seconds) {
        if (seconds <= 0) {
            return "\u2014";
        }
        if (seconds < 60) {
            return trimSeconds(seconds) + "s";
        }
        int minutes = (int) (seconds / 60);
        return minutes + "m " + String.format("%02d", Math.round(seconds - minutes * 60.0)) + "s";
    }

    /**
     * Escapes the five XML/HTML metacharacters so arbitrary scenario and feature names are safe inside HTML.
     *
     * @param raw the raw text
     * @return the escaped text
     */
    public static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
                "&#39;");
    }

    private static void appendLine(StringBuilder text, String label, String value) {
        if (value != null && !value.isBlank()) {
            text.append("• *").append(label).append(":* ").append(value).append('\n');
        }
    }

    private static String localLine(RunEnvironment environment) {
        StringBuilder local = new StringBuilder("local");
        if (environment.host() != null && !environment.host().isBlank()) {
            local.append(" · ").append(environment.host());
        }
        if (environment.user() != null && !environment.user().isBlank()) {
            local.append(" (").append(environment.user()).append(')');
        }
        return local.toString();
    }

    private static String ciRows(RunEnvironment environment) {
        return row("CI", escape(environment.ciProvider()))
                + row("Pipeline", link(environment.pipelineUrl(), "#" + environment.pipelineId()))
                + row("Job", link(environment.jobUrl(), environment.jobName() + " (#" + environment.jobId() + ")"))
                + row("Branch", escape(environment.branch())) + row("Commit", escape(environment.commit()))
                + row("Triggered by", escape(environment.triggeredBy())) + row("Runner", escape(environment.runner()));
    }

    private static String localRows(RunEnvironment environment) {
        return row("Ran on", "local") + row("Host", escape(environment.host()))
                + row("User", escape(environment.user())) + row("Branch", escape(environment.branch()));
    }

    private static String row(String label, String value) {
        return "<tr><th>" + escape(label) + "</th><td>" + value + "</td></tr>";
    }

    private static String link(String url, String text) {
        if (url == null || url.isBlank()) {
            return escape(text);
        }
        return "<a href=\"" + escape(url) + "\">" + escape(text) + "</a>";
    }

    private static String scenarioRow(JUnitResults.Scenario scenario) {
        StringBuilder cell = new StringBuilder("<td>").append(escape(scenario.name()));
        if (scenario.failed() && !scenario.reason().isBlank()) {
            cell.append("<span class=\"reason\">").append(escape(scenario.reason())).append("</span>");
        }
        cell.append("</td>");
        return "<tr><td class=\"status\">" + pill(scenario) + "</td>" + cell + "<td class=\"time\">"
                + formatDuration(scenario.time()) + "</td></tr>";
    }

    private static String pill(JUnitResults.Scenario scenario) {
        if (scenario.failed()) {
            return "<span class=\"pill fail\">FAIL</span>";
        }
        if (scenario.skipped()) {
            return "<span class=\"pill skip\">SKIP</span>";
        }
        return "<span class=\"pill pass\">PASS</span>";
    }

    private static void appendSlackScenario(StringBuilder text, JUnitResults.Scenario scenario, boolean showTime) {
        text.append("> ").append(glyph(scenario)).append("  ").append(slackEscape(scenario.name()));
        if (showTime && scenario.time() > 0) {
            text.append("  _").append(formatDuration(scenario.time())).append('_');
        }
        text.append('\n');
        if (scenario.failed() && !scenario.reason().isBlank()) {
            text.append(">     \u21b3 ").append(slackEscape(scenario.reason())).append('\n');
        }
    }

    private static String glyph(JUnitResults.Scenario scenario) {
        if (scenario.failed()) {
            return "\uD83D\uDD34";
        }
        if (scenario.skipped()) {
            return "\u26aa";
        }
        return "\u2705";
    }

    private static String slackEscape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String trimSeconds(double seconds) {
        String formatted = String.format("%.1f", seconds);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }
}
