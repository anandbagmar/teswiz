package com.znsio.teswiz.runner;

import java.time.Duration;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.entities.Platform;

import io.appium.java_client.AppiumDriver;

public class VisualElement {
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
            } else {
                Actions actions = new Actions(driverFacade.getInnerDriver());
                actions.moveToLocation(center.getX(), center.getY()).click().perform();
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
            } else {
                Actions actions = new Actions(driverFacade.getInnerDriver());
                actions.moveToLocation(center.getX(), center.getY()).doubleClick().perform();
            }
        }
    }

    public void hover() {
        highlight();
        Point center = getCenter();
        LOGGER.info(String.format("Hovering over visual element '%s' at (%d, %d)", label, center.getX(), center.getY()));
        if (driverFacade != null && driverFacade.getInnerDriver() != null) {
            Actions actions = new Actions(driverFacade.getInnerDriver());
            actions.moveToLocation(center.getX(), center.getY()).perform();
        }
    }

    public void sendKeys(CharSequence... keysToSend) {
        click();
        LOGGER.info(String.format("Sending keys '%s' to visual element '%s'", String.join("", keysToSend), label));
        if (driverFacade != null && driverFacade.getInnerDriver() != null) {
            Actions actions = new Actions(driverFacade.getInnerDriver());
            actions.sendKeys(keysToSend).perform();
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
            Actions actions = new Actions(driverFacade.getInnerDriver());
            actions.moveToLocation(center.getX(), center.getY())
                    .clickAndHold()
                    .moveToElement(target)
                    .release()
                    .perform();
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
        if (driverFacade != null && driverFacade.getInnerDriver() instanceof AppiumDriver appiumDriver) {
            int startX = center.getX();
            int startY = center.getY();
            int endX = startX;
            int endY = startY;

            int deltaX = width / 2;
            int deltaY = height / 2;
            if (deltaX < 50) deltaX = 100;
            if (deltaY < 50) deltaY = 100;

            switch (direction) {
                case UP -> endY = startY - deltaY;
                case DOWN -> endY = startY + deltaY;
                case LEFT -> endX = startX - deltaX;
                case RIGHT -> endX = startX + deltaX;
            }

            PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
            Sequence swipeSequence = new Sequence(finger, 1);
            swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), startX, startY));
            swipeSequence.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
            swipeSequence.addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), endX, endY));
            swipeSequence.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
            appiumDriver.perform(List.of(swipeSequence));
        }
    }

    private void performMobileTap(int tapX, int tapY) {
        if (driverFacade != null && driverFacade.getInnerDriver() instanceof AppiumDriver appiumDriver) {
            PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
            Sequence clickPosition = new Sequence(touch, 1);
            clickPosition
                    .addAction(touch.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), tapX, tapY))
                    .addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
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

