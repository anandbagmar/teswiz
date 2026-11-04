package com.znsio.teswiz.reporting.notify.failure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.reporting.notify.JUnitResults.Scenario;
import com.znsio.teswiz.reporting.notify.JUnitResults.ScenarioStatus;

/**
 * {@link FailureClassifier} answers "what kind of failure was this, and where did it come from?" for one failed
 * scenario. The kind is the simple name of the exception class from the JUnit {@code type} attribute; the place is the
 * first frame of the stack that belongs to the code under test rather than to the JDK or a library, so that eight
 * scenarios dying on the same line read as one cause. A soft-assertion wrapper has a useless stack (it is thrown from
 * the after-scenario hook), so for that one the place is the assertion's own origin line from the message. The shapes
 * below are copied from a real STAGE run.
 */
class FailureClassifierTest {

    private static final String INDEX_TRACE = """
            java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0
            \tat java.base/jdk.internal.util.Preconditions.outOfBounds(Preconditions.java:64)
            \tat java.base/java.util.ArrayList.get(ArrayList.java:427)
            \tat com.entain.casino.businessLayer.domain.RewardsBL.awardFreeSpins(RewardsBL.java:113)
            \tat com.entain.casino.glue.steps.domain.RewardsSteps.thePlayerIsAwardedFreeSpins(RewardsSteps.java:32)
            \tat ✽.the player is awarded free spins(file:///builds/freespin.feature:30)
            """;

    private static final String ASSERTJ_MESSAGE = """

            Multiple Failures (1 failure)
            -- failure 1 --
            [step 4: And the player's balance is reduced] [Balance should be reduced after the bet (pre-operation 1018.49)]
            Expecting actual:
              1018.49
            to be less than:
              1018.49
            at BalanceTracking.assertReduced(BalanceTracking.java:111)""";

    private static final String ASSERTJ_TRACE = ASSERTJ_MESSAGE + """

            \tat com.znsio.teswiz.steps.Hooks.afterScenario(Hooks.java:82)
            \tat com.znsio.teswiz.steps.RunCukes.afterScenario(RunCukes.java:40)
            """;

    @Test
    @DisplayName("the type is the simple class name from the fully-qualified type attribute")
    void typeIsTheSimpleClassName() {
        FailureCause cause = FailureClassifier.classify(failed("java.lang.IndexOutOfBoundsException", "m", ""));

        assertThat(cause.type()).isEqualTo("IndexOutOfBoundsException");
    }

    @Test
    @DisplayName("a type with no package is kept as is")
    void typeWithoutPackageIsKept() {
        assertThat(FailureClassifier.classify(failed("AssertionError", "m", "")).type()).isEqualTo("AssertionError");
    }

    @Test
    @DisplayName("a nested class keeps its own name, not the outer class")
    void nestedClassKeepsItsOwnName() {
        FailureCause cause = FailureClassifier.classify(failed("com.x.Outer$InnerFailure", "m", ""));

        assertThat(cause.type()).isEqualTo("InnerFailure");
    }

    @Test
    @DisplayName("an empty type is Unclassified")
    void emptyTypeIsUnclassified() {
        assertThat(FailureClassifier.classify(failed("", "m", "")).type()).isEqualTo(FailureClassifier.UNCLASSIFIED);
    }

    @Test
    @DisplayName("the place is the first frame of the code under test, skipping JDK frames and the module prefix")
    void placeIsTheFirstApplicationFrame() {
        FailureCause cause = FailureClassifier.classify(failed("java.lang.IndexOutOfBoundsException", "m",
                INDEX_TRACE));

        assertThat(cause.location()).isEqualTo("RewardsBL.awardFreeSpins:113");
    }

    @Test
    @DisplayName("library frames such as Playwright are skipped to reach the project's own frame")
    void libraryFramesAreSkipped() {
        String trace = """
                com.microsoft.playwright.impl.TargetClosedError: closed
                \tat com.microsoft.playwright.impl.Connection.dispatch(Connection.java:100)
                \tat com.microsoft.playwright.impl.PageImpl.screenshot(PageImpl.java:55)
                \tat com.entain.casino.screen.GamePlayScreen.spin(GamePlayScreen.java:396)
                """;

        FailureCause cause = FailureClassifier.classify(failed("com.microsoft.playwright.impl.TargetClosedError",
                "closed", trace));

        assertThat(cause.location()).isEqualTo("GamePlayScreen.spin:396");
    }

    @Test
    @DisplayName("only the outer exception's frames count; a Caused by section is ignored")
    void causedBySectionIsIgnored() {
        String trace = """
                com.x.Outer: wrapper
                \tat com.x.Service.run(Service.java:10)
                Caused by: java.io.IOException: boom
                \tat com.x.Inner.read(Inner.java:99)
                """;

        assertThat(FailureClassifier.classify(failed("com.x.Outer", "wrapper", trace)).location())
                .isEqualTo("Service.run:10");
    }

    @Test
    @DisplayName("a soft-assertion wrapper takes its place from the assertion's own origin line, not the hook frames")
    void softAssertionWrapperUsesTheMessageOrigin() {
        FailureCause cause = FailureClassifier.classify(failed("org.assertj.core.error.AssertJMultipleFailuresError",
                ASSERTJ_MESSAGE, ASSERTJ_TRACE));

        assertThat(cause.type()).isEqualTo("AssertJMultipleFailuresError");
        assertThat(cause.location()).isEqualTo("BalanceTracking.assertReduced:111");
    }

    @Test
    @DisplayName("a soft-assertion wrapper with no origin line has no place rather than a misleading hook frame")
    void softAssertionWrapperWithoutOriginHasNoPlace() {
        String message = "\nMultiple Failures (1 failure)\n-- failure 1 --\n[step 1: Given x] [desc]\nExpecting true";
        FailureCause cause = FailureClassifier.classify(failed("org.assertj.core.error.AssertJMultipleFailuresError",
                message, "\tat com.znsio.teswiz.steps.Hooks.afterScenario(Hooks.java:82)"));

        assertThat(cause.location()).isEmpty();
    }

    @Test
    @DisplayName("a trace with only library frames falls back to its first frame")
    void libraryOnlyTraceFallsBackToFirstFrame() {
        String trace = "java.lang.IllegalStateException: x\n\tat java.base/java.util.ArrayList.get(ArrayList.java:427)\n";

        assertThat(FailureClassifier.classify(failed("java.lang.IllegalStateException", "x", trace)).location())
                .isEqualTo("ArrayList.get:427");
    }

    @Test
    @DisplayName("native and unknown-source frames have no line number")
    void framesWithoutALineNumberOmitIt() {
        String nativeFrame = "x\n\tat com.x.Runner.invoke(Native Method)\n";
        String unknownFrame = "x\n\tat com.x.Runner.invoke(Unknown Source)\n";

        assertThat(FailureClassifier.classify(failed("com.x.E", "x", nativeFrame)).location())
                .isEqualTo("Runner.invoke");
        assertThat(FailureClassifier.classify(failed("com.x.E", "x", unknownFrame)).location())
                .isEqualTo("Runner.invoke");
    }

    @Test
    @DisplayName("a constructor frame keeps its <init> name")
    void constructorFrameKeepsInit() {
        String trace = "x\n\tat com.x.Widget.<init>(Widget.java:7)\n";

        assertThat(FailureClassifier.classify(failed("com.x.E", "x", trace)).location()).isEqualTo("Widget.<init>:7");
    }

    @Test
    @DisplayName("no trace means no place")
    void noTraceMeansNoPlace() {
        assertThat(FailureClassifier.classify(failed("com.x.E", "x", "")).location()).isEmpty();
    }

    @Test
    @DisplayName("lines that are not stack frames are ignored")
    void nonFrameLinesAreIgnored() {
        String trace = "com.x.E: x\nsome free text\n\tat ✽.a step(file:///f.feature:3)\n\tat com.x.Real.go(Real.java:5)\n";

        assertThat(FailureClassifier.classify(failed("com.x.E", "x", trace)).location()).isEqualTo("Real.go:5");
    }

    @Test
    @DisplayName("the label joins type and place, or is just the type when there is no place")
    void labelJoinsTypeAndPlace() {
        assertThat(new FailureCause("IndexOutOfBoundsException", "RewardsBL.awardFreeSpins:113").label())
                .isEqualTo("IndexOutOfBoundsException @ RewardsBL.awardFreeSpins:113");
        assertThat(new FailureCause("Unclassified", "").label()).isEqualTo("Unclassified");
    }

    private static Scenario failed(String type, String reason, String trace) {
        return new Scenario("scenario", ScenarioStatus.FAILED, reason, 0.1, type, trace);
    }
}
