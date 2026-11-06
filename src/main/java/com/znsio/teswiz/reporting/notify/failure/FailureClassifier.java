package com.znsio.teswiz.reporting.notify.failure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;

/**
 * Works out why one failed scenario failed — see {@link FailureCause}.
 * <p>
 * The <b>type</b> is the simple class name of the exception from the JUnit {@code type} attribute. The message alone
 * cannot give it: a message such as {@code Index 0 out of bounds for length 0} never names its exception.
 * <p>
 * The <b>location</b> is where the failure came from, so that many scenarios dying on one line read as one cause:
 * <ul>
 * <li>for an ordinary exception, the first stack frame that belongs to the code under test rather than to the JDK or a
 * well-known library (the first frame at all if every frame is a library one);</li>
 * <li>for an AssertJ soft-assertion wrapper, the assertion's own origin line from its message — the wrapper is thrown
 * from the after-scenario hook, so its stack points at the framework, not at the assertion.</li>
 * </ul>
 * Only the outer exception's frames are read; a {@code Caused by:} section is ignored.
 */
public final class FailureClassifier {

    /** The type given to a failure whose JUnit element carries no exception class. */
    public static final String UNCLASSIFIED = "Unclassified";

    private static final String SOFT_ASSERTION_WRAPPER = "AssertJMultipleFailuresError";
    private static final String CAUSED_BY = "Caused by:";
    private static final String SUPPRESSED = "Suppressed:";

    // The Gherkin step a soft-assertion message tags each failure with, written by CurrentStep.describe as
    // "[step 4: And the player's balance is reduced]". Group 1 is the step text without the "step N:" prefix.
    private static final Pattern SOFT_ASSERTION_STEP = Pattern.compile("\\[step\\s+\\d+:\\s*(.*?)\\s*\\]");

    // Cucumber writes the running step into the stack trace as a pseudo-frame "at ✽.<step text>(file:///...feature:n)".
    // Group 1 is the step text. The star (U+273D) is how Cucumber marks the Gherkin step line.
    private static final Pattern CUCUMBER_STEP_FRAME = Pattern.compile("\\bat\\s+\u273d\\.(.*?)\\(file:");

    // Packages whose frames are infrastructure, not the code under test. A frame from one of these is skipped when
    // looking for the place a failure came from.
    private static final List<String> LIBRARY_PACKAGES = List.of("java.", "javax.", "jdk.", "sun.", "com.sun.",
            "org.junit.", "org.opentest4j.", "io.cucumber.", "org.assertj.", "org.apache.", "org.mockito.",
            "net.bytebuddy.", "com.fasterxml.", "com.microsoft.playwright.", "org.openqa.selenium.", "io.restassured.",
            "io.appium.");

    // One stack frame: "at [module/]package.Class.method(File.java:123)". The line number is optional because native
    // and unknown-source frames have none. Matched anywhere in the text, not as a whole line: an XML parser
    // normalises the line breaks inside an attribute value to spaces, so a soft-assertion message read from the JUnit
    // `message` attribute arrives as a single line with its "at Class.method(File.java:n)" origin embedded in it.
    private static final Pattern FRAME = Pattern
            .compile("\\bat\\s+(?:[^\\s/()]+/)?([\\w.$]+)\\.([\\w$<>]+)\\(([^():]*)(?::(\\d+))?\\)");

    private FailureClassifier() {
    }

    /**
     * Classifies a failed scenario.
     *
     * @param scenario a failed or errored scenario
     * @return its cause; never null
     */
    public static FailureCause classify(Scenario scenario) {
        return new FailureCause(typeOf(scenario.type()), locationOf(scenario), stepOf(scenario));
    }

    private static String typeOf(String fullyQualifiedType) {
        String simple = simpleName(fullyQualifiedType);
        return simple.isEmpty() ? UNCLASSIFIED : simple;
    }

    private static String simpleName(String fullyQualifiedType) {
        if (fullyQualifiedType == null || fullyQualifiedType.isBlank()) {
            return "";
        }
        String name = fullyQualifiedType.trim();
        name = name.substring(name.lastIndexOf('.') + 1);
        int nested = name.lastIndexOf('$');
        boolean namedNestedClass = nested >= 0 && nested + 1 < name.length()
                && Character.isJavaIdentifierStart(name.charAt(nested + 1));
        return namedNestedClass ? name.substring(nested + 1) : name;
    }

    private static String locationOf(Scenario scenario) {
        if (SOFT_ASSERTION_WRAPPER.equals(simpleName(scenario.type()))) {
            return firstFrameIn(scenario.reason()).map(Frame::label).orElse("");
        }
        List<Frame> frames = framesOf(outerSection(scenario.trace()));
        return firstApplicationFrame(frames).map(Frame::label).orElse("");
    }

    // The Gherkin step running when the scenario failed, read from whichever source carries it: a soft-assertion
    // message tags it as "[step N: ...]"; an ordinary failure's trace carries Cucumber's "at ✽.<step>(file:...)"
    // pseudo-frame. When neither is present (a truncated trace, an error with no step line, an unclassified failure)
    // the step is unknown and the empty string, so the cause degrades to grouping by type and place alone.
    private static String stepOf(Scenario scenario) {
        return firstMatch(SOFT_ASSERTION_STEP, scenario.reason())
                .or(() -> firstMatch(CUCUMBER_STEP_FRAME, scenario.trace())).orElse("");
    }

    // The first capturing-group-1 match of the pattern in the text, trimmed; empty when the text is null or no match.
    private static Optional<String> firstMatch(Pattern pattern, String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? Optional.of(matcher.group(1).trim()).filter(step -> !step.isEmpty()) : Optional.empty();
    }

    // The trace up to the first "Caused by:" / "Suppressed:" line — the outer exception's own frames.
    private static String outerSection(String trace) {
        StringBuilder outer = new StringBuilder();
        for (String line : lines(trace)) {
            String trimmed = line.trim();
            if (trimmed.startsWith(CAUSED_BY) || trimmed.startsWith(SUPPRESSED)) {
                break;
            }
            outer.append(line).append('\n');
        }
        return outer.toString();
    }

    private static Optional<Frame> firstApplicationFrame(List<Frame> frames) {
        return frames.stream().filter(frame -> !frame.isLibrary()).findFirst().or(() -> frames.stream().findFirst());
    }

    private static Optional<Frame> firstFrameIn(String text) {
        return framesOf(text).stream().findFirst();
    }

    private static List<Frame> framesOf(String text) {
        List<Frame> frames = new ArrayList<>();
        if (text == null) {
            return frames;
        }
        Matcher matcher = FRAME.matcher(text);
        while (matcher.find()) {
            frames.add(new Frame(matcher.group(1), matcher.group(2), matcher.group(4)));
        }
        return frames;
    }

    private static String[] lines(String text) {
        return text == null ? new String[0] : text.split("\\R");
    }

    /** One parsed stack frame: the declaring class (as written), the method and the optional line number. */
    private record Frame(String className, String method, String line) {

        boolean isLibrary() {
            return LIBRARY_PACKAGES.stream().anyMatch(className::startsWith);
        }

        String label() {
            String simpleClass = className.substring(className.lastIndexOf('.') + 1);
            return simpleClass + "." + method + (line == null ? "" : ":" + line);
        }
    }
}
