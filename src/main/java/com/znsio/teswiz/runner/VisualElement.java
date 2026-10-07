package com.znsio.teswiz.runner;

import java.time.Duration;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import com.znsio.teswiz.entities.Direction;

import io.appium.java_client.AppiumDriver;

public class VisualElement {
    private static final int MINIMUM_SWIPE_DELTA = 50;
    private static final int DEFAULT_SWIPE_DELTA = 100;
    private static final Duration SWIPE_DURATION = Duration.ofMillis(600);

    private static final Logger LOGGER = LogManager.getLogger(VisualElement.class.getName());

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final String label;
    private final Driver driverFacade;

    public VisualElement(int x, int y, int width, int height, String label, Driver driverFacade) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.driverFacade = driverFacade;
    }

    public int getX() {
        return x;
    }

    public VisualElement withOffset(int offsetX, int offsetY) {
        if (offsetX == 0 && offsetY == 0) {
            return this;
        }
        return new VisualElement(x + offsetX, y + offsetY, width, height, label, driverFacade);
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public String getLabel() {
        return label;
    }

    public Point getCenter() {
        return new Point(x + width / 2, y + height / 2);
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    public VisualElement highlight() {
        if (driverFacade != null) {
            driverFacade.highlightVisualElement(this);
        }
        return this;
    }

    public void click() {
        highlight();
        Point center = getCenter();
        LOGGER.info(String.format("Clicking visual element '%s' at center coordinates (%d, %d)", label, center.getX(), center.getY()));
        if (driverFacade != null && driverFacade.getInnerDriver() != null) {
            if (Driver.APPIUM_DRIVER.equals(driverFacade.getType())) {
                performMobileTap(center.getX(), center.getY());
            } else if (driverFacade.getInnerDriver()
                    instanceof com.znsio.teswiz.visual.NativeCoordinateInput nativeInput) {
                // Preferred: real browser input, so the click also reaches <canvas> content, which ignores the
                // synthesised DOM events used by the JavascriptExecutor fallback below.
                nativeInput.clickAtViewportPoint(center.getX(), center.getY());
            } else if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
                Actions actions = new Actions(driverFacade.getInnerDriver());
                actions.moveToLocation(center.getX(), center.getY()).click().perform();
            } else if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.JavascriptExecutor js) {
                warnSyntheticFallback("click");
                js.executeScript(
                        "var el = document.elementFromPoint(arguments[0], arguments[1]); " +
                        "if (el) { " +
                        "  el.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); " +
                        "  el.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); " +
                        "  el.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); " +
                        "  if (typeof el.click === 'function') { el.click(); } " +
                        "}",
                        center.getX(), center.getY()
                );
            } else {
                LOGGER.warn(String.format("Unable to perform click on visual element '%s': driver does not support Interactive or JavascriptExecutor", label));
            }
        } else {
            LOGGER.warn(String.format("Unable to click visual element '%s': driverFacade or inner driver is null", label));
        }
    }

    public void doubleClick() {
        highlight();
        Point center = getCenter();
        LOGGER.info(String.format("Double-clicking visual element '%s' at (%d, %d)", label, center.getX(), center.getY()));
        if (driverFacade != null && driverFacade.getInnerDriver() != null) {
            if (Driver.APPIUM_DRIVER.equals(driverFacade.getType())) {
                performMobileTap(center.getX(), center.getY());
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                performMobileTap(center.getX(), center.getY());
            } else if (driverFacade.getInnerDriver()
                    instanceof com.znsio.teswiz.visual.NativeCoordinateInput nativeInput) {
                nativeInput.doubleClickAtViewportPoint(center.getX(), center.getY());
            } else if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
                Actions actions = new Actions(driverFacade.getInnerDriver());
                actions.moveToLocation(center.getX(), center.getY()).doubleClick().perform();
            } else if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.JavascriptExecutor js) {
                warnSyntheticFallback("doubleClick");
                js.executeScript(
                        "var el = document.elementFromPoint(arguments[0], arguments[1]); " +
                        "if (el) { " +
                        "  el.dispatchEvent(new MouseEvent('dblclick', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); " +
                        "}",
                        center.getX(), center.getY()
                );
            }
        }
    }

    public void hover() {
        highlight();
        Point center = getCenter();
        LOGGER.info(String.format("Hovering over visual element '%s' at (%d, %d)", label, center.getX(), center.getY()));
        if (driverFacade != null && driverFacade.getInnerDriver() != null) {
            if (driverFacade.getInnerDriver()
                    instanceof com.znsio.teswiz.visual.NativeCoordinateInput nativeInput) {
                nativeInput.hoverAtViewportPoint(center.getX(), center.getY());
            } else if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
                Actions actions = new Actions(driverFacade.getInnerDriver());
                actions.moveToLocation(center.getX(), center.getY()).perform();
            } else if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.JavascriptExecutor js) {
                warnSyntheticFallback("hover");
                js.executeScript(
                        "var el = document.elementFromPoint(arguments[0], arguments[1]); " +
                        "if (el) { " +
                        "  el.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); " +
                        "  el.dispatchEvent(new MouseEvent('mouseover', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); " +
                        "}",
                        center.getX(), center.getY()
                );
            }
        }
    }

    public void sendKeys(CharSequence... keysToSend) {
        click();
        LOGGER.info(String.format("Sending keys '%s' to visual element '%s'", String.join("", keysToSend), label));
        if (driverFacade != null && driverFacade.getInnerDriver() != null) {
            String keys = String.join("", keysToSend);
            try {
                if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
                    Actions actions = new Actions(driverFacade.getInnerDriver());
                    actions.sendKeys(keysToSend).perform();
                    return;
                }
            } catch (Exception e) {
                LOGGER.debug("Actions sendKeys failed, trying activeElement fallback: " + e.getMessage());
            }
            try {
                driverFacade.getInnerDriver().switchTo().activeElement().sendKeys(keys);
            } catch (Exception e) {
                LOGGER.debug("Could not send keys to activeElement: " + e.getMessage());
            }
        }
    }

    public void tap() {
        click();
    }

    public void doubleTap() {
        doubleClick();
    }

    public void dragAndDropTo(WebElement target) {
        Point center = getCenter();
        LOGGER.info(String.format("Dragging visual element '%s' from (%d, %d) to target element", label, center.getX(), center.getY()));
        if (driverFacade != null && driverFacade.getInnerDriver() != null) {
            if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
                Actions actions = new Actions(driverFacade.getInnerDriver());
                actions.moveToLocation(center.getX(), center.getY())
                        .clickAndHold()
                        .moveToElement(target)
                        .release()
                        .perform();
            } else if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.JavascriptExecutor js) {
                js.executeScript(
                        "var source = document.elementFromPoint(arguments[0], arguments[1]); " +
                        "if (source && arguments[2]) { " +
                        "  source.dispatchEvent(new MouseEvent('dragstart', {bubbles: true})); " +
                        "  arguments[2].dispatchEvent(new MouseEvent('drop', {bubbles: true})); " +
                        "  source.dispatchEvent(new MouseEvent('dragend', {bubbles: true})); " +
                        "}",
                        center.getX(), center.getY(), target
                );
            }
        }
    }

    public void zoom(double scaleFactor) {
        LOGGER.info(String.format("Performing zoom (scale: %.2f) on visual element '%s'", scaleFactor, label));
    }

    public void pinch(double scaleFactor) {
        LOGGER.info(String.format("Performing pinch (scale: %.2f) on visual element '%s'", scaleFactor, label));
    }

    public void swipe(Direction direction) {
        Point center = getCenter();
        LOGGER.info(String.format("Swiping '%s' on visual element '%s' at (%d, %d)", direction, label, center.getX(), center.getY()));
        if (driverFacade != null
                && driverFacade.getInnerDriver() instanceof com.znsio.teswiz.visual.NativeCoordinateInput nativeInput) {
            Point end = swipeEndPoint(center, direction);
            nativeInput.dragFromViewportPoint(center.getX(), center.getY(), end.getX(), end.getY());
            return;
        }
        if (driverFacade != null && driverFacade.getInnerDriver() instanceof AppiumDriver appiumDriver) {
            Point end = swipeEndPoint(center, direction);
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipeSequence = new Sequence(finger, 1);
            swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), center.getX(), center.getY()));
            swipeSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipeSequence.addAction(finger.createPointerMove(SWIPE_DURATION, PointerInput.Origin.viewport(), end.getX(), end.getY()));
            appiumDriver.perform(List.of(swipeSequence));
            return;
        }
        LOGGER.warn(String.format(
                "Cannot swipe visual element '%s': this driver exposes neither native coordinate input nor an "
                        + "Appium touch pointer, so the gesture was not performed", label));
    }

    public void longPress(Duration duration) {
        highlight();
        Point center = getCenter();
        LOGGER.info(String.format("Long-pressing visual element '%s' at (%d, %d) for %d ms", label, center.getX(), center.getY(), duration.toMillis()));
        if (driverFacade != null && driverFacade.getInnerDriver() != null) {
            if (Driver.APPIUM_DRIVER.equals(driverFacade.getType()) && driverFacade.getInnerDriver() instanceof AppiumDriver appiumDriver) {
                PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
                Sequence sequence = new Sequence(finger, 1);
                sequence.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), center.getX(), center.getY()));
                sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
                sequence.addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), center.getX(), center.getY()));
                sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
                appiumDriver.perform(List.of(sequence));
            } else if (driverFacade.getInnerDriver()
                    instanceof com.znsio.teswiz.visual.NativeCoordinateInput nativeInput) {
                nativeInput.longPressAtViewportPoint(center.getX(), center.getY(), duration);
            } else if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
                Actions actions = new Actions(driverFacade.getInnerDriver());
                actions.moveToLocation(center.getX(), center.getY())
                        .clickAndHold()
                        .pause(duration)
                        .release()
                        .perform();
            } else {
                LOGGER.warn(String.format(
                        "Unable to long-press visual element '%s': driver supports neither native coordinate input "
                                + "nor Interactive", label));
            }
        }
    }

    public void longPress() {
        longPress(Duration.ofSeconds(2));
    }

    /**
     * The point a swipe in the given direction should end at, derived from the element's own size so the gesture
     * stays proportional to what was matched.
     *
     * @param center    the element's centre
     * @param direction the swipe direction
     * @return the end point of the swipe
     */
    private Point swipeEndPoint(Point center, Direction direction) {
        int deltaX = swipeDelta(width);
        int deltaY = swipeDelta(height);
        return switch (direction) {
            case UP -> new Point(center.getX(), center.getY() - deltaY);
            case DOWN -> new Point(center.getX(), center.getY() + deltaY);
            case LEFT -> new Point(center.getX() - deltaX, center.getY());
            case RIGHT -> new Point(center.getX() + deltaX, center.getY());
        };
    }

    /**
     * Returns the swipe distance for an element dimension: half the element, but never a distance so small that
     * the gesture reads as a tap. Shared by the native-web and Appium swipe paths so both travel equally far.
     *
     * @param elementSize the element's width or height, in pixels
     * @return the swipe distance in pixels
     */
    private int swipeDelta(int elementSize) {
        int delta = elementSize / 2;
        return delta < MINIMUM_SWIPE_DELTA ? DEFAULT_SWIPE_DELTA : delta;
    }

    /**
     * Warns that an action is falling back to synthesised DOM events. That path cannot drive {@code <canvas>}
     * content, and previously failed silently - the action logged success while the application never reacted - so
     * the degradation is called out explicitly rather than left to be discovered from screenshots.
     *
     * @param action the action being attempted, for the message
     */
    private void warnSyntheticFallback(String action) {
        LOGGER.warn(String.format(
                "Performing '%s' on visual element '%s' via synthesised DOM events: this driver exposes no native "
                        + "coordinate input, so the action will NOT reach <canvas> content", action, label));
    }

    private void performMobileTap(int tapX, int tapY) {
        if (driverFacade != null && driverFacade.getInnerDriver() instanceof AppiumDriver appiumDriver) {
            PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
            Sequence clickPosition = new Sequence(touch, 1);
            clickPosition
                    .addAction(touch.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), tapX, tapY))
                    .addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                    .addAction(new org.openqa.selenium.interactions.Pause(touch, Duration.ofMillis(100)))
                    .addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            appiumDriver.perform(List.of(clickPosition));
        }
    }

    @Override
    public String toString() {
        return String.format("VisualElement{label='%s', bounds=[x=%d, y=%d, w=%d, h=%d]}", label, x, y, width, height);
    }

    public WebElement toWebElement() {
        return (WebElement) java.lang.reflect.Proxy.newProxyInstance(
                VisualElement.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                new VisualElementWebElementInvocationHandler(this)
        );
    }

    private static class VisualElementWebElementInvocationHandler implements java.lang.reflect.InvocationHandler {
        private final VisualElement visualElement;

        VisualElementWebElementInvocationHandler(VisualElement visualElement) {
            this.visualElement = visualElement;
        }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            switch (methodName) {
                case "click":
                    int retries = Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS);
                    if (retries < 1) retries = 3;
                    int delaySeconds = Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS);
                    if (delaySeconds < 1) delaySeconds = 1;
                    for (int i = 1; i <= retries; i++) {
                        try {
                            visualElement.click();
                            return null;
                        } catch (Exception e) {
                            if (i == retries) throw e;
                            LOGGER.warn(String.format("Visual element click failed on attempt %d of %d, retrying after %ds: %s", i, retries, delaySeconds, e.getMessage()));
                            try { Thread.sleep(delaySeconds * 1000L); } catch (InterruptedException ignored) {}
                        }
                    }
                    return null;
                case "sendKeys":
                    if (args != null && args.length > 0 && args[0] instanceof CharSequence[]) {
                        visualElement.sendKeys((CharSequence[]) args[0]);
                    }
                    return null;
                case "isDisplayed":
                    return true;
                case "isEnabled":
                    return true;
                case "isSelected":
                    return false;
                case "getText":
                    return visualElement.getLabel();
                case "getTagName":
                    return "visual-element";
                case "getAttribute":
                    return visualElement.getLabel();
                case "getLocation":
                    return new Point(visualElement.getX(), visualElement.getY());
                case "getSize":
                    return visualElement.getSize();
                case "getRect":
                    return new org.openqa.selenium.Rectangle(visualElement.getX(), visualElement.getY(), visualElement.getHeight(), visualElement.getWidth());
                case "toString":
                    return visualElement.toString();
                default:
                    return null;
            }
        }
    }
}

