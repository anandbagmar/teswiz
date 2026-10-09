package com.znsio.teswiz.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Unit tests for {@link ElementWaiter} — the bounded, non-throwing visibility/presence/text waits.
 * <p>
 * The driver is mocked so the test is engine-agnostic (it exercises the Selenium
 * {@code WebDriverWait}/{@code ExpectedConditions} layer that every teswiz engine sits behind). The
 * key behaviour under test is that a condition that never becomes true returns {@code false} within
 * the bound rather than throwing {@code TimeoutException}.
 */
class ElementWaiterTest {

    private static final By LOCATOR = By.cssSelector("#thing");
    private static final int ONE_SECOND = 1;

    @Test
    void isElementVisible_returnsTrueWhenElementIsVisible() {
        WebDriver driver = mock(WebDriver.class);
        WebElement visible = mock(WebElement.class);
        when(visible.isDisplayed()).thenReturn(true);
        when(driver.findElement(LOCATOR)).thenReturn(visible);

        assertThat(new ElementWaiter(driver).isElementVisible(LOCATOR, ONE_SECOND)).isTrue();
    }

    @Test
    void isElementVisible_returnsFalseOnTimeoutInsteadOfThrowing() {
        WebDriver driver = mock(WebDriver.class);
        when(driver.findElement(LOCATOR)).thenThrow(new NoSuchElementException("absent"));

        assertThat(new ElementWaiter(driver).isElementVisible(LOCATOR, ONE_SECOND)).isFalse();
    }

    @Test
    void isElementPresentWithin_returnsTrueWhenPresent() {
        WebDriver driver = mock(WebDriver.class);
        when(driver.findElement(LOCATOR)).thenReturn(mock(WebElement.class));

        assertThat(new ElementWaiter(driver).isElementPresentWithin(LOCATOR, ONE_SECOND)).isTrue();
    }

    @Test
    void isElementPresentWithin_returnsFalseOnTimeout() {
        WebDriver driver = mock(WebDriver.class);
        when(driver.findElement(LOCATOR)).thenThrow(new NoSuchElementException("absent"));

        assertThat(new ElementWaiter(driver).isElementPresentWithin(LOCATOR, ONE_SECOND)).isFalse();
    }

    @Test
    void waitTillTextIsPresent_returnsTrueWhenTextMatches() {
        WebDriver driver = mock(WebDriver.class);
        WebElement element = mock(WebElement.class);
        when(element.getText()).thenReturn("Please wait");
        when(driver.findElement(LOCATOR)).thenReturn(element);

        assertThat(new ElementWaiter(driver).waitTillTextIsPresent(LOCATOR, "wait", ONE_SECOND)).isTrue();
    }

    @Test
    void waitTillTextIsPresent_returnsFalseOnTimeoutWhenTextNeverAppears() {
        WebDriver driver = mock(WebDriver.class);
        WebElement element = mock(WebElement.class);
        lenient().when(element.getText()).thenReturn("something else");
        when(driver.findElement(LOCATOR)).thenReturn(element);

        assertThat(new ElementWaiter(driver).waitTillTextIsPresent(LOCATOR, "wait", ONE_SECOND)).isFalse();
    }
}
