package com.znsio.teswiz.runner;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Bounded element synchronisation that answers a <em>boolean</em> without throwing on timeout.
 * <p>
 * teswiz's existing {@code Driver.waitTill*}/{@code waitForClickabilityOf} helpers are bounded but
 * <em>throw</em> {@link TimeoutException} when the condition is not met, and the instant
 * {@code isElementPresent}/{@code isElementDisplayed} checks do not wait at all — so a caller that
 * wants "is this element visible within N seconds, true/false" (e.g. an optional cookie banner that
 * slides in a moment after navigation) had no primitive to call. {@code ElementWaiter} fills that
 * gap.
 * <p>
 * It drives the single {@link WebDriver} behind each teswiz engine, using Selenium
 * {@link WebDriverWait}/{@link ExpectedConditions}, and takes {@link By}. {@code By} is the locator
 * currency for the Selenium and Appium engines; Playwright screens are authored natively (pw-java via
 * {@code context.page()}, pw-ts via {@code .screen.ts}) and do not use these {@code By} waits.
 */
public class ElementWaiter {

    private final WebDriver driver;

    ElementWaiter(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Whether an element matching {@code locator} becomes visible within {@code numberOfSecondsToWait}.
     * Returns {@code false} on timeout instead of throwing — safe for optional/transient elements.
     *
     * @param locator               the element locator (any {@link By}; Selenium/Appium engines)
     * @param numberOfSecondsToWait the bound, in seconds
     * @return {@code true} if visible within the bound, else {@code false}
     */
    public boolean isElementVisible(By locator, int numberOfSecondsToWait) {
        return waitForCondition(ExpectedConditions.visibilityOfElementLocated(locator), numberOfSecondsToWait);
    }

    /**
     * Whether an element matching {@code locator} becomes present in the DOM within
     * {@code numberOfSecondsToWait}. Returns {@code false} on timeout instead of throwing. This is the
     * bounded companion to the instant {@code Driver.isElementPresent(By)}.
     *
     * @param locator               the element locator (any {@link By})
     * @param numberOfSecondsToWait the bound, in seconds
     * @return {@code true} if present within the bound, else {@code false}
     */
    public boolean isElementPresentWithin(By locator, int numberOfSecondsToWait) {
        return waitForCondition(ExpectedConditions.presenceOfElementLocated(locator), numberOfSecondsToWait);
    }

    /**
     * Whether {@code text} appears in the element matching {@code locator} within
     * {@code numberOfSecondsToWait}. Returns {@code false} on timeout instead of throwing.
     *
     * @param locator               the element locator (any {@link By})
     * @param text                  the text expected to be present in the element
     * @param numberOfSecondsToWait the bound, in seconds
     * @return {@code true} if the text is present within the bound, else {@code false}
     */
    public boolean waitTillTextIsPresent(By locator, String text, int numberOfSecondsToWait) {
        return waitForCondition(ExpectedConditions.textToBePresentInElementLocated(locator, text),
                numberOfSecondsToWait);
    }

    private boolean waitForCondition(ExpectedCondition<?> condition, int numberOfSecondsToWait) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait)).until(condition);
            return true;
        } catch (TimeoutException timeout) {
            return false;
        }
    }
}
