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
    private static final Duration MOBILE_DOUBLE_TAP_GAP = Duration.ofMillis(100);

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

    private void clearHighlight() {
        if (driverFacade != null) {
            driverFacade.clearHighlight();
        }
    }

    public void click() {
        highlight();
        try {
            performClick();
        } finally {
            clearHighlight();
        }
    }

    private void performClick() {
        Point center = getCenter();
        LOGGER.info("Clicking visual element '{}' at center coordinates ({}, {})", label, center.getX(), center.getY());
        if (!hasInnerDriver()) {
            warnNotPerformed("click");
            return;
        }
        if (isAppium()) {
            performMobileTap(center.getX(), center.getY());
            return;
        }
        // Preferred: real browser input, so the click also reaches <canvas> content, which ignores the
        // synthesised DOM events used by the JavascriptExecutor fallback below.
        if (nativeInput() != null) {
            nativeInput().clickAtViewportPoint(center.getX(), center.getY());
            return;
        }
        if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
            new Actions(driverFacade.getInnerDriver()).moveToLocation(center.getX(), center.getY()).click().perform();
            return;
        }
        if (jsExecutor() != null) {
            warnSyntheticFallback("click");
            jsExecutor().executeScript("var el = document.elementFromPoint(arguments[0], arguments[1]); " + "if (el) { "
                    + "  el.dispatchEvent(new MouseEvent('mousedown', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                    + "  el.dispatchEvent(new MouseEvent('mouseup', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                    + "  el.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                    + "  if (typeof el.click === 'function') { el.click(); } " + "}", center.getX(), center.getY());
            return;
        }
        warnNotPerformed("click");
    }

    public void doubleClick() {
        highlight();
        try {
            performDoubleClick();
        } finally {
            clearHighlight();
        }
    }

    private void performDoubleClick() {
        Point center = getCenter();
        LOGGER.info("Double-clicking visual element '{}' at ({}, {})", label, center.getX(), center.getY());
        if (!hasInnerDriver()) {
            warnNotPerformed("doubleClick");
            return;
        }
        if (isAppium()) {
            performMobileTap(center.getX(), center.getY());
            sleepQuietly(MOBILE_DOUBLE_TAP_GAP);
            performMobileTap(center.getX(), center.getY());
            return;
        }
        if (nativeInput() != null) {
            nativeInput().doubleClickAtViewportPoint(center.getX(), center.getY());
            return;
        }
        if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
            new Actions(driverFacade.getInnerDriver()).moveToLocation(center.getX(), center.getY()).doubleClick()
                    .perform();
            return;
        }
        if (jsExecutor() != null) {
            warnSyntheticFallback("doubleClick");
            jsExecutor().executeScript("var el = document.elementFromPoint(arguments[0], arguments[1]); " + "if (el) { "
                    + "  el.dispatchEvent(new MouseEvent('dblclick', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                    + "}", center.getX(), center.getY());
            return;
        }
        warnNotPerformed("doubleClick");
    }

    public void hover() {
        highlight();
        try {
            performHover();
        } finally {
            clearHighlight();
        }
    }

    private void performHover() {
        Point center = getCenter();
        LOGGER.info("Hovering over visual element '{}' at ({}, {})", label, center.getX(), center.getY());
        if (!hasInnerDriver()) {
            warnNotPerformed("hover");
            return;
        }
        if (nativeInput() != null) {
            nativeInput().hoverAtViewportPoint(center.getX(), center.getY());
            return;
        }
        if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
            new Actions(driverFacade.getInnerDriver()).moveToLocation(center.getX(), center.getY()).perform();
            return;
        }
        if (jsExecutor() != null) {
            warnSyntheticFallback("hover");
            jsExecutor().executeScript("var el = document.elementFromPoint(arguments[0], arguments[1]); " + "if (el) { "
                    + "  el.dispatchEvent(new MouseEvent('mousemove', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                    + "  el.dispatchEvent(new MouseEvent('mouseover', {bubbles: true, cancelable: true, clientX: arguments[0], clientY: arguments[1]})); "
                    + "}", center.getX(), center.getY());
            return;
        }
        warnNotPerformed("hover");
    }

    public void sendKeys(CharSequence... keysToSend) {
        highlight();
        try {
            performSendKeys(keysToSend);
        } finally {
            clearHighlight();
        }
    }

    private void performSendKeys(CharSequence... keysToSend) {
        click();
        String keys = String.join("", keysToSend);
        LOGGER.info("Sending keys '{}' to visual element '{}'", keys, label);
        if (!hasInnerDriver()) {
            warnNotPerformed("sendKeys");
            return;
        }
        try {
            if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
                new Actions(driverFacade.getInnerDriver()).sendKeys(keysToSend).perform();
                return;
            }
        } catch (RuntimeException e) {
            LOGGER.debug("Actions sendKeys failed, trying activeElement fallback: {}", e.getMessage());
        }
        try {
            driverFacade.getInnerDriver().switchTo().activeElement().sendKeys(keys);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not send keys to activeElement: {}", e.getMessage());
        }
    }

    public void tap() {
        click();
    }

    public void doubleTap() {
        doubleClick();
    }

    public void dragAndDropTo(WebElement target) {
        try {
            performDragAndDropTo(target);
        } finally {
            clearHighlight();
        }
    }

    private void performDragAndDropTo(WebElement target) {
        Point center = getCenter();
        LOGGER.info("Dragging visual element '{}' from ({}, {}) to target element", label, center.getX(),
                center.getY());
        if (!hasInnerDriver()) {
            warnNotPerformed("dragAndDropTo");
            return;
        }
        if (nativeInput() != null) {
            Point targetCenter = elementCenter(target);
            nativeInput().dragFromViewportPoint(center.getX(), center.getY(), targetCenter.getX(), targetCenter.getY());
            return;
        }
        if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
            new Actions(driverFacade.getInnerDriver()).moveToLocation(center.getX(), center.getY()).clickAndHold()
                    .moveToElement(target).release().perform();
            return;
        }
        if (jsExecutor() != null) {
            warnSyntheticFallback("dragAndDropTo");
            jsExecutor().executeScript(
                    "var source = document.elementFromPoint(arguments[0], arguments[1]); "
                            + "if (source && arguments[2]) { "
                            + "  source.dispatchEvent(new MouseEvent('dragstart', {bubbles: true})); "
                            + "  arguments[2].dispatchEvent(new MouseEvent('drop', {bubbles: true})); "
                            + "  source.dispatchEvent(new MouseEvent('dragend', {bubbles: true})); " + "}",
                    center.getX(), center.getY(), target);
            return;
        }
        warnNotPerformed("dragAndDropTo");
    }

    public void zoom(double scaleFactor) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(String.format(
                    "Zoom (scale: %.2f) on visual element '%s' is not implemented; the gesture was not performed",
                    scaleFactor, label));
        }
    }

    public void pinch(double scaleFactor) {
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn(String.format(
                    "Pinch (scale: %.2f) on visual element '%s' is not implemented; the gesture was not performed",
                    scaleFactor, label));
        }
    }

    public void swipe(Direction direction) {
        try {
            performSwipe(direction);
        } finally {
            clearHighlight();
        }
    }

    private void performSwipe(Direction direction) {
        Point center = getCenter();
        LOGGER.info("Swiping '{}' on visual element '{}' at ({}, {})", direction, label, center.getX(), center.getY());
        if (!hasInnerDriver()) {
            warnNotPerformed("swipe");
            return;
        }
        Point end = swipeEndPoint(center, direction);
        if (isAppium()) {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipeSequence = new Sequence(finger, 1);
            swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(),
                    center.getX(), center.getY()));
            swipeSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipeSequence.addAction(
                    finger.createPointerMove(SWIPE_DURATION, PointerInput.Origin.viewport(), end.getX(), end.getY()));
            appiumDriver().perform(List.of(swipeSequence));
            return;
        }
        if (nativeInput() != null) {
            nativeInput().dragFromViewportPoint(center.getX(), center.getY(), end.getX(), end.getY());
            return;
        }
        warnNotPerformed("swipe");
    }

    public void longPress(Duration duration) {
        highlight();
        try {
            performLongPress(duration);
        } finally {
            clearHighlight();
        }
    }

    private void performLongPress(Duration duration) {
        Point center = getCenter();
        LOGGER.info("Long-pressing visual element '{}' at ({}, {}) for {} ms", label, center.getX(), center.getY(),
                duration.toMillis());
        if (!hasInnerDriver()) {
            warnNotPerformed("longPress");
            return;
        }
        if (isAppium()) {
            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence sequence = new Sequence(finger, 1);
            sequence.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(),
                    center.getX(), center.getY()));
            sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            sequence.addAction(
                    finger.createPointerMove(duration, PointerInput.Origin.viewport(), center.getX(), center.getY()));
            sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            appiumDriver().perform(List.of(sequence));
            return;
        }
        if (nativeInput() != null) {
            nativeInput().longPressAtViewportPoint(center.getX(), center.getY(), duration);
            return;
        }
        if (driverFacade.getInnerDriver() instanceof org.openqa.selenium.interactions.Interactive) {
            new Actions(driverFacade.getInnerDriver()).moveToLocation(center.getX(), center.getY()).clickAndHold()
                    .pause(duration).release().perform();
            return;
        }
        warnNotPerformed("longPress");
    }

    public void longPress() {
        longPress(Duration.ofSeconds(2));
    }

    /**
     * The point a swipe in the given direction should end at, derived from the element's own size so the gesture stays
     * proportional to what was matched.
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
     * Returns the swipe distance for an element dimension: half the element, but never a distance so small that the
     * gesture reads as a tap. Shared by the native-web and Appium swipe paths so both travel equally far.
     *
     * @param elementSize the element's width or height, in pixels
     * @return the swipe distance in pixels
     */
    private int swipeDelta(int elementSize) {
        int delta = elementSize / 2;
        return delta < MINIMUM_SWIPE_DELTA ? DEFAULT_SWIPE_DELTA : delta;
    }

    /**
     * Warns that an action is falling back to synthesised DOM events. That path cannot drive {@code <canvas>} content,
     * and previously failed silently - the action logged success while the application never reacted - so the
     * degradation is called out explicitly rather than left to be discovered from screenshots.
     *
     * @param action the action being attempted, for the message
     */
    private void warnSyntheticFallback(String action) {
        LOGGER.warn(
                "Performing '{}' on visual element '{}' via synthesised DOM events: this driver exposes no native coordinate input, so the action will NOT reach <canvas> content",
                action, label);
    }

    /**
     * Warns that an action could not be performed because the current driver supports none of the required input
     * mechanisms. Keeps every gesture consistent: an unsupported action is always reported, never silently dropped.
     *
     * @param action the action that could not be performed, for the message
     */
    private void warnNotPerformed(String action) {
        LOGGER.warn(
                "Unable to perform '{}' on visual element '{}': driver or inner driver is null, or supports no known input mechanism",
                action, label);
    }

    private boolean hasInnerDriver() {
        return driverFacade != null && driverFacade.getInnerDriver() != null;
    }

    private boolean isAppium() {
        return Driver.APPIUM_DRIVER.equals(driverFacade.getType())
                && driverFacade.getInnerDriver() instanceof AppiumDriver;
    }

    private AppiumDriver appiumDriver() {
        return (AppiumDriver) driverFacade.getInnerDriver();
    }

    private com.znsio.teswiz.visual.NativeCoordinateInput nativeInput() {
        return driverFacade.getInnerDriver() instanceof com.znsio.teswiz.visual.NativeCoordinateInput nativeInput
                ? nativeInput
                : null;
    }

    private org.openqa.selenium.JavascriptExecutor jsExecutor() {
        return driverFacade.getInnerDriver() instanceof org.openqa.selenium.JavascriptExecutor js ? js : null;
    }

    private static Point elementCenter(WebElement element) {
        Point location = element.getLocation();
        Dimension size = element.getSize();
        return new Point(location.getX() + size.getWidth() / 2, location.getY() + size.getHeight() / 2);
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void performMobileTap(int tapX, int tapY) {
        if (driverFacade != null && driverFacade.getInnerDriver() instanceof AppiumDriver appiumDriver) {
            PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
            Sequence clickPosition = new Sequence(touch, 1);
            clickPosition
                    .addAction(
                            touch.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), tapX, tapY))
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
        return (WebElement) java.lang.reflect.Proxy.newProxyInstance(VisualElement.class.getClassLoader(),
                new Class<?>[] { WebElement.class }, new VisualElementWebElementInvocationHandler(this));
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
                    if (retries < 1)
                        retries = 3;
                    int delaySeconds = Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS);
                    if (delaySeconds < 1)
                        delaySeconds = 1;
                    for (int i = 1; i <= retries; i++) {
                        try {
                            visualElement.click();
                            return null;
                        } catch (Exception e) {
                            if (i == retries)
                                throw e;
                            LOGGER.warn("Visual element click failed on attempt {} of {}, retrying after {}s: {}", i,
                                    retries, delaySeconds, e.getMessage());
                            sleepQuietly(Duration.ofSeconds(delaySeconds));
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
                    return new org.openqa.selenium.Rectangle(visualElement.getX(), visualElement.getY(),
                            visualElement.getHeight(), visualElement.getWidth());
                case "toString":
                    return visualElement.toString();
                default:
                    return null;
            }
        }
    }
}
