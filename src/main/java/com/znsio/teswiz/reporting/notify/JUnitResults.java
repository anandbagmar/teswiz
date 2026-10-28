package com.znsio.teswiz.reporting.notify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.znsio.teswiz.exceptions.NotificationException;

/**
 * A generic, immutable parse of a cucumber/JUnit {@code testsuite} XML — the counts (tests / passed / failed / errors /
 * skipped) and the failing cases grouped by their {@code classname} (feature) then {@code name} (scenario), in document
 * order. It is domain-agnostic: it carries no "tuple", label or wording — a consuming project reads these numbers and
 * groups and composes its own message. Only the machine-readable XML is read; nothing is sent here.
 */
public final class JUnitResults {

    // LogManager/Logger are referenced fully-qualified ON PURPOSE: an auto-import-organizer in this environment
    // rewrites a plain `import org.apache.logging.log4j.LogManager;` to `java.util.logging.LogManager` and drops the
    // Logger import, breaking the build. Fully-qualifying is the only form that survives it.
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager
            .getLogger(JUnitResults.class.getName());

    private static final String TESTSUITE_TAG = "testsuite";
    private static final String TESTCASE_TAG = "testcase";
    private static final String FAILURE_TAG = "failure";
    private static final String ERROR_TAG = "error";
    private static final String SKIPPED_TAG = "skipped";
    private static final String CLASSNAME_ATTRIBUTE = "classname";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String MESSAGE_ATTRIBUTE = "message";
    private static final String TIME_ATTRIBUTE = "time";
    private static final String TESTS_ATTRIBUTE = "tests";
    private static final String FAILURES_ATTRIBUTE = "failures";
    private static final String ERRORS_ATTRIBUTE = "errors";
    private static final String SKIPPED_ATTRIBUTE = "skipped";

    private final int totalTests;
    private final int failed;
    private final int errors;
    private final int skipped;
    private final double durationSeconds;
    private final List<FeatureFailures> failuresByFeature;
    private final List<FeatureScenarios> scenariosByFeature;

    private JUnitResults(int totalTests, int failed, int errors, int skipped, double durationSeconds,
            List<FeatureFailures> failuresByFeature, List<FeatureScenarios> scenariosByFeature) {
        this.totalTests = totalTests;
        this.failed = failed;
        this.errors = errors;
        this.skipped = skipped;
        this.durationSeconds = durationSeconds;
        this.failuresByFeature = List.copyOf(failuresByFeature);
        this.scenariosByFeature = List.copyOf(scenariosByFeature);
    }

    /**
     * Parses a JUnit XML into results.
     *
     * @param junitXml the path to the {@code cucumber-junit} XML
     * @return the parsed, immutable results
     */
    public static JUnitResults fromXml(Path junitXml) {
        Element suite = readTestsuite(junitXml);
        int totalTests = intAttribute(suite, TESTS_ATTRIBUTE);
        int failed = intAttribute(suite, FAILURES_ATTRIBUTE);
        int errors = intAttribute(suite, ERRORS_ATTRIBUTE);
        int skipped = intAttribute(suite, SKIPPED_ATTRIBUTE);
        double durationSeconds = doubleAttribute(suite, TIME_ATTRIBUTE);
        List<FeatureScenarios> scenarios = extractScenariosByFeature(suite);
        List<FeatureFailures> failures = extractFailuresByFeature(scenarios);
        LOGGER.debug("Parsed {} tests ({} failed, {} errors) in {}s from {}", totalTests, failed, errors,
                durationSeconds, junitXml);
        return new JUnitResults(totalTests, failed, errors, skipped, durationSeconds, failures, scenarios);
    }

    /**
     * @return the total number of scenarios in the suite
     */
    public int totalTests() {
        return totalTests;
    }

    /**
     * @return the number of passed scenarios (total minus failed, errored and skipped)
     */
    public int passed() {
        return Math.max(0, totalTests - failed - errors - skipped);
    }

    /**
     * @return the number of failed scenarios
     */
    public int failed() {
        return failed;
    }

    /**
     * @return the number of errored scenarios
     */
    public int errors() {
        return errors;
    }

    /**
     * @return the number of skipped scenarios
     */
    public int skipped() {
        return skipped;
    }

    /**
     * @return the total wall-clock run time of the suite in seconds (the {@code <testsuite time>} attribute), or
     *         {@code 0} when absent
     */
    public double durationSeconds() {
        return durationSeconds;
    }

    /**
     * @return {@code true} when any scenario failed or errored
     */
    public boolean hasFailures() {
        return failed > 0 || errors > 0;
    }

    /**
     * @return {@code true} when the suite ran no scenarios
     */
    public boolean isEmpty() {
        return totalTests == 0;
    }

    /**
     * @return the failing scenarios grouped by feature, in document order (features with no failures omitted)
     */
    public List<FeatureFailures> failuresByFeature() {
        return failuresByFeature;
    }

    /**
     * @return every scenario — passed, failed, errored and skipped — grouped by feature, in document order, each
     *         carrying its status, optional failure/skip reason and run time
     */
    public List<FeatureScenarios> scenariosByFeature() {
        return scenariosByFeature;
    }

    /**
     * Whether the XML at this path is present and parseable into a {@code <testsuite>}. Never throws — any problem
     * (missing, truncated, malformed) returns {@code false}. Lets a caller tell a file still being written from a
     * complete one.
     *
     * @param junitXml the path to the JUnit XML
     * @return {@code true} when the file parses to a {@code <testsuite>}
     */
    public static boolean isReadable(Path junitXml) {
        if (junitXml == null || !Files.exists(junitXml)) {
            return false;
        }
        try {
            return parse(junitXml).getElementsByTagName(TESTSUITE_TAG).getLength() > 0;
        } catch (NotificationException e) {
            return false;
        }
    }

    private static Element readTestsuite(Path junitXml) {
        Document document = parse(junitXml);
        NodeList suites = document.getElementsByTagName(TESTSUITE_TAG);
        if (suites.getLength() == 0) {
            throw new NotificationException("No <testsuite> element found in " + junitXml);
        }
        return (Element) suites.item(0);
    }

    private static Document parse(Path junitXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(junitXml.toFile());
            document.getDocumentElement().normalize();
            return document;
        } catch (Exception e) {
            throw new NotificationException("Failed to parse JUnit XML " + junitXml, e);
        }
    }

    private static List<FeatureScenarios> extractScenariosByFeature(Element suite) {
        Map<String, List<Scenario>> byFeature = new LinkedHashMap<>();
        NodeList testcases = suite.getElementsByTagName(TESTCASE_TAG);
        for (int i = 0; i < testcases.getLength(); i++) {
            Element testcase = (Element) testcases.item(i);
            String feature = testcase.getAttribute(CLASSNAME_ATTRIBUTE);
            byFeature.computeIfAbsent(feature, key -> new ArrayList<>()).add(toScenario(testcase));
        }
        List<FeatureScenarios> result = new ArrayList<>();
        for (Map.Entry<String, List<Scenario>> entry : byFeature.entrySet()) {
            result.add(new FeatureScenarios(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private static Scenario toScenario(Element testcase) {
        String name = testcase.getAttribute(NAME_ATTRIBUTE);
        double time = doubleAttribute(testcase, TIME_ATTRIBUTE);
        Element error = firstChild(testcase, ERROR_TAG);
        if (error != null) {
            return new Scenario(name, ScenarioStatus.ERRORED, reasonOf(error), time);
        }
        Element failure = firstChild(testcase, FAILURE_TAG);
        if (failure != null) {
            return new Scenario(name, ScenarioStatus.FAILED, reasonOf(failure), time);
        }
        Element skipped = firstChild(testcase, SKIPPED_TAG);
        if (skipped != null) {
            return new Scenario(name, ScenarioStatus.SKIPPED, reasonOf(skipped), time);
        }
        return new Scenario(name, ScenarioStatus.PASSED, "", time);
    }

    // The human-readable reason comes from the child element's `message` attribute (Cucumber writes it for a
    // <failure>/<error>; a bare <skipped/> usually has none, so this returns "" rather than fabricating one).
    private static String reasonOf(Element outcome) {
        String message = outcome.getAttribute(MESSAGE_ATTRIBUTE);
        return message == null ? "" : message.trim();
    }

    private static Element firstChild(Element testcase, String tag) {
        NodeList children = testcase.getElementsByTagName(tag);
        return children.getLength() == 0 ? null : (Element) children.item(0);
    }

    // The failures view is the scenario view filtered to failed/errored cases, so the two stay consistent by
    // construction and the existing failuresByFeature() API is unchanged for its callers.
    private static List<FeatureFailures> extractFailuresByFeature(List<FeatureScenarios> scenariosByFeature) {
        List<FeatureFailures> result = new ArrayList<>();
        for (FeatureScenarios feature : scenariosByFeature) {
            List<String> failingNames = new ArrayList<>();
            for (Scenario scenario : feature.scenarios()) {
                if (scenario.failed()) {
                    failingNames.add(scenario.name());
                }
            }
            if (!failingNames.isEmpty()) {
                result.add(new FeatureFailures(feature.feature(), failingNames));
            }
        }
        return result;
    }

    private static int intAttribute(Element element, String attribute) {
        String value = element.getAttribute(attribute);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static double doubleAttribute(Element element, String attribute) {
        String value = element.getAttribute(attribute);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * The outcome of a single scenario as recorded in the JUnit XML.
     */
    public enum ScenarioStatus {
        /** All steps passed. */
        PASSED,
        /** A business assertion failed (a {@code <failure>} child). */
        FAILED,
        /** An unexpected exception was thrown (an {@code <error>} child). */
        ERRORED,
        /** The scenario did not run (a {@code <skipped>} child). */
        SKIPPED
    }

    /**
     * A single scenario's outcome, in document order within its feature.
     *
     * @param name   the scenario name (JUnit {@code name})
     * @param status the outcome
     * @param reason the failure/skip reason (the {@code message} attribute), or an empty string when none
     * @param time   the scenario run time in seconds (the {@code time} attribute), or {@code 0} when absent
     */
    public record Scenario(String name, ScenarioStatus status, String reason, double time) {

        /** @return {@code true} when this scenario failed or errored */
        public boolean failed() {
            return status == ScenarioStatus.FAILED || status == ScenarioStatus.ERRORED;
        }

        /** @return {@code true} when this scenario was skipped */
        public boolean skipped() {
            return status == ScenarioStatus.SKIPPED;
        }

        /** @return {@code true} when this scenario passed */
        public boolean passed() {
            return status == ScenarioStatus.PASSED;
        }
    }

    /**
     * Every scenario for one feature, in document order.
     *
     * @param feature   the feature (JUnit {@code classname})
     * @param scenarios the scenarios, whatever their outcome
     */
    public record FeatureScenarios(String feature, List<Scenario> scenarios) {

        /**
         * Canonical constructor; defensively copies the scenario list.
         */
        public FeatureScenarios {
            scenarios = List.copyOf(scenarios);
        }
    }

    /**
     * The failing scenarios for one feature, in document order.
     *
     * @param feature   the feature (JUnit {@code classname})
     * @param scenarios the failing scenario names (JUnit {@code name})
     */
    public record FeatureFailures(String feature, List<String> scenarios) {

        /**
         * Canonical constructor; defensively copies the scenario list.
         */
        public FeatureFailures {
            scenarios = List.copyOf(scenarios);
        }
    }
}
