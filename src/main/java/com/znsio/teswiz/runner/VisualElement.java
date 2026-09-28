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

    public void click() {
        Point center = getCenter();
        LOGGER.info(String.format("Clicking visual element '%s' at center coordinates (%d, %d)", label, center.getX(), center.getY()));
        if (driverFacade.getInnerDriver() != null) {
            if (Driver.APPIUM_DRIVER.equals(driverFacade.getType())) {
                performMobileTap(center.getX(), center.getY());
            } else {
                Actions actions = new Actions(driverFacade.getInnerDriver());
                actions.moveToLocation(center.getX(), center.getY()).click().perform();
            }
        } else {
            LOGGER.warn(String.format("Unable to click visual element '%s': inner driver is null", label));
        }
    }

    public void doubleClick() {
        Point center = getCenter();
        LOGGER.info(String.format("Double-clicking visual element '%s' at (%d, %d)", label, center.getX(), center.getY()));
        if (driverFacade.getInnerDriver() != null) {
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
        Point center = getCenter();
        LOGGER.info(String.format("Hovering over visual element '%s' at (%d, %d)", label, center.getX(), center.getY()));
        if (driverFacade.getInnerDriver() != null) {
            Actions actions = new Actions(driverFacade.getInnerDriver());
            actions.moveToLocation(center.getX(), center.getY()).perform();
        }
    }

    public void sendKeys(CharSequence... keysToSend) {
        click();
        LOGGER.info(String.format("Sending keys '%s' to visual element '%s'", String.join("", keysToSend), label));
        if (driverFacade.getInnerDriver() != null) {
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
        if (driverFacade.getInnerDriver() != null) {
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
        if (driverFacade.getInnerDriver() instanceof AppiumDriver appiumDriver) {
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
        if (driverFacade.getInnerDriver() instanceof AppiumDriver appiumDriver) {
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
}
