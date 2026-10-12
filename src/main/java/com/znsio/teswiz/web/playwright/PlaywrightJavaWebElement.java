package com.znsio.teswiz.web.playwright;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;
import com.microsoft.playwright.Locator;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.options.BoundingBox;

final class PlaywrightJavaWebElement implements WebElement {
    private final Locator locator;
    private final Duration implicitWaitTimeout;

    PlaywrightJavaWebElement(Locator locator, Duration implicitWaitTimeout) {
        this.locator = locator;
        this.implicitWaitTimeout = implicitWaitTimeout;
    }

    private void highlight() {
        PlaywrightHighlighter.highlight(locator);
    }

    @Override
    public void click() {
        highlight();
        locator.click();
    }

    @Override
    public void submit() {
        click();
    }

    @Override
    public void sendKeys(CharSequence... keysToSend) {
        highlight();
        String value = java.util.Arrays.stream(keysToSend).map(String::valueOf).collect(Collectors.joining());
        if (isFileInput(value)) {
            locator.setInputFiles(Path.of(value));
            return;
        }
        locator.pressSequentially(value);
    }

    @Override
    public void clear() {
        highlight();
        locator.clear();
    }

    @Override
    public String getTagName() {
        Object tagName = locator.evaluate("element => element.tagName.toLowerCase()");
        return String.valueOf(tagName);
    }

    @Override
    public String getAttribute(String name) {
        return locator.getAttribute(name);
    }

    @Override
    public String getDomAttribute(String name) {
        return getAttribute(name);
    }

    @Override
    public String getDomProperty(String name) {
        Object value = locator.evaluate("(args) => args[0][args[1]]", List.of(elementHandle(), name));
        return null == value ? null : String.valueOf(value);
    }

    @Override
    public String getText() {
        String text = locator.innerText();
        if (null == text || text.isBlank()) {
            text = locator.textContent();
        }
        return null == text ? "" : text.trim();
    }

    // Nested element location by Selenium By is unsupported on Playwright Java; author screens natively
    // via context.page().locator(...) from PlaywrightJavaScreenContext (the By bridge was removed in Phase 7).
    @Override
    public List<WebElement> findElements(By by) {
        throw byLocatorUnsupported("findElements(By)");
    }

    @Override
    public WebElement findElement(By by) {
        throw byLocatorUnsupported("findElement(By)");
    }

    private UnsupportedOperationException byLocatorUnsupported(String operation) {
        return new UnsupportedOperationException(com.znsio.teswiz.web.WebEngineCapabilities.formatDiagnosticMessage(
                com.znsio.teswiz.web.WebEngine.PLAYWRIGHT_JAVA,
                com.znsio.teswiz.web.WebCapability.ELEMENT_LOCATION_BY_SELENIUM_LOCATOR, operation));
    }

    @Override
    public SearchContext getShadowRoot() {
        throw new UnsupportedOperationException(com.znsio.teswiz.web.WebEngineCapabilities.formatDiagnosticMessage(
                com.znsio.teswiz.web.WebEngine.PLAYWRIGHT_JAVA, com.znsio.teswiz.web.WebCapability.SHADOW_DOM,
                "getShadowRoot"));
    }

    @Override
    public boolean isSelected() {
        try {
            return locator.isChecked();
        } catch (RuntimeException ignored) {
            Object selected = locator.evaluate("element => element.matches(':checked')");
            return Boolean.TRUE.equals(selected);
        }
    }

    @Override
    public boolean isEnabled() {
        return locator.isEnabled();
    }

    @Override
    public boolean isDisplayed() {
        return locator.isVisible();
    }

    @Override
    public Point getLocation() {
        Rectangle rectangle = getRect();
        return new Point(rectangle.getX(), rectangle.getY());
    }

    @Override
    public Dimension getSize() {
        Rectangle rectangle = getRect();
        return new Dimension(rectangle.getWidth(), rectangle.getHeight());
    }

    @Override
    public Rectangle getRect() {
        BoundingBox boundingBox = locator.boundingBox();
        if (null == boundingBox) {
            return new Rectangle(0, 0, 0, 0);
        }
        return new Rectangle((int) Math.round(boundingBox.x), (int) Math.round(boundingBox.y),
                (int) Math.round(boundingBox.width), (int) Math.round(boundingBox.height));
    }

    @Override
    public String getCssValue(String propertyName) {
        Object value = locator.evaluate("(args) => getComputedStyle(args[0]).getPropertyValue(args[1])",
                List.of(elementHandle(), propertyName));
        return null == value ? "" : String.valueOf(value);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> target) {
        return target.convertFromPngBytes(locator.screenshot());
    }

    Locator locator() {
        return locator;
    }

    ElementHandle elementHandle() {
        return locator.elementHandle();
    }

    private boolean isFileInput(String value) {
        if (!"input".equalsIgnoreCase(getTagName())) {
            return false;
        }
        if (!"file".equalsIgnoreCase(getAttribute("type"))) {
            return false;
        }
        return Files.exists(Path.of(value));
    }
}
