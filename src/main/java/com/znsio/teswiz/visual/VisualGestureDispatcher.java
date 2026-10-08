package com.znsio.teswiz.visual;

import java.time.Duration;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.exceptions.UnsupportedVisualGestureException;

import io.appium.java_client.AppiumDriver;

/**
 * Dispatches coordinate-based visual gestures (click, double-click, hover, long-press, swipe, drag, enter text)
 * to whatever input mechanism the active inner driver supports. This is the single place that knows how a
 * visual gesture maps onto Appium touch, a browser's native coordinate input, Selenium {@link Actions}, or
 * synthesised DOM events - so {@code VisualElement} stays a pure value object and does not inspect the driver.
 *
 * <p>
 * Dispatch priority for each gesture: Appium touch, then browser {@link NativeCoordinateInput} (so gestures also
 * reach {@code <canvas>} content), then Selenium {@link Actions}, then synthesised DOM events as a last resort.
 * When a gesture is genuinely unsupported by the active driver, an {@link UnsupportedVisualGestureException} is
 * thrown rather than silently doing nothing, so a test fails loudly instead of passing against an unchanged app.
 *
 * <p>
 * Parity note: {@code click}, {@code doubleClick}, {@code hover} and {@code enterText} are supported on every
 * engine (Selenium, Playwright-Java, Playwright-TS, Appium). {@code swipe} and {@code longPress} require touch
 * (Appium) or native coordinate input (Playwright); on a plain Selenium web driver they are unsupported and
 * throw. {@code zoom}/{@code pinch} are not implemented on any engine and always throw.
 */
public final class VisualGestureDispatcher {
    private static final Logger LOGGER = LogManager.getLogger(VisualGestureDispatcher.class.getName());

    private static final int MINIMUM_SWIPE_DELTA = 50;
    private static final int DEFAULT_SWIPE_DELTA = 100;
    private static final Duration SWIPE_DURATION = Duration.ofMillis(600);
    private static final Duration MOBILE_DOUBLE_TAP_GAP = Duration.ofMillis(100);
    private static final Duration MOBILE_TAP_HOLD = Duration.ofMillis(100);

    private final WebDriver innerDriver;
    private final boolean appium;

    public VisualGestureDispatcher(WebDriver innerDriver, boolean appium) {
        this.innerDriver = innerDriver;
        this.appium = appium;
    }

    public void click(Point point, String label) {
        LOGGER.info("Clicking visual element '{}' at ({}, {})", label, point.getX(), point.getY());
        if (appiumDriver() != null) {
            performMobileTap(point);
            return;
        }
        if (nativeInput() != null) {
            nativeInput().clickAtViewportPoint(point.getX(), point.getY());
            return;
        }
        if (interactive() != null) {
            new Actions(innerDriver).moveToLocation(point.getX(), point.getY()).click().perform();
            return;
        }
        if (jsExecutor() != null) {
            warnSyntheticFallback("click", label);
            jsExecutor().executeScript(
                    "var el = document.elementFromPoint(arguments[0], arguments[1]); "
                            + "if (el) { "
                            + "  el.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                            + "  el.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                            + "  el.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                            + "  if (typeof el.click === 'function') { el.click(); } "
                            + "}",
                    point.getX(), point.getY());
            return;
        }
        throw unsupported("click", label);
    }

    public void doubleClick(Point point, String label) {
        LOGGER.info("Double-clicking visual element '{}' at ({}, {})", label, point.getX(), point.getY());
        if (appiumDriver() != null) {
            performMobileTap(point);
            sleepQuietly(MOBILE_DOUBLE_TAP_GAP);
            performMobileTap(point);
            return;
        }
        if (nativeInput() != null) {
            nativeInput().doubleClickAtViewportPoint(point.getX(), point.getY());
            return;
        }
        if (interactive() != null) {
            new Actions(innerDriver).moveToLocation(point.getX(), point.getY()).doubleClick().perform();
            return;
        }
        if (jsExecutor() != null) {
            warnSyntheticFallback("doubleClick", label);
            jsExecutor().executeScript(
                    "var el = document.elementFromPoint(arguments[0], arguments[1]); "
                            + "if (el) { "
                            + "  el.dispatchEvent(new MouseEvent('dblclick', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                            + "}",
                    point.getX(), point.getY());
            return;
        }
        throw unsupported("doubleClick", label);
    }

    public void hover(Point point, String label) {
        LOGGER.info("Hovering over visual element '{}' at ({}, {})", label, point.getX(), point.getY());
        if (nativeInput() != null) {
            nativeInput().hoverAtViewportPoint(point.getX(), point.getY());
            return;
        }
        if (interactive() != null) {
            new Actions(innerDriver).moveToLocation(point.getX(), point.getY()).perform();
            return;
        }
        if (jsExecutor() != null) {
            warnSyntheticFallback("hover", label);
            jsExecutor().executeScript(
                    "var el = document.elementFromPoint(arguments[0], arguments[1]); "
                            + "if (el) { "
                            + "  el.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                            + "  el.dispatchEvent(new MouseEvent('mouseover', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                            + "}",
                    point.getX(), point.getY());
            return;
        }
        throw unsupported("hover", label);
    }

    public void enterText(Point point, String label, CharSequence... keysToSend) {
        click(point, label);
        String keys = String.join("", keysToSend);
        LOGGER.info("Sending keys '{}' to visual element '{}'", keys, label);
        if (interactive() != null) {
            try {
                new Actions(innerDriver).sendKeys(keysToSend).perform();
                return;
            } catch (RuntimeException e) {
                LOGGER.debug("Actions sendKeys failed, trying activeElement fallback: {}", e.getMessage());
            }
        }
        try {
            innerDriver.switchTo().activeElement().sendKeys(keys);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not send keys to activeElement: {}", e.getMessage());
            throw unsupported("enterText", label);
        }
    }

    public void longPress(Point point, Duration duration, String label) {
        LOGGER.info("Long-pressing visual element '{}' at ({}, {}) for {} ms", label, point.getX(), point.getY(), duration.toMillis());
        if (appiumDriver() != null) {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence sequence = new Sequence(finger, 1);
            sequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), point.getX(), point.getY()));
            sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            sequence.addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), point.getX(), point.getY()));
            sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            appiumDriver().perform(List.of(sequence));
            return;
        }
        if (nativeInput() != null) {
            nativeInput().longPressAtViewportPoint(point.getX(), point.getY(), duration);
            return;
        }
        if (interactive() != null) {
            new Actions(innerDriver).moveToLocation(point.getX(), point.getY()).clickAndHold().pause(duration).release().perform();
            return;
        }
        throw unsupported("longPress", label);
    }

    public void swipe(Point center, int width, int height, Direction direction, String label) {
        LOGGER.info("Swiping '{}' on visual element '{}' at ({}, {})", direction, label, center.getX(), center.getY());
        Point end = swipeEndPoint(center, width, height, direction);
        if (appiumDriver() != null) {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipeSequence = new Sequence(finger, 1);
            swipeSequence.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), center.getX(), center.getY()));
            swipeSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipeSequence.addAction(finger.createPointerMove(SWIPE_DURATION, PointerInput.Origin.viewport(), end.getX(), end.getY()));
            appiumDriver().perform(List.of(swipeSequence));
            return;
        }
        if (nativeInput() != null) {
            nativeInput().dragFromViewportPoint(center.getX(), center.getY(), end.getX(), end.getY());
            return;
        }
        throw unsupported("swipe", label);
    }

    public void dragAndDropTo(Point source, WebElement target, String label) {
        LOGGER.info("Dragging visual element '{}' from ({}, {}) to target element", label, source.getX(), source.getY());
        if (nativeInput() != null) {
            Point targetCenter = elementCenter(target);
            nativeInput().dragFromViewportPoint(source.getX(), source.getY(), targetCenter.getX(), targetCenter.getY());
            return;
        }
        if (interactive() != null) {
            new Actions(innerDriver).moveToLocation(source.getX(), source.getY()).clickAndHold().moveToElement(target).release().perform();
            return;
        }
        if (jsExecutor() != null) {
            warnSyntheticFallback("dragAndDropTo", label);
            jsExecutor().executeScript(
                    "var source = document.elementFromPoint(arguments[0], arguments[1]); "
                            + "if (source && arguments[2]) { "
                            + "  source.dispatchEvent(new MouseEvent('dragstart', {bubbles: true})); "
                            + "  arguments[2].dispatchEvent(new MouseEvent('drop', {bubbles: true})); "
                            + "  source.dispatchEvent(new MouseEvent('dragend', {bubbles: true})); "
                            + "}",
                    source.getX(), source.getY(), target);
            return;
        }
        throw unsupported("dragAndDropTo", label);
    }

    public void zoom(double scaleFactor, String label) {
        throw new UnsupportedVisualGestureException(String.format(
                "Zoom (scale: %.2f) on visual element '%s' is not implemented on any engine", scaleFactor, label));
    }

    public void pinch(double scaleFactor, String label) {
        throw new UnsupportedVisualGestureException(String.format(
                "Pinch (scale: %.2f) on visual element '%s' is not implemented on any engine", scaleFactor, label));
    }

    private void performMobileTap(Point point) {
        PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
        Sequence tap = new Sequence(touch, 1);
        tap.addAction(touch.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), point.getX(), point.getY()))
                .addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(touch, MOBILE_TAP_HOLD))
                .addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        appiumDriver().perform(List.of(tap));
    }

    private Point swipeEndPoint(Point center, int width, int height, Direction direction) {
        int deltaX = swipeDelta(width);
        int deltaY = swipeDelta(height);
        return switch (direction) {
            case UP -> new Point(center.getX(), center.getY() - deltaY);
            case DOWN -> new Point(center.getX(), center.getY() + deltaY);
            case LEFT -> new Point(center.getX() - deltaX, center.getY());
            case RIGHT -> new Point(center.getX() + deltaX, center.getY());
        };
    }

    private int swipeDelta(int elementSize) {
        int delta = elementSize / 2;
        return delta < MINIMUM_SWIPE_DELTA ? DEFAULT_SWIPE_DELTA : delta;
    }

    private AppiumDriver appiumDriver() {
        return appium && innerDriver instanceof AppiumDriver appiumDriver ? appiumDriver : null;
    }

    private NativeCoordinateInput nativeInput() {
        return innerDriver instanceof NativeCoordinateInput nativeInput ? nativeInput : null;
    }

    private Interactive interactive() {
        return innerDriver instanceof Interactive interactiveDriver ? interactiveDriver : null;
    }

    private JavascriptExecutor jsExecutor() {
        return innerDriver instanceof JavascriptExecutor js ? js : null;
    }

    private UnsupportedVisualGestureException unsupported(String gesture, String label) {
        return new UnsupportedVisualGestureException(String.format(
                "Cannot perform '%s' on visual element '%s': the active driver supports no touch, native "
                        + "coordinate input, Selenium Actions, or JavascriptExecutor mechanism for this gesture",
                gesture, label));
    }

    private void warnSyntheticFallback(String gesture, String label) {
        LOGGER.warn(
                "Performing '{}' on visual element '{}' via synthesised DOM events: this driver exposes no native "
                        + "coordinate input, so the action will NOT reach <canvas> content", gesture, label);
    }

    private static Point elementCenter(WebElement element) {
        Point location = element.getLocation();
        org.openqa.selenium.Dimension size = element.getSize();
        return new Point(location.getX() + size.getWidth() / 2, location.getY() + size.getHeight() / 2);
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
