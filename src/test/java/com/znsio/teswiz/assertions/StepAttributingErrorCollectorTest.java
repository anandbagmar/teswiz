package com.znsio.teswiz.assertions;

import com.znsio.teswiz.runner.CurrentStep;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.Setup;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StepAttributingErrorCollectorTest {

    private final long threadId = Thread.currentThread().getId();

    @BeforeEach
    void setUp() {
        CurrentStep.reset(threadId);
        Setup.load("./configs/teswiz/teswiz_config.properties.template");
    }

    @AfterEach
    void tearDown() {
        CurrentStep.remove(threadId);
    }

    @Test
    void collectorPrefixesMessageAndPreservesStackTraceAndCause() {
        CurrentStep.setStepText(threadId, "Given a player session is active");
        CurrentStep.advance(threadId);

        SoftAssertions softly = new SoftAssertions();
        softly.setDelegate(new StepAttributingErrorCollector(threadId));

        softly.assertThat(false).as("Bet should be accepted").isTrue();

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("[step 1: Given a player session is active]")
                .hasMessageContaining("[Bet should be accepted]")
                .hasMessageContaining("Expecting value to be true but was false");
    }

    @Test
    void twoIdenticalAssertionsFromDifferentStepsProduceDistinguishableMessages() {
        SoftAssertions softly = new SoftAssertions();
        softly.setDelegate(new StepAttributingErrorCollector(threadId));

        // Step 3
        CurrentStep.setStepText(threadId, "Then the bet is accepted");
        CurrentStep.advance(threadId);
        CurrentStep.advance(threadId);
        CurrentStep.advance(threadId);
        softly.assertThat(false).as("Bet should be accepted").isTrue();
        CurrentStep.clearStepText(threadId);

        // Step 7
        CurrentStep.setStepText(threadId, "Then the bet is accepted");
        CurrentStep.advance(threadId);
        CurrentStep.advance(threadId);
        CurrentStep.advance(threadId);
        CurrentStep.advance(threadId);
        softly.assertThat(false).as("Bet should be accepted").isTrue();
        CurrentStep.clearStepText(threadId);

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("[step 3: Then the bet is accepted]")
                .hasMessageContaining("[step 7: Then the bet is accepted]")
                .hasMessageContaining("[Bet should be accepted]");
    }

    @Test
    void assertAllStillThrowsAndListsEveryFailure() {
        SoftAssertions softly = new SoftAssertions();
        softly.setDelegate(new StepAttributingErrorCollector(threadId));

        CurrentStep.setStepText(threadId, "Step A");
        CurrentStep.advance(threadId);
        softly.assertThat(1).isEqualTo(2);

        CurrentStep.setStepText(threadId, "Step B");
        CurrentStep.advance(threadId);
        softly.assertThat("foo").isEqualTo("bar");

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("[step 1: Step A]")
                .hasMessageContaining("[step 2: Step B]");
    }

    @Test
    void noStepContextLeavesErrorUnchanged() {
        SoftAssertions softly = new SoftAssertions();
        softly.setDelegate(new StepAttributingErrorCollector(threadId));

        // Outside step context (CurrentStep describe is null)
        softly.assertThat(false).as("Outside step assertion").isTrue();

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("[Outside step assertion]")
                .extracting(Throwable::getMessage)
                .asString()
                .doesNotContain("[step ");
    }

    @Test
    void indexOnlyDegradedModeWhenStepTextMissing() {
        SoftAssertions softly = new SoftAssertions();
        softly.setDelegate(new StepAttributingErrorCollector(threadId));

        CurrentStep.advance(threadId);
        softly.assertThat(false).as("Index only test").isTrue();

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("[step 1] [Index only test]");
    }

    @Test
    void flagOffMatchesStandardFormat() {
        Setup.addBooleanValueToConfigs(Setup.STEP_ATTRIBUTION_ENABLED, false);

        CurrentStep.setStepText(threadId, "Then the bet is accepted");
        CurrentStep.advance(threadId);

        SoftAssertions softly = new SoftAssertions();
        if (Runner.isStepAttributionEnabled()) {
            softly.setDelegate(new StepAttributingErrorCollector(threadId));
        }

        softly.assertThat(false).as("Bet should be accepted").isTrue();

        assertThatThrownBy(softly::assertAll)
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("[Bet should be accepted]")
                .extracting(Throwable::getMessage)
                .asString()
                .doesNotContain("[step 1");
    }

    @Test
    void parallelScenarioRunIsThreadIsolated() throws Exception {
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 1; i <= threadCount; i++) {
            final int threadIndex = i;
            futures.add(executor.submit(() -> {
                long tId = Thread.currentThread().getId();
                CurrentStep.reset(tId);
                try {
                    CurrentStep.setStepText(tId, "Step text for worker " + threadIndex);
                    for (int s = 0; s < threadIndex; s++) {
                        CurrentStep.advance(tId);
                    }
                    readyLatch.countDown();
                    startLatch.await();

                    SoftAssertions softly = new SoftAssertions();
                    softly.setDelegate(new StepAttributingErrorCollector(tId));
                    softly.assertThat(false).as("Worker assertion " + threadIndex).isTrue();

                    try {
                        softly.assertAll();
                        return "";
                    } catch (AssertionError e) {
                        return e.getMessage();
                    }
                } finally {
                    CurrentStep.remove(tId);
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        for (int i = 1; i <= threadCount; i++) {
            String failureMessage = futures.get(i - 1).get();
            assertThat(failureMessage)
                    .contains("[step " + i + ": Step text for worker " + i + "]")
                    .contains("[Worker assertion " + i + "]");
        }

        executor.shutdown();
    }
}
