package com.znsio.teswiz.reporting.notify;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.znsio.teswiz.exceptions.NotificationException;

/**
 * A generic, immutable parse of a cucumber/JUnit {@code testsuite} XML — the counts (tests / passed / failed / errors /
 * skipped) and the failing cases grouped by their {@code classname} (feature) then {@code name} (scenario), in
 * document order. It is domain-agnostic: it carries no "tuple", label or wording — a consuming project reads these
 * numbers and groups and composes its own message. Only the machine-readable XML is read; nothing is sent here.
 */
public final class JUnitResults {

    private static final Logger LOGGER = LogManager.getLogger(JUnitResults.class.getName());

    private static final String TESTSUITE_TAG = "testsuite";
    private static final String TESTCASE_TAG = "testcase";
    private static final String FAILURE_TAG = "failure";
    private static final String ERROR_TAG = "error";
    private static final String CLASSNAME_ATTRIBUTE = "classname";
    private static final String NAME_ATTRIBUTE = "name";
    private static final String TESTS_ATTRIBUTE = "tests";
    private static final String FAILURES_ATTRIBUTE = "failures";
    private static final String ERRORS_ATTRIBUTE = "errors";
    private static final String SKIPPED_ATTRIBUTE = "skipped";

    private final int totalTests;
    private final int failed;
    private final int errors;
    private final int skipped;
    private final List<FeatureFailures> failuresByFeature;

    private JUnitResults(int totalTests, int failed, int errors, int skipped,
            List<FeatureFailures> failuresByFeature) {
        this.totalTests = totalTests;
        this.failed = failed;
        this.errors = errors;
        this.skipped = skipped;
        this.failuresByFeature = List.copyOf(failuresByFeature);
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
        List<FeatureFailures> failures = extractFailuresByFeature(suite);
        LOGGER.debug("Parsed {} tests ({} failed, {} errors) from {}", totalTests, failed, errors, junitXml);
        return new JUnitResults(totalTests, failed, errors, skipped, failures);
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

    private static List<FeatureFailures> extractFailuresByFeature(Element suite) {
        Map<String, List<String>> byFeature = new LinkedHashMap<>();
        NodeList testcases = suite.getElementsByTagName(TESTCASE_TAG);
        for (int i = 0; i < testcases.getLength(); i++) {
            Element testcase = (Element) testcases.item(i);
            if (isFailure(testcase)) {
                String feature = testcase.getAttribute(CLASSNAME_ATTRIBUTE);
                String scenario = testcase.getAttribute(NAME_ATTRIBUTE);
                byFeature.computeIfAbsent(feature, key -> new ArrayList<>()).add(scenario);
            }
        }
        List<FeatureFailures> result = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byFeature.entrySet()) {
            result.add(new FeatureFailures(entry.getKey(), List.copyOf(entry.getValue())));
        }
        return result;
    }

    private static boolean isFailure(Element testcase) {
        return testcase.getElementsByTagName(FAILURE_TAG).getLength() > 0
                || testcase.getElementsByTagName(ERROR_TAG).getLength() > 0;
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
