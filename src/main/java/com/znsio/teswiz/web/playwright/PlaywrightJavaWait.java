package com.znsio.teswiz.web.playwright;

import java.time.Duration;
import java.time.Instant;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Native Playwright bounded waits over a {@link Locator}. These are the engine-native replacements
 * for the Selenium {@code By}-based waits in {@code ElementWaiter}/{@code Driver}, so that Playwright
 * screens never route element waits through the removed {@code By} bridge. Each boolean wait returns
 * {@code false} on timeout instead of throwing, matching {@code ElementWaiter} semantics.
 */
final class PlaywrightJavaWait {
    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);

    private PlaywrightJavaWait() {
    }

    static int untilCountAtLeast(Locator locator, Duration timeout, int expectedMinimum) {
        Instant deadline = Instant.now().plus(timeout);
        int currentCount = locator.count();
        while (currentCount < expectedMinimum && Instant.now().isBefore(deadline)) {
            sleep();
            currentCount = locator.count();
        }
        return currentCount;
    }

    /**
     * Whether {@code locator} becomes visible within {@code timeout}. Native replacement for
     * {@code ElementWaiter.isElementVisible(By, int)} on Playwright. Returns {@code false} on timeout.
     */
    static boolean untilVisible(Locator locator, Duration timeout) {
        return waitForState(locator, WaitForSelectorState.VISIBLE, timeout);
    }

    /**
     * Whether {@code locator} becomes attached to the DOM within {@code timeout}. Native replacement
     * for {@code ElementWaiter.isElementPresentWithin(By, int)} on Playwright. Returns {@code false}
     * on timeout.
     */
    static boolean untilPresent(Locator locator, Duration timeout) {
        return waitForState(locator, WaitForSelectorState.ATTACHED, timeout);
    }

    /**
     * Whether {@code locator} becomes hidden (or detached) within {@code timeout}. Returns
     * {@code false} on timeout.
     */
    static boolean untilHidden(Locator locator, Duration timeout) {
        return waitForState(locator, WaitForSelectorState.HIDDEN, timeout);
    }

    private static boolean waitForState(Locator locator, WaitForSelectorState state, Duration timeout) {
        try {
            locator.first().waitFor(new Locator.WaitForOptions()
                    .setState(state)
                    .setTimeout((double) timeout.toMillis()));
            return true;
        } catch (PlaywrightException timeoutOrError) {
            return false;
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(POLL_INTERVAL.toMillis());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Playwright Java element lookup",
                    interruptedException);
        }
    }
}
