package com.znsio.teswiz.runner;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;

import com.google.common.collect.ImmutableMap;
import com.znsio.teswiz.exceptions.FileNotUploadedException;
import com.znsio.teswiz.exceptions.InvalidTestDataException;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.HasNotifications;
import io.appium.java_client.android.StartsActivity;
import io.appium.java_client.ios.IOSDriver;
import org.apache.commons.lang3.NotImplementedException;

import java.io.File;
import java.io.IOException;

/**
 * Appium-only touch-gesture and device-control mechanics extracted from {@link Driver}.
 *
 * <p>Owns the W3C {@link PointerInput}/{@link Sequence} choreography (scroll, swipe, tap, flick,
 * double-tap, pinch/zoom, multi-touch) plus the platform-switch device controls (background,
 * foreground, deep-link, relaunch, clipboard, notifications). Holds the {@link AppiumDriver} and
 * casts to {@link AndroidDriver}/{@link IOSDriver} where a platform-specific call is required, so
 * those casts and {@code switch (Runner.getPlatform())} ladders live in one place.
 *
 * <p>Appium-only by nature - parity with web is intentionally not provided here. {@link Driver}
 * keeps its public gesture methods delegating to this class, so callers are unaffected. Gestures
 * that also need web element resolution ({@code dragAndDrop}, {@code selectNotification...},
 * {@code horizontalSwipeWithGesture}, {@code longPress(By)}) stay on {@link Driver}, which resolves
 * the element and may call the mechanics here.
 */
class MobileGestures {

    private static final Logger LOGGER = LogManager.getLogger(MobileGestures.class.getName());
    private static final String DIMENSION = "dimension: ";
    private static final String TO = "' to '";

    private final AppiumDriver appiumDriver;

    MobileGestures(AppiumDriver appiumDriver) {
        this.appiumDriver = appiumDriver;
    }

    void scroll(Point fromPoint, Point toPoint) {
        PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
        Sequence scroller = new Sequence(touch, 1);
        scroller.addAction(touch.createPointerMove(Duration.ofSeconds(0), PointerInput.Origin.viewport(),
                fromPoint.getX(), fromPoint.getY()));
        scroller.addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        scroller.addAction(touch.createPointerMove(Duration.ofSeconds(1), PointerInput.Origin.viewport(),
                toPoint.getX(), toPoint.getY()));
        scroller.addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        LOGGER.info("fromPoint width: %d, fromPoint height: %d".formatted(fromPoint.getX(), fromPoint.getY()));
        LOGGER.info("toPoint width: %d, toPoint height: %d".formatted(toPoint.getX(), toPoint.getY()));
        appiumDriver.perform(singletonList(scroller));
    }

    void scrollDownByScreenSize() {
        Dimension windowSize = appiumDriver.manage().window().getSize();
        LOGGER.info(DIMENSION + windowSize.toString());
        int width = windowSize.width / 2;
        int fromHeight = (int) (windowSize.height * 0.8);
        int toHeight = (int) (windowSize.height * 0.2);
        LOGGER.info("width: %d, from height: %d, to height: %d".formatted(width, fromHeight, toHeight));
        scroll(new Point(width, fromHeight), new Point(width, toHeight));
    }

    void scrollVertically(int fromPercentScreenHeight, int toPercentScreenHeight, int percentScreenWidth) {
        Dimension windowSize = appiumDriver.manage().window().getSize();
        LOGGER.info(DIMENSION + windowSize.toString());
        int width = (windowSize.width * percentScreenWidth) / 100;
        int fromHeight = windowSize.height * fromPercentScreenHeight / 100;
        int toHeight = windowSize.height * toPercentScreenHeight / 100;
        LOGGER.info("width: %d, from height: %d, to height: %d".formatted(width, fromHeight, toHeight));
        scroll(new Point(width, fromHeight), new Point(width, toHeight));
    }

    void tapOnMiddleOfScreenOnDevice() {
        Dimension screenSize = appiumDriver.manage().window().getSize();
        int midHeight = screenSize.height / 2;
        int midWidth = screenSize.width / 2;
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("tapOnMiddleOfScreen: Screen dimensions: '%s'. Tapping on coordinates: %d:%d%n",
                    screenSize, midWidth, midHeight));
        }
        PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
        Sequence clickPosition = new Sequence(touch, 1);
        clickPosition
                .addAction(touch.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), midWidth,
                        midHeight))
                .addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        appiumDriver.perform(Arrays.asList(clickPosition));
    }

    private int getWindowHeight() {
        Dimension windowSize = appiumDriver.manage().window().getSize();
        LOGGER.info(DIMENSION + windowSize.toString());
        return windowSize.height;
    }

    private int getWindowWidth() {
        return appiumDriver.manage().window().getSize().width;
    }

    private void checkPercentagesAreValid(int... percentages) {
        boolean arePercentagesValid = Arrays.stream(percentages)
                .allMatch(percentage -> percentage >= 0 && percentage <= 100);
        if (!arePercentagesValid) {
            throw new RuntimeException(
                    String.format("Invalid percentage value - percentage value should be between 0 - 100. but are %s",
                            Arrays.toString(percentages)));
        }
    }

    void swipeRight() {
        int height = getWindowHeight() / 2;
        int fromWidth = (int) (getWindowWidth() * 0.2);
        int toWidth = (int) (getWindowWidth() * 0.7);
        LOGGER.info("height: {}, from width: {}, to width: {}", height, fromWidth, toWidth);
        swipe(height, fromWidth, toWidth);
    }

    void swipeLeft() {
        int height = getWindowHeight() / 2;
        int fromWidth = (int) (getWindowWidth() * 0.8);
        int toWidth = (int) (getWindowWidth() * 0.3);
        LOGGER.info("height: {}, from width: {}, to width: {}", height, fromWidth, toWidth);
        swipe(height, fromWidth, toWidth);
    }

    void swipeByPassingPercentageAttributes(int percentScreenHeight, int fromPercentScreenWidth,
            int toPercentScreenWidth) {
        LOGGER.info(
                "percent attributes passed to method are: percentScreenHeight: {}, fromPercentScreenWidth: {}, toPercentScreenWidth: {}",
                percentScreenHeight, fromPercentScreenWidth, toPercentScreenWidth);
        checkPercentagesAreValid(percentScreenHeight, fromPercentScreenWidth, toPercentScreenWidth);
        int height = getWindowHeight() * percentScreenHeight / 100;
        int fromWidth = getWindowWidth() * fromPercentScreenWidth / 100;
        int toWidth = getWindowWidth() * toPercentScreenWidth / 100;
        LOGGER.info("swipe gesture at height: {}, from width: {}, to width: {}", height, fromWidth, toWidth);
        swipe(height, fromWidth, toWidth);
    }

    private void swipe(int height, int fromWidth, int toWidth) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence sequence = new Sequence(finger, 1);
        sequence.addAction(finger.createPointerMove(ofMillis(0), PointerInput.Origin.viewport(), fromWidth, height));
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.MIDDLE.asArg()));
        sequence.addAction(new Pause(finger, ofSeconds(1)));
        sequence.addAction(finger.createPointerMove(ofSeconds(1), PointerInput.Origin.viewport(), toWidth, height));
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.MIDDLE.asArg()));
        appiumDriver.perform(singletonList(sequence));
    }

    void openNotifications() {
        ((HasNotifications) appiumDriver).openNotifications();
    }

    void doubleTap(WebElement element) {
        int x = element.getLocation().getX();
        int y = element.getLocation().getY();
        PointerInput touch = new PointerInput(PointerInput.Kind.MOUSE, "touch");
        Sequence clickPosition = new Sequence(touch, 1);
        clickPosition.addAction(touch.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), x, y))
                .addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(touch, ofMillis(10)))
                .addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        appiumDriver.perform(Arrays.asList(clickPosition));
    }

    void flick() {
        Dimension screenSize = appiumDriver.manage().window().getSize();
        LOGGER.info("Implementing flick action on the basis of screen size and co-ordinates");
        int startX = screenSize.width - 100;
        int startY = screenSize.height / 2;
        int endX = screenSize.width / 2;
        int endY = screenSize.height / 2;
        LOGGER.info("Start co-ordinates- X axis: " + startX + " & Y axis: " + startY + ", End co-ordinates- X axis: "
                + endX + " & Y axis: " + endY);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence flick = new Sequence(finger, 0);
        flick.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), startX, startY));
        flick.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        flick.addAction(finger.createPointerMove(Duration.ofMillis(100), PointerInput.Origin.viewport(), endX, endY));
        flick.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        appiumDriver.perform(Arrays.asList(flick));
    }

    private Sequence fingerAction(String fingerName, Point locus, int startRadius, int endRadius, double angle,
            Duration duration) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, fingerName);
        Sequence fingerPath = new Sequence(finger, 0);
        int fingerStartXPoint = (int) Math.floor(locus.x + startRadius * Math.cos(angle));
        int fingerStartYPoint = (int) Math.floor(locus.y - startRadius * Math.sin(angle));
        int fingerEndXPoint = (int) Math.floor(locus.x + endRadius * Math.cos(angle));
        int fingerEndYPoint = (int) Math.floor(locus.y - endRadius * Math.sin(angle));
        LOGGER.debug("fingerStartXPoint: %d, fingerStartYPoint: %d \nfingerEndXPoint: %d, fingerEndYPoint: %d"
                .formatted(fingerStartXPoint, fingerStartYPoint, fingerEndXPoint, fingerEndYPoint));
        fingerPath
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), fingerStartXPoint,
                        fingerStartYPoint))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofMillis(10))).addAction(finger.createPointerMove(duration,
                        PointerInput.Origin.viewport(), fingerEndXPoint, fingerEndYPoint))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        return fingerPath;
    }

    private Collection<Sequence> pinchAndZoom(Point locus, int startRadius, int endRadius, int pinchAngle,
            Duration duration) {
        double angle = Math.PI / 2 - (2 * Math.PI / 360 * pinchAngle);
        LOGGER.debug("Locus: %s, startRadius: %d, endRadius: %d, pinchAngle: %d, duration: %s".formatted(locus,
                startRadius, endRadius, pinchAngle, duration));
        Sequence finger1Path = fingerAction("finger1", locus, startRadius, endRadius, angle, duration);
        angle = angle + Math.PI;
        Sequence finger2Path = fingerAction("finger2", locus, startRadius, endRadius, angle, duration);
        return Arrays.asList(finger1Path, finger2Path);
    }

    private Collection<Sequence> pinchAndZoomIn(Point locus, int distance) {
        int startRadius = 200, endRadius = 200 + distance, pinchAngle = 45, duration = 100;
        return pinchAndZoom(locus, startRadius, endRadius, pinchAngle, Duration.ofMillis(duration));
    }

    private Collection<Sequence> pinchAndZoomOut(Point locus, int distance) {
        int endRadius = 200, startRadius = 200 + distance, pinchAngle = 45, duration = 100;
        return pinchAndZoom(locus, startRadius, endRadius, pinchAngle, Duration.ofMillis(duration));
    }

    void pinchAndZoomIn(WebElement element) {
        Dimension size = element.getSize();
        int centerX = size.getWidth() / 2;
        int centerY = size.getHeight() / 2;
        LOGGER.debug("Web element dimensions are centerX: %d, centerY: %d".formatted(centerX, centerY));
        appiumDriver.perform(pinchAndZoomIn(new Point(centerX, centerY), 5));
    }

    void pinchAndZoomOut(WebElement element) {
        Dimension size = element.getSize();
        int centerX = size.getWidth() / 2;
        int centerY = size.getHeight() / 2;
        LOGGER.debug("Web element dimensions are centerX: %d, centerY: %d".formatted(centerX, centerY));
        appiumDriver.perform(pinchAndZoomOut(new Point(centerX, centerY), 5));
    }

    void multiTouchOnElements(WebElement firstElement, WebElement secondElement) {
        LOGGER.info("Determining x and y co-ordinates of WebElements to perform multi touch action");
        Dimension screenSize = appiumDriver.manage().window().getSize();
        int xCoordinateFirstElement = (screenSize.width - 40) / 2;
        int yCoordinateFirstElement = firstElement.getLocation().y;
        int xCoordinateSecondElement = (screenSize.width - 40) / 2;
        int yCoordinateSecondElement = secondElement.getLocation().y;
        multiTouch(xCoordinateFirstElement, yCoordinateFirstElement, xCoordinateSecondElement,
                yCoordinateSecondElement);
    }

    private void multiTouch(int xElement1, int yElement1, int xElement2, int yElement2) {
        PointerInput finger1 = new PointerInput(PointerInput.Kind.MOUSE, "finger1");
        PointerInput finger2 = new PointerInput(PointerInput.Kind.MOUSE, "finger2");
        LOGGER.info("Creating two action sequences to perform multi touch action with two fingers");
        Sequence multiTouchAction = new Sequence(finger1, 1);
        Sequence multiTouchAction2 = new Sequence(finger2, 1);
        LOGGER.info(
                "Performing tap action simultaneously on elements present at co-ordinates X1: %d Y1: %d and X2: %d Y2: %d"
                        .formatted(xElement1, yElement1, xElement2, yElement2));
        multiTouchAction
                .addAction(finger1.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), xElement1,
                        yElement1))
                .addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        multiTouchAction2
                .addAction(finger2.createPointerMove(Duration.ofMillis(1), PointerInput.Origin.viewport(), xElement2,
                        yElement2))
                .addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        appiumDriver.perform(asList(multiTouchAction, multiTouchAction2));
    }

    void putAppInBackgroundFor(int numberOfSeconds) {
        switch (Runner.getPlatform()) {
            case android:
                ((AndroidDriver) appiumDriver).runAppInBackground(Duration.ofSeconds(numberOfSeconds));
                break;
            case iOS:
                ((IOSDriver) appiumDriver).runAppInBackground(Duration.ofSeconds(numberOfSeconds));
                break;
            default:
                throw new NotImplementedException(
                        "putAppInBackgroundFor method is not implemented for " + Runner.getPlatform());
        }
    }

    void bringAppInForeground() {
        ((StartsActivity) appiumDriver).currentActivity();
    }

    void goToDeepLinkUrl(String url, String packageName) {
        LOGGER.info("Hitting a Deep Link URL: " + url);
        appiumDriver.executeScript("mobile:deepLink", ImmutableMap.of("url", url, "package", packageName));
    }

    void pushFileToDevice(String filePathToPush, String devicePath) {
        LOGGER.info("Pushing the file: '" + filePathToPush + TO + Runner.getPlatform().name() + "' " + "device "
                + "on path: '" + devicePath + "'");
        try {
            switch (Runner.getPlatform()) {
                case android:
                    ((AndroidDriver) appiumDriver).pushFile(devicePath, new File(filePathToPush));
                    break;
                case iOS:
                    ((IOSDriver) appiumDriver).pushFile(devicePath, new File(filePathToPush));
                    break;
                default:
                    throw new InvalidTestDataException("pushFile is supported only on Android/iOS platform");
            }
        } catch (IOException e) {
            throw new FileNotUploadedException(String.format("Error in pushing the file: '%s%s%s' device on path: '%s'",
                    filePathToPush, TO, Runner.getPlatform().name(), devicePath), e);
        }
    }

    void relaunchApp() {
        String appPackageName = Runner.getAppPackageName();
        switch (Runner.getPlatform()) {
            case android:
                ((AndroidDriver) appiumDriver).terminateApp(appPackageName);
                ((AndroidDriver) appiumDriver).activateApp(appPackageName);
                break;
            case iOS:
                ((IOSDriver) appiumDriver).terminateApp(appPackageName);
                ((IOSDriver) appiumDriver).activateApp(appPackageName);
                break;
            default:
                throw new NotImplementedException("relaunchApp method is not implemented for " + Runner.getPlatform());
        }
    }

    String getClipboardText() {
        return switch (Runner.getPlatform()) {
            case android -> ((AndroidDriver) appiumDriver).getClipboardText();
            case iOS -> ((IOSDriver) appiumDriver).getClipboardText();
            default -> throw new NotImplementedException(
                    "getClipboardText method is not implemented for " + Runner.getPlatform());
        };
    }

    void setClipboardText(String text) {
        switch (Runner.getPlatform()) {
            case android -> ((AndroidDriver) appiumDriver).setClipboardText(text);
            case iOS -> ((IOSDriver) appiumDriver).setClipboardText(text);
            default -> throw new NotImplementedException(
                    "setClipboardText method is not implemented for " + Runner.getPlatform());
        }
    }

    List<Sequence> dragSequence(Point from, Point to) {
        PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
        Sequence sequence = new Sequence(touch, 1);
        sequence.addAction(touch.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(),
                from.getX(), from.getY()))
                .addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(touch.createPointerMove(Duration.ofSeconds(1), PointerInput.Origin.viewport(),
                        to.getX(), to.getY()))
                .addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        return List.of(sequence);
    }

    void perform(List<Sequence> sequences) {
        appiumDriver.perform(sequences);
    }

    Dimension windowSize() {
        return appiumDriver.manage().window().getSize();
    }
}
