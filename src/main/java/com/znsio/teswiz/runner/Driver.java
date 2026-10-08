package com.znsio.teswiz.runner;

import static com.znsio.teswiz.tools.Wait.waitFor;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Collectors;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.google.common.collect.ImmutableMap;
import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.exceptions.FileNotUploadedException;
import com.znsio.teswiz.exceptions.InvalidTestDataException;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.HidesKeyboard;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.HasNotifications;
import io.appium.java_client.android.StartsActivity;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.SupportsContextSwitching;

public class Driver {
    public static final String WEB_DRIVER = "WebDriver";
    public static final String APPIUM_DRIVER = "AppiumDriver";
    public static final String PDF_DRIVER = "PDFDriver";
    private static final Logger LOGGER = LogManager.getLogger(Driver.class.getName());
    private final String type;
    private final WebDriver driver;
    private final String userPersona;
    private final String appName;
    private final Platform driverForPlatform;
    private static final String DIMENSION = "dimension: ";
    private final boolean isRunningInHeadlessMode;
    private static final String TO = "' to '";
    private Visual visually;
    public Driver(String testName, Platform forPlatform, String userPersona, String appName, AppiumDriver appiumDriver) {
        this.driver = appiumDriver;
        this.type = APPIUM_DRIVER;
        this.userPersona = userPersona;
        this.appName = appName;
        this.driverForPlatform = forPlatform;
        this.isRunningInHeadlessMode = false;
        instantiateEyes(testName, appiumDriver);
    }

    public Driver(String testName, Platform forPlatform, String userPersona, String appName, WebDriver webDriver,
            boolean isRunInHeadlessMode) {
        this.driver = webDriver;
        this.type = WEB_DRIVER;
        this.userPersona = userPersona;
        this.appName = appName;
        this.driverForPlatform = forPlatform;
        this.isRunningInHeadlessMode = isRunInHeadlessMode;
        instantiateEyes(testName, webDriver);
    }

    public Driver(String userPersona, Platform platform, TestExecutionContext context, String pdfFileName) {
        type = PDF_DRIVER;
        driver = null;
        this.userPersona = userPersona;
        this.appName = pdfFileName;
        this.driverForPlatform = platform;
        this.isRunningInHeadlessMode = false;
        instantiateEyes(context.getTestName(), pdfFileName);
    }

    private void instantiateEyes(String testName, String pdfFileName) {
        this.visually = new Visual(this.type, this.driverForPlatform, testName, userPersona, pdfFileName);
        this.visually.setDriverFacade(this);
    }

    private void instantiateEyes(String testName, AppiumDriver innerDriver) {
        this.visually = new Visual(this.type, this.driverForPlatform, innerDriver, testName, userPersona, appName);
        this.visually.setDriverFacade(this);
    }

    private void instantiateEyes(String testName, WebDriver innerDriver) {
        this.visually = new Visual(this.type, this.driverForPlatform, innerDriver, testName, userPersona, appName);
        this.visually.setDriverFacade(this);
    }

    public WebElement waitForClickabilityOf(String elementId) {
        return waitForClickabilityOf(elementId, DriverDefaults.waitTimeoutSeconds());
    }

    public WebElement waitForClickabilityOf(String elementId, int numberOfSecondsToWait) {
        return decorateElement((new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait)))
                .until(ExpectedConditions.elementToBeClickable(findElementByAccessibilityId(elementId))));
    }

    public WebElement findElementByAccessibilityId(String locator) {
        return decorateElement(driver.findElement(AppiumBy.accessibilityId(locator)));
    }

    public void waitForAlert() {
        waitForAlert(DriverDefaults.waitTimeoutSeconds());
    }

    public void waitForAlert(int numberOfSecondsToWait) {
        new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait)).until(ExpectedConditions.alertIsPresent());
        driver.switchTo().alert();
    }

    public WebElement findElement(By elementId) {
        return decorateElement(driver.findElement(elementId));
    }

    public void hideKeyboard() {
        try {
            if (driver instanceof io.appium.java_client.HidesKeyboard hidesKeyboard) {
                hidesKeyboard.hideKeyboard();
            }
        } catch (Exception e) {
            LOGGER.debug("Soft keyboard already hidden or unable to hide keyboard: " + e.getMessage());
        }
    }

    public List<WebElement> findElements(By element) {
        return decorateElements(this.driver.findElements(element));
    }

    public WebElement findElementById(String locator) {
        return decorateElement(driver.findElement(By.id(locator)));
    }

    public WebElement findElementByXpath(String locator) {
        return decorateElement(driver.findElement(By.xpath(locator)));
    }

    public void scroll(Point fromPoint, Point toPoint) {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
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

    public WebElement scrollToAnElementByText(String text) {
        return driver.findElement(AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector())" + ".scrollIntoView(new UiSelector().text(\"" + text + "\"));"));
    }

    public WebElement scrollToAnElementByText(String text, int maxSwipes) {
        return driver.findElement(
                AppiumBy.androidUIAutomator("new UiScrollable(new UiSelector().scrollable(true)).setMaxSearchSwipes("
                        + maxSwipes + ").scrollIntoView(new UiSelector().text(\"" + text + "\"));"));
    }

    public boolean isElementPresent(By locator) {
        return !driver.findElements(locator).isEmpty();
    }

    public boolean isElementPresentByAccessibilityId(String locator) {
        return !driver.findElements(AppiumBy.accessibilityId(locator)).isEmpty();
    }

    public boolean isElementPresentWithin(WebElement parentElement, By locator) {
        return !parentElement.findElements(locator).isEmpty();
    }

    public void scrollDownByScreenSize() {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        Dimension windowSize = appiumDriver.manage().window().getSize();
        LOGGER.info(DIMENSION + windowSize.toString());
        int width = windowSize.width / 2;
        int fromHeight = (int) (windowSize.height * 0.8);
        int toHeight = (int) (windowSize.height * 0.2);
        LOGGER.info("width: %d, from height: %d, to height: %d".formatted(width, fromHeight, toHeight));
        Point from = new Point(width, fromHeight);
        Point to = new Point(width, toHeight);
        scroll(from, to);
    }

    public void scrollVertically(int fromPercentScreenHeight, int toPercentScreenHeight, int percentScreenWidth) {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        Dimension windowSize = appiumDriver.manage().window().getSize();
        LOGGER.info(DIMENSION + windowSize.toString());
        int width = (windowSize.width * percentScreenWidth) / 100;
        int fromHeight = windowSize.height * fromPercentScreenHeight / 100;
        int toHeight = windowSize.height * toPercentScreenHeight / 100;
        LOGGER.info("width: %d, from height: %d, to height: %d".formatted(width, fromHeight, toHeight));
        Point from = new Point(width, fromHeight);
        Point to = new Point(width, toHeight);
        scroll(from, to);
    }

    public void tapOnMiddleOfScreen() {
        if (this.type.equals(Driver.APPIUM_DRIVER)) {
            tapOnMiddleOfScreenOnDevice();
        } else {
            simulateMouseMovementOnBrowser();
        }
    }

    private void tapOnMiddleOfScreenOnDevice() {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
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
        waitFor(1);
    }

    private void simulateMouseMovementOnBrowser() {
        Actions actions = new Actions(this.driver);
        Dimension screenSize = driver.manage().window().getSize();
        Point currentPosition = driver.manage().window().getPosition();

        int midHeight = screenSize.height / 2;
        int midWidth = screenSize.width / 2;
        int currentPositionX = currentPosition.getX();
        int currentPositionY = currentPosition.getY();
        LOGGER.info("Current position: '{}':'{}'", currentPositionX, currentPositionY);

        int offsetX = currentPositionX < midWidth ? 50 : -50;
        int offsetY = currentPositionY < midHeight ? 50 : -50;

        LOGGER.info("Using offset: '{}':'{}'", offsetX, offsetY);

        actions.moveByOffset(offsetX, offsetY).perform();
        waitFor(1);
    }

    private int getWindowHeight() {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        Dimension windowSize = appiumDriver.manage().window().getSize();
        LOGGER.info(DIMENSION + windowSize.toString());
        return windowSize.height;
    }

    private int getWindowWidth() {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
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

    public void swipeRight() {
        int height = getWindowHeight() / 2;
        int fromWidth = (int) (getWindowWidth() * 0.2);
        int toWidth = (int) (getWindowWidth() * 0.7);
        LOGGER.info("height: {}, from width: {}, to width: {}", height, fromWidth, toWidth);
        swipe(height, fromWidth, toWidth);
    }

    public void swipeLeft() {
        int height = getWindowHeight() / 2;
        int fromWidth = (int) (getWindowWidth() * 0.8);
        int toWidth = (int) (getWindowWidth() * 0.3);
        LOGGER.info("height: {}, from width: {}, to width: {}", height, fromWidth, toWidth);
        swipe(height, fromWidth, toWidth);
    }

    public void swipeByPassingPercentageAttributes(int percentScreenHeight, int fromPercentScreenWidth,
            int toPercentScreenWidth) {
        LOGGER.info("percent attributes passed to method are: percentScreenHeight: {}, fromPercentScreenWidth: {}, toPercentScreenWidth: {}", percentScreenHeight, fromPercentScreenWidth, toPercentScreenWidth);
        checkPercentagesAreValid(percentScreenHeight, fromPercentScreenWidth, toPercentScreenWidth);
        int height = getWindowHeight() * percentScreenHeight / 100;
        int fromWidth = getWindowWidth() * fromPercentScreenWidth / 100;
        int toWidth = getWindowWidth() * toPercentScreenWidth / 100;
        LOGGER.info("swipe gesture at height: {}, from width: {}, to width: {}", height, fromWidth, toWidth);
        swipe(height, fromWidth, toWidth);
    }

    private void swipe(int height, int fromWidth, int toWidth) {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence sequence = new Sequence(finger, 1);
        sequence.addAction(finger.createPointerMove(ofMillis(0), PointerInput.Origin.viewport(), fromWidth, height));
        sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.MIDDLE.asArg()));
        sequence.addAction(new Pause(finger, ofSeconds(1)));
        sequence.addAction(finger.createPointerMove(ofSeconds(1), PointerInput.Origin.viewport(), toWidth, height));
        sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.MIDDLE.asArg()));
        appiumDriver.perform(singletonList(sequence));
    }

    public void openNotifications() {
        LOGGER.info("Fetching the NOTIFICATIONS on the device: ");
        waitFor(3);
        ((HasNotifications) driver).openNotifications();
        waitFor(2);
    }

    public void selectNotificationFromNotificationDrawer(By selectNotificationLocator) {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        Dimension screenSize = appiumDriver.manage().window().getSize();
        PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
        Sequence dragNotificationBar = new Sequence(touch, 1);
        dragNotificationBar.addAction(touch.createPointerMove(Duration.ofSeconds(0), PointerInput.Origin.viewport(),
                screenSize.width / 2, 0));
        dragNotificationBar.addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        dragNotificationBar.addAction(touch.createPointerMove(Duration.ofSeconds(1), PointerInput.Origin.viewport(),
                screenSize.width / 2, screenSize.height));
        dragNotificationBar.addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        appiumDriver.perform(singletonList(dragNotificationBar));
        appiumDriver.perform(singletonList(dragNotificationBar));
        waitFor(1);

        WebElement selectNotificationElement = driver.findElement(selectNotificationLocator);
        LOGGER.info("Notification found: " + selectNotificationElement.isDisplayed());
        selectNotificationElement.click();
    }

    public void putAppInBackgroundFor(int numberOfSeconds) {
        switch (Runner.getPlatform()) {
            case android:
                ((AndroidDriver) driver).runAppInBackground(Duration.ofSeconds(numberOfSeconds));
                break;
            case iOS:
                ((IOSDriver) driver).runAppInBackground(Duration.ofSeconds(numberOfSeconds));
                break;
            default:
                throw new NotImplementedException(
                        "putAppInBackgroundFor method is not implemented for " + Runner.getPlatform());
        }
    }

    public void bringAppInForeground() {
        ((StartsActivity) driver).currentActivity();
    }

    public void goToDeepLinkUrl(String url, String packageName) {
        LOGGER.info("Hitting a Deep Link URL: " + url);
        ((AppiumDriver) driver).executeScript("mobile:deepLink", ImmutableMap.of("url", url, "package", packageName));
    }

    public WebDriver getInnerDriver() {
        return driver;
    }

    public Platform getPlatformName() {
        return driverForPlatform;
    }

    public Path printAndSavePageSourceDump() {
        String pageSource = capturePageSource();
        int dumpIndex = getNextPageSourceDumpIndex();
        Path outputFile = resolvePageSourceDumpPath(dumpIndex);

        try {
            Files.createDirectories(outputFile.getParent());
            Files.writeString(outputFile, pageSource, StandardCharsets.UTF_8);
            LOGGER.info("Saved page source dump to: {}", outputFile.toAbsolutePath());
            return outputFile;
        } catch (IOException e) {
            throw new RuntimeException("Could not save page source dump to: " + outputFile.toAbsolutePath(), e);
        }
    }

    private String capturePageSource() {
        return driver.getPageSource();
    }

    private int getNextPageSourceDumpIndex() {
        TestExecutionContext context = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
        Integer currentIndex = (Integer) context.getTestState(TEST_CONTEXT.PAGE_SOURCE_DUMP_INDEX);
        int nextIndex = currentIndex == null ? 1 : currentIndex + 1;
        context.addTestState(TEST_CONTEXT.PAGE_SOURCE_DUMP_INDEX, nextIndex);
        return nextIndex;
    }

    private Path resolvePageSourceDumpPath(int dumpIndex) {
        TestExecutionContext context = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
        String scenarioLogDirectory = context.getTestStateAsString(TEST_CONTEXT.SCENARIO_LOG_DIRECTORY);
        if (scenarioLogDirectory == null || scenarioLogDirectory.isBlank()) {
            throw new IllegalStateException("Scenario log directory is unavailable for page source dump");
        }
        String dumpFileName = (driverForPlatform == Platform.android || driverForPlatform == Platform.iOS)
                ? "hierarchy-dump-" + dumpIndex + ".xml"
                : "dom-dump-" + dumpIndex + ".html";
        return Path.of(scenarioLogDirectory, dumpFileName);
    }

    public String getType() {
        return this.type;
    }

    public Visual getVisual() {
        return this.visually;
    }

    public void longPress(By elementId) {
        longPress(elementId, 1);
    }

    public void longPress(By elementId, long durationInSeconds) {
        WebElement elementToBeLongTapped = new WebDriverWait(driver,
                Duration.ofSeconds(DriverDefaults.waitTimeoutSeconds()))
                .until(ExpectedConditions.elementToBeClickable(elementId));
        final Point location = elementToBeLongTapped.getLocation();
        final PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        final Sequence sequence = new Sequence(finger, 1);
        sequence.addAction(
                finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), location.x, location.y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofSeconds(durationInSeconds)))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        ((AppiumDriver) driver).perform(Collections.singletonList(sequence));
    }

    public void pushFileToDevice(String filePathToPush, String devicePath) {
        LOGGER.info("Pushing the file: '" + filePathToPush + TO + Runner.getPlatform().name() + "' " + "device "
                + "on path: '" + devicePath + "'");
        try {
            switch (Runner.getPlatform()) {
                case android:
                    ((AndroidDriver) driver).pushFile(devicePath, new File(filePathToPush));
                    break;
                case iOS:
                    ((IOSDriver) driver).pushFile(devicePath, new File(filePathToPush));
                    break;
                default:
                    throw new InvalidTestDataException("pushFile is supported only on Android/iOS platform");
            }
        } catch (IOException e) {
            throw new FileNotUploadedException(String.format("Error in pushing the file: '%s%s%s' device on path: '%s'",
                    filePathToPush, TO, Runner.getPlatform().name(), devicePath), e);
        }
    }

    public void allowPermission(By element) {
        waitForClickabilityOf(element);
        if (Runner.getPlatform().equals(Platform.android)) {
            driver.findElement(element).click();
        }
    }

    public WebElement waitForClickabilityOf(By elementId) {
        return waitForClickabilityOf(elementId, DriverDefaults.waitTimeoutSeconds());
    }

    public WebElement waitForClickabilityOf(By elementId, int numberOfSecondsToWait) {
        return decorateElement((new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait))
                .until(ExpectedConditions.elementToBeClickable(elementId))));
    }

    public List<WebElement> findElementsByAccessibilityId(String elementId) {
        return decorateElements(((AppiumDriver) driver).findElements(AppiumBy.accessibilityId(elementId)));
    }

    public WebElement waitTillElementIsPresent(By elementId) {
        return waitTillElementIsPresent(elementId, DriverDefaults.waitTimeoutSeconds());
    }

    public WebElement waitTillElementIsVisible(By elementId) {
        return waitTillElementIsVisible(elementId, DriverDefaults.waitTimeoutSeconds());
    }

    public WebElement waitTillElementIsPresent(By elementId, int numberOfSecondsToWait) {
        return decorateElement((new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait))
                .until(ExpectedConditions.presenceOfElementLocated(elementId))));
    }

    public WebElement waitTillElementIsVisible(By elementId, int numberOfSecondsToWait) {
        return decorateElement((new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait))
                .until(ExpectedConditions.visibilityOfElementLocated(elementId))));
    }

    public List<WebElement> waitTillVisibilityOfAllElements(By elementId) {
        return waitTillVisibilityOfAllElements(elementId, DriverDefaults.waitTimeoutSeconds());
    }

    public List<WebElement> waitTillVisibilityOfAllElements(By elementId, int numberOfSecondsToWait) {
        return decorateElements((new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait))
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(elementId))));
    }

    public WebElement waitTillElementIsVisible(String elementId) {
        return waitTillElementIsVisible(elementId, DriverDefaults.waitTimeoutSeconds());
    }

    public WebElement waitTillElementIsVisible(String elementId, int numberOfSecondsToWait) {
        return decorateElement((new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait))
                .until(ExpectedConditions.visibilityOf(findElementByAccessibilityId(elementId)))));
    }

    public List<WebElement> waitTillPresenceOfAllElements(By elementId) {
        return waitTillPresenceOfAllElements(elementId, DriverDefaults.waitTimeoutSeconds());
    }

    public List<WebElement> waitTillPresenceOfAllElements(By elementId, int numberOfSecondsToWait) {
        return decorateElements((new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait))
                .until(ExpectedConditions.presenceOfAllElementsLocatedBy(elementId))));
    }

    public void setWindowSize(int width, int height) {
        if (this.type.equals(Driver.WEB_DRIVER)) {
            driver.manage().window().setSize(new Dimension(width, height));
        }
    }

    public void moveToElement(By moveToElementLocator) {
        Actions actions = new Actions(driver);
        actions.moveToElement(driver.findElement(moveToElementLocator)).build().perform();
        waitFor(1);
    }

    private org.openqa.selenium.Rectangle activeHighlightBounds;

    public org.openqa.selenium.Rectangle getActiveHighlightBounds() {
        return activeHighlightBounds;
    }

    public void clearHighlight() {
        this.activeHighlightBounds = null;
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return;
        }
        if (APPIUM_DRIVER.equals(type) || driver instanceof AppiumDriver) {
            return;
        }
        if (driver instanceof JavascriptExecutor js) {
            try {
                js.executeScript(
                    "let visualBox = document.getElementById('teswiz-visual-highlight');" +
                    "if (visualBox) { visualBox.remove(); }" +
                    "if (window.teswizLastHighlightedElement) {" +
                    "  try {" +
                    "    window.teswizLastHighlightedElement.style.outline = window.teswizLastOutline || '';" +
                    "    window.teswizLastHighlightedElement.style.outlineOffset = window.teswizLastOutlineOffset || '';" +
                    "    window.teswizLastHighlightedElement.style.boxShadow = window.teswizLastBoxShadow || '';" +
                    "  } catch(e) {}" +
                    "  delete window.teswizLastHighlightedElement;" +
                    "  delete window.teswizLastOutline;" +
                    "  delete window.teswizLastOutlineOffset;" +
                    "  delete window.teswizLastBoxShadow;" +
                    "}"
                );
            } catch (Exception ignored) {}
        }
    }

    public void highlightElement(WebElement element) {
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return;
        }
        clearHighlight();
        if (APPIUM_DRIVER.equals(type) || driver instanceof AppiumDriver) {
            LOGGER.debug("DOM-based element highlighting is not supported on native mobile app.");
            return;
        }
        String color = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_COLOR, "#FF4500");
        String borderWidth = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_BORDER_WIDTH, "3px");
        if (driver instanceof JavascriptExecutor js) {
            try {
                js.executeScript(
                    "window.teswizLastHighlightedElement = arguments[0];" +
                    "window.teswizLastOutline = arguments[0].style.outline;" +
                    "window.teswizLastOutlineOffset = arguments[0].style.outlineOffset;" +
                    "window.teswizLastBoxShadow = arguments[0].style.boxShadow;" +
                    "arguments[0].style.outline = '" + borderWidth + " solid " + color + "';" +
                    "arguments[0].style.outlineOffset = '-2px';" +
                    "arguments[0].style.boxShadow = '0 0 10px " + color + "';"
                , element);
                LOGGER.info("Highlighted WebElement visually with outline color: " + color);
            } catch (Exception e) {
                LOGGER.debug("Could not highlight web element: " + e.getMessage());
            }
        }
    }

    public void highlightVisualElement(int x, int y, int width, int height) {
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return;
        }
        clearHighlight();
        this.activeHighlightBounds = new org.openqa.selenium.Rectangle(x, y, height, width);
        if (APPIUM_DRIVER.equals(type) || driver instanceof AppiumDriver) {
            LOGGER.info("Visual element screenshot image canvas highlighting active on native mobile app at bounds [x={}, y={}, w={}, h={}]", x, y, width, height);
            return;
        }
        String color = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_COLOR, "#FF4500");
        String borderWidth = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_BORDER_WIDTH, "3px");
        if (driver instanceof JavascriptExecutor js) {
            try {
                js.executeScript(
                    "let id = 'teswiz-visual-highlight';" +
                    "let box = document.createElement('div');" +
                    "box.id = id;" +
                    "document.body.appendChild(box);" +
                    "box.style.position = 'fixed';" +
                    "box.style.left = '" + x + "px';" +
                    "box.style.top = '" + y + "px';" +
                    "box.style.width = '" + width + "px';" +
                    "box.style.height = '" + height + "px';" +
                    "box.style.border = '" + borderWidth + " solid " + color + "';" +
                    "box.style.backgroundColor = 'rgba(255, 69, 0, 0.25)';" +
                    "box.style.boxShadow = '0 0 10px " + color + "';" +
                    "box.style.zIndex = '2147483647';" +
                    "box.style.pointerEvents = 'none';" +
                    "box.style.boxSizing = 'border-box';" +
                    "box.style.transition = 'all 0.1s ease-in-out';"
                );
                LOGGER.info("Highlighted visual element at viewport bounds [x={}, y={}, w={}, h={}] with color {}", x, y, width, height, color);
            } catch (Exception e) {
                LOGGER.debug("Could not highlight visual element at (" + x + ", " + y + "): " + e.getMessage());
            }
        }
    }

    public void highlightVisualElement(VisualElement visualElement) {
        if (visualElement != null) {
            highlightVisualElement(visualElement.getX(), visualElement.getY(), visualElement.getWidth(), visualElement.getHeight());
        }
    }

    // ------------------------------------------------------------------------
    // Visual element gesture dispatch
    //
    // The engine-specific "how" (Appium touch / native coordinate input / Selenium Actions / synthesised DOM)
    // lives in VisualGestureDispatcher. VisualElement forwards here with its own geometry, so it stays a value
    // object and never inspects the inner driver type.
    // ------------------------------------------------------------------------

    private com.znsio.teswiz.visual.VisualGestureDispatcher visualGestureDispatcher() {
        return new com.znsio.teswiz.visual.VisualGestureDispatcher(driver, APPIUM_DRIVER.equals(type));
    }

    public void visualClickAt(org.openqa.selenium.Point point, String label) {
        visualGestureDispatcher().click(point, label);
    }

    public void visualDoubleClickAt(org.openqa.selenium.Point point, String label) {
        visualGestureDispatcher().doubleClick(point, label);
    }

    public void visualHoverAt(org.openqa.selenium.Point point, String label) {
        visualGestureDispatcher().hover(point, label);
    }

    public void visualEnterTextAt(org.openqa.selenium.Point point, String label, CharSequence... keysToSend) {
        visualGestureDispatcher().enterText(point, label, keysToSend);
    }

    public void visualLongPressAt(org.openqa.selenium.Point point, Duration duration, String label) {
        visualGestureDispatcher().longPress(point, duration, label);
    }

    public void visualSwipe(org.openqa.selenium.Point center, int width, int height,
            com.znsio.teswiz.entities.Direction direction, String label) {
        visualGestureDispatcher().swipe(center, width, height, direction, label);
    }

    public void visualDragAndDropTo(org.openqa.selenium.Point source, WebElement target, String label) {
        visualGestureDispatcher().dragAndDropTo(source, target, label);
    }

    public void visualZoom(double scaleFactor, String label) {
        visualGestureDispatcher().zoom(scaleFactor, label);
    }

    public void visualPinch(double scaleFactor, String label) {
        visualGestureDispatcher().pinch(scaleFactor, label);
    }

    private WebElement decorateElement(WebElement element) {
        if (element == null) {
            return null;
        }
        if (Proxy.isProxyClass(element.getClass()) && Proxy.getInvocationHandler(element) instanceof ElementInvocationHandler) {
            return element;
        }
        if (element.getClass().getName().contains("Playwright")) {
            return element;
        }
        return (WebElement) Proxy.newProxyInstance(
                Driver.class.getClassLoader(),
                new Class<?>[]{WebElement.class, WrapsElement.class},
                new ElementInvocationHandler(element)
        );
    }

    private List<WebElement> decorateElements(List<WebElement> elements) {
        if (elements == null) {
            return Collections.emptyList();
        }
        return elements.stream().map(this::decorateElement).collect(Collectors.toList());
    }

    private class ElementInvocationHandler implements InvocationHandler {
        private final WebElement target;

        ElementInvocationHandler(WebElement target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("getWrappedElement".equals(methodName) && (args == null || args.length == 0)) {
                return target;
            }
            if ("click".equals(methodName) || "sendKeys".equals(methodName) || "clear".equals(methodName) || "submit".equals(methodName)) {
                highlightElement(target);
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    public double getViewportScaleFactor(int screenshotImageWidth) {
        if (screenshotImageWidth <= 0) {
            return 1.0;
        }
        if (APPIUM_DRIVER.equals(type) || driver instanceof AppiumDriver) {
            try {
                int viewportWidth = driver.manage().window().getSize().getWidth();
                if (viewportWidth > 0) {
                    double scale = (double) screenshotImageWidth / viewportWidth;
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Calculated mobile viewport scale factor: %.2f (screenshot width: %d px, window width: %d px)",
                            scale, screenshotImageWidth, viewportWidth));
                    }
                    return scale;
                }
            } catch (Exception e) {
                LOGGER.debug("Could not get window size for Appium driver: " + e.getMessage());
            }
            return 1.0;
        }
        if (driver instanceof JavascriptExecutor js) {
            try {
                Object result = js.executeScript("return window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;");
                if (result instanceof Number num && num.doubleValue() > 0) {
                    double viewportWidth = num.doubleValue();
                    double scale = (double) screenshotImageWidth / viewportWidth;
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format("Calculated viewport scale factor: %.2f (screenshot width: %d px, viewport width: %.0f px)",
                            scale, screenshotImageWidth, viewportWidth));
                    }
                    return scale;
                }
            } catch (Exception e) {
                LOGGER.debug("Failed to retrieve viewport width via JavaScript: " + e.getMessage());
            }
        }
        return 1.0;
    }

    public boolean isDriverRunningInHeadlessMode() {
        return this.isRunningInHeadlessMode;
    }

    public WebDriver setWebViewContext() {
        LOGGER.info("Setting web view context");
        SupportsContextSwitching contextSwitchingDriver = (SupportsContextSwitching) driver;
        Set<String> contextHandles = contextSwitchingDriver.getContextHandles();
        LOGGER.info("List of context handles present");
        contextHandles.stream().forEach(LOGGER::info);
        return contextSwitchingDriver.context((String) contextHandles.toArray()[contextHandles.size() - 1]);
    }

    public WebDriver setNativeAppContext() {
        return setNativeAppContext("NATIVE_APP");
    }

    public WebDriver setNativeAppContext(String contextName) {
        LOGGER.info("Setting native app context");
        SupportsContextSwitching contextSwitchingDriver = (SupportsContextSwitching) driver;
        return contextSwitchingDriver.context(contextName);
    }

    public WebDriver switchFrameToDefault() {
        return driver.switchTo().defaultContent();
    }

    public WebDriver switchToFrame(String id) {
        return driver.switchTo().frame(id);
    }

    public void scrollToBottom() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
    }

    public void scrollTillElementIntoView(By elementId) {
        if (isNativeMobilePlatform()) {
            scrollNativeElementIntoView(elementId);
        } else {
            scrollTillElementIntoView(driver.findElement(elementId));
        }
    }

    private void scrollNativeElementIntoView(By elementId) {
        int maxScrollAttempts = DriverDefaults.scrollMaxAttempts();
        for (int attempt = 0; attempt < maxScrollAttempts; attempt++) {
            List<WebElement> matches = driver.findElements(elementId);
            if (!matches.isEmpty() && isElementDisplayed(matches.get(0))) {
                return;
            }
            scrollDownByScreenSize();
        }
        throw new NoSuchElementException("scrollTillElementIntoView: element '" + elementId
                + "' was not visible after " + maxScrollAttempts + " scroll attempts");
    }

    public void scrollTillElementIntoView(WebElement element) {
        if (isNativeMobilePlatform()) {
            // On native mobile, scrolling needs a re-findable locator. If the caller
            // already has a WebElement and it's on screen, nothing to do; otherwise
            // they should use the By-based overload (the only reliable native path).
            if (!isElementDisplayed(element)) {
                LOGGER.warn("scrollTillElementIntoView(WebElement) cannot scroll on native "
                        + "mobile platforms - the element is off-screen and not re-findable. "
                        + "Use scrollTillElementIntoView(By) instead.");
            }
        } else {
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        }
    }

    private boolean isNativeMobilePlatform() {
        return driverForPlatform == Platform.android || driverForPlatform == Platform.iOS;
    }

    public void switchToNextTab() {
        Iterator<String> iterator = driver.getWindowHandles().iterator();
        try {
            iterator.next();
            driver.switchTo().window(iterator.next());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("Unable to get next window handle.", e);
        }
    }

    public void switchToParentTab() {
        try {
            driver.switchTo().window(driver.getWindowHandles().iterator().next());
        } catch (NoSuchElementException e) {
            throw new NoSuchElementException("No previous tab found.", e);
        }
    }

    public void uploadFileInBrowser(String filePath, By locator) {
        try {
            LOGGER.info("Uploading file: " + filePath + " to the browser");
            driver.findElement(locator).sendKeys(filePath);
        } catch (Exception e) {
            throw new FileNotUploadedException(
                    String.format("Error in uploading the file: '%s%s%s", filePath, TO, Runner.getPlatform().name()),
                    e);
        }
    }

    /**
     * This method injects the media to browserstack to perform,
     * image scanning eg: QRcode,barcode etc
     * Throws NotImplementedException if platform is NOT android, and cloudName is
     * NOT browserstack
     *
     * @param uploadFileURL is an absolute path where a media file is located
     */
    public void injectMediaToBrowserstackDevice(String uploadFileURL) {
        String cloudName = Runner.getCloudName();
        if (isMobilePlatform() && cloudName.equalsIgnoreCase("browserstack")) {
            String cloudUser = Runner.getCloudUser();
            String cloudKey = Runner.getCloudKey();
            BrowserStackImageInjection.injectMediaToDriver(uploadFileURL, ((AppiumDriver) driver), cloudUser, cloudKey);
        } else {
            throw new NotImplementedException("injectMediaToBrowserstackDevice is not implemented for: " + cloudName);
        }
    }

    /**
     * This method injects the already uploaded media in browserstack(media url) to
     * browserstack real device,
     * image scanning eg: QRcode,barcode etc
     * Throws NotImplementedException if platform is NOT android, and cloudName is
     * NOT browserstack
     *
     * @param browserStackMediaUrl is a media url generated after uploading a file
     *                             to browserstack cloud using BS API
     */
    public void injectMediaUrlToBrowserstackDevice(String browserStackMediaUrl) {
        String cloudName = Runner.getCloudName();
        if (cloudName.equalsIgnoreCase("browserstack") && isMobilePlatform()) {
            BrowserStackImageInjection.injectMediaToDriver(browserStackMediaUrl, ((AppiumDriver) driver));
        } else {
            throw new NotImplementedException("injectMediaToBrowserstackDevice is not implemented for: " + cloudName);
        }
    }

    public boolean isMobilePlatform() {
        return Runner.getPlatform().equals(Platform.android) || Runner.getPlatform().equals(Platform.iOS);
    }

    public void scrollInDynamicLayer(Direction direction, WebElement dynamicLayerElement) {
        Dimension dimension = dynamicLayerElement.getSize();
        int width = (int) (dimension.width * 0.5);
        int fromHeight = (int) (dimension.height * 0.7);
        int toHeight = (int) (dimension.height * 0.6);
        int[] height = { fromHeight, toHeight };
        if (direction.equals(Direction.UP)) {
            Arrays.sort(height);
        }
        Point fromPoint = new Point(width, height[0]);
        Point toPoint = new Point(width, height[1]);
        scroll(fromPoint, toPoint);
    }

    public void setAttributeValue(WebElement element, String attribute, String value) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].setAttribute(arguments[1],arguments[2])", element,
                attribute, value);
    }

    public void dragAndDrop(By draggableLocator, By dropZoneLocator) {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        PointerInput touch = new PointerInput(PointerInput.Kind.TOUCH, "touch");
        Sequence sequence = new Sequence(touch, 1);

        WebElement dragElement = findElement(draggableLocator);
        WebElement dropZoneElement = findElement(dropZoneLocator);

        int middleXCoordinate_dragElement = dragElement.getLocation().x + dragElement.getSize().width / 2;
        int middleYCoordinate_dragElement = dragElement.getLocation().y + dragElement.getSize().height / 2;

        int middleXCoordinate_dropZone = dropZoneElement.getLocation().x + dropZoneElement.getSize().width / 2;
        int middleYCoordinate_dropZone = dropZoneElement.getLocation().y + dropZoneElement.getSize().height / 2;

        sequence.addAction(touch.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(),
                middleXCoordinate_dragElement, middleYCoordinate_dragElement))
                .addAction(touch.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(touch.createPointerMove(Duration.ofSeconds(1), PointerInput.Origin.viewport(),
                        middleXCoordinate_dropZone, middleYCoordinate_dropZone))
                .addAction(touch.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        appiumDriver.perform(List.of(sequence));
    }

    public void doubleTap(WebElement element) {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
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

    public void flick() {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        Dimension screenSize = driver.manage().window().getSize();

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

    public void horizontalSwipeWithGesture(WebElement element, Direction direction) {
        WebElement targetElement = element;
        if (element instanceof WrapsElement wrapsElement) {
            targetElement = wrapsElement.getWrappedElement();
        }
        RemoteWebElement remoteWebElement = (RemoteWebElement) targetElement;
        if ((direction.equals(Direction.LEFT)) || direction.equals(Direction.RIGHT)) {
            ((JavascriptExecutor) driver).executeScript("mobile: swipeGesture", Map.of("elementId",
                    remoteWebElement.getId(), "direction", direction.toString(), "percent", 1, "speed", 80));
        } else {
            throw new InvalidTestDataException("Invalid Direction");
        }
    }

    private Sequence fingerAction(String fingerName, Point locus, int startRadius, int endRadius, double angle,
            Duration duration) {

        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, fingerName);
        Sequence fingerPath = new Sequence(finger, 0);

        int fingerStartXPoint = (int) Math.floor(locus.x + startRadius * Math.cos(angle)); // converting from polar
                                                                                           // coordinates to cartesian
        int fingerStartYPoint = (int) Math.floor(locus.y - startRadius * Math.sin(angle));

        int fingerEndXPoint = (int) Math.floor(locus.x + endRadius * Math.cos(angle));
        int fingerEndYPoint = (int) Math.floor(locus.y - endRadius * Math.sin(angle));

        LOGGER.debug("fingerStartXPoint: %d, fingerStartYPoint: %d \nfingerEndXPoint: %d, fingerEndYPoint: %d"
                .formatted(fingerStartXPoint, fingerStartYPoint, fingerEndXPoint, fingerEndYPoint));
        fingerPath
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), fingerStartXPoint,
                        fingerStartYPoint))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new Pause(finger, Duration.ofMillis(10)))
                .addAction(finger.createPointerMove(duration, PointerInput.Origin.viewport(), fingerEndXPoint,
                        fingerEndYPoint))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        return fingerPath;
    }

    private Collection<Sequence> pinchAndZoom(Point locus, int startRadius, int endRadius, int pinchAngle,
            Duration duration) {

        double angle = Math.PI / 2 - (2 * Math.PI / 360 * pinchAngle); // convert degree angle into radians
        LOGGER.debug("Locus: %s, startRadius: %d, endRadius: %d, pinchAngle: %d, duration: %s"
                .formatted(locus, startRadius, endRadius, pinchAngle, duration));

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

    public void pinchAndZoomIn(WebElement element) {

        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        Dimension size = element.getSize();
        int centerX = size.getWidth() / 2;
        int centerY = size.getHeight() / 2;
        LOGGER.debug("Web element dimensions are centerX: %d, centerY: %d".formatted(centerX, centerY));

        Point locus = new Point(centerX, centerY);
        appiumDriver.perform(pinchAndZoomIn(locus, 5));
    }

    public void pinchAndZoomOut(WebElement element) {

        AppiumDriver appiumDriver = (AppiumDriver) this.driver;
        Dimension size = element.getSize();
        int centerX = size.getWidth() / 2;
        int centerY = size.getHeight() / 2;
        LOGGER.debug("Web element dimensions are centerX: %d, centerY: %d".formatted(centerX, centerY));

        Point locus = new Point(centerX, centerY);
        appiumDriver.perform(pinchAndZoomOut(locus, 5));
    }

    public void multiTouchOnElements(WebElement firstElement, WebElement SecondElement) {
        LOGGER.info("Determining x and y co-ordinates of WebElements to perform multi touch action");
        Dimension screenSize = driver.manage().window().getSize();
        int xCoordinate_firstElement = (screenSize.width - 40) / 2;
        int yCoordinate_firstElement = firstElement.getLocation().y;

        int xCoordinate_secondElement = (screenSize.width - 40) / 2;
        int yCoordinate_secondElement = SecondElement.getLocation().y;
        multiTouch(xCoordinate_firstElement, yCoordinate_firstElement, xCoordinate_secondElement,
                yCoordinate_secondElement);
    }

    private void multiTouch(int x_element1, int y_element1, int x_element2, int y_element2) {
        AppiumDriver appiumDriver = (AppiumDriver) this.driver;

        PointerInput finger1 = new PointerInput(PointerInput.Kind.MOUSE, "finger1");
        PointerInput finger2 = new PointerInput(PointerInput.Kind.MOUSE, "finger2");

        LOGGER.info("Creating two action sequences to perform multi touch action with two fingers");
        Sequence multiTouchAction = new Sequence(finger1, 1);
        Sequence multiTouchAction2 = new Sequence(finger2, 1);

        LOGGER.info(
                "Performing tap action simultaneously on elements present at co-ordinates X1: %d Y1: %d and X2: %d Y2: %d"
                        .formatted(x_element1, y_element1, x_element2, y_element2));
        multiTouchAction
                .addAction(finger1.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), x_element1,
                        y_element1))
                .addAction(finger1.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger1.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        multiTouchAction2
                .addAction(finger2.createPointerMove(Duration.ofMillis(1), PointerInput.Origin.viewport(), x_element2,
                        y_element2))
                .addAction(finger2.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger2.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));

        appiumDriver.perform(asList(multiTouchAction, multiTouchAction2));
    }

    public void relaunchApp() {
        String appPackageName = Runner.getAppPackageName();
        switch (Runner.getPlatform()) {
            case android:
                ((AndroidDriver) driver).terminateApp(appPackageName);
                ((AndroidDriver) driver).activateApp(appPackageName);
                break;
            case iOS:
                ((IOSDriver) driver).terminateApp(appPackageName);
                ((IOSDriver) driver).activateApp(appPackageName);
                break;
            default:
                throw new NotImplementedException("relaunchApp method is not implemented for " + Runner.getPlatform());
        }
    }

    public boolean waitTillElementIsInvisible(By elementId) {
        return waitTillElementIsInvisible(elementId, DriverDefaults.waitTimeoutSeconds());
    }

    public boolean waitTillElementIsInvisible(By elementId, int numberOfSecondsToWait) {
        return (new WebDriverWait(driver, Duration.ofSeconds(numberOfSecondsToWait))
                .until(ExpectedConditions.invisibilityOfElementLocated(elementId)));
    }

    public boolean isElementDisplayed(By locator) {
        List<WebElement> elementList = driver.findElements(locator);
        return !elementList.isEmpty() && elementList.get(0).isDisplayed();
    }

    public boolean isElementDisplayed(WebElement element) {
        try {
            return element.isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getClipboardText() {
        return switch (Runner.getPlatform()) {
            case android -> ((AndroidDriver) driver).getClipboardText();
            case iOS -> ((IOSDriver) driver).getClipboardText();
            default ->
                throw new NotImplementedException(
                        "getClipboardText method is not implemented for " + Runner.getPlatform());
        };
    }

    public void setClipboardText(String text) {
        switch (Runner.getPlatform()) {
            case android -> ((AndroidDriver) driver).setClipboardText(text);
            case iOS -> ((IOSDriver) driver).setClipboardText(text);
            default ->
                throw new NotImplementedException(
                        "setClipboardText method is not implemented for " + Runner.getPlatform());
        }
    }

    public WebElement click(By elementId) {
        WebElement element = waitForClickabilityOf(elementId);
        element.click();
        return element;
    }

    public WebElement click(By elementId, int numberOfSecondsToWait) {
        WebElement element = waitForClickabilityOf(elementId, numberOfSecondsToWait);
        element.click();
        return element;
    }

    public boolean switchToWebViewContextSafely() {
        try {
            setWebViewContext();
            return true;
        } catch (RuntimeException e) {
            if (driverForPlatform == Platform.android) {
                throw e;
            }
            LOGGER.warn("Best-effort web view context switch failed on {}: {}",
                    driverForPlatform, e.getMessage());
            return false;
        }
    }

    public void switchToNativeContextSafely() {
        try {
            setNativeAppContext();
        } catch (RuntimeException e) {
            if (driverForPlatform == Platform.android) {
                throw e;
            }
            LOGGER.warn("Best-effort native context switch failed on {}: {}",
                    driverForPlatform, e.getMessage());
        }
    }

    public void clickAndWaitForElement(By elementToClick, By elementToWaitFor) {
        clickAndWaitForElement(elementToClick, elementToWaitFor,
                DriverDefaults.clickRetryAttempts(), DriverDefaults.waitTimeoutSeconds());
    }

    public void clickAndWaitForElement(By elementToClick, By elementToWaitFor,
            int maxAttempts, int timeoutPerAttemptInSeconds) {
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                waitForClickabilityOf(elementToClick, timeoutPerAttemptInSeconds).click();
                waitTillElementIsPresent(elementToWaitFor, timeoutPerAttemptInSeconds);
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                waitFor(DriverDefaults.clickRetryDelaySeconds());
            }
        }
        throw new RuntimeException(
                "Unable to click '" + elementToClick + "' and reach '" + elementToWaitFor + "'",
                lastFailure);
    }

    public void clickWithFallbackAndWaitForDisappearance(By primary, By fallback,
            By elementToDisappear,
            int timeoutInSeconds) {
        waitTillElementIsPresent(elementToDisappear, timeoutInSeconds);
        try {
            waitForClickabilityOf(primary, timeoutInSeconds).click();
            waitTillElementIsInvisible(elementToDisappear, timeoutInSeconds);
            return;
        } catch (RuntimeException ignored) {
            // primary may not be hit-testable; fall through to the secondary control
        }
        try {
            waitForClickabilityOf(fallback, timeoutInSeconds).click();
            waitTillElementIsInvisible(elementToDisappear, timeoutInSeconds);
        } catch (RuntimeException e) {
            throw new RuntimeException("Unable to dismiss dialog: " + elementToDisappear, e);
        }
    }

    public VisualElement findByText(String text) {
        return this.visually.findByText(text);
    }

    public VisualElement findByImage(String... imageTemplatePaths) {
        return findByImage(Arrays.asList(imageTemplatePaths));
    }

    public VisualElement findByImage(List<String> imageTemplatePaths) {
        return this.visually.findByImage(imageTemplatePaths);
    }

    public VisualElement findByImage(List<String> imageTemplatePaths, double confidenceThreshold) {
        return this.visually.findByImage(imageTemplatePaths, confidenceThreshold);
    }

    public VisualElement findByImage(List<String> imageTemplatePaths, double confidenceThreshold, com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findByImage(imageTemplatePaths, confidenceThreshold, region);
    }

    public VisualElement findByTextOrImage(String text, String... imageTemplatePaths) {
        return findByTextOrImage(text, Arrays.asList(imageTemplatePaths));
    }

    public VisualElement findByTextOrImage(String text, List<String> imageTemplatePaths) {
        return this.visually.findByTextOrImage(text, imageTemplatePaths);
    }

    public VisualElement findByTextOrImage(String text, List<String> imageTemplatePaths, com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findByTextOrImage(text, imageTemplatePaths, region);
    }

    public VisualElement findByImageOrText(List<String> imageTemplatePaths, String text) {
        return this.visually.findByImageOrText(imageTemplatePaths, text);
    }

    public List<VisualElement> findAllByText(String text) {
        return this.visually.findAllByText(text);
    }

    public List<VisualElement> findAllByImage(String... imageTemplatePaths) {
        return findAllByImage(Arrays.asList(imageTemplatePaths));
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths) {
        return this.visually.findAllByImage(imageTemplatePaths);
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths, double confidenceThreshold) {
        return this.visually.findAllByImage(imageTemplatePaths, confidenceThreshold);
    }

    public List<VisualElement> findAllByTextOrImage(String text, String... imageTemplatePaths) {
        return findAllByTextOrImage(text, Arrays.asList(imageTemplatePaths));
    }

    public List<VisualElement> findAllByTextOrImage(String text, List<String> imageTemplatePaths) {
        return this.visually.findAllByTextOrImage(text, imageTemplatePaths);
    }

    public List<VisualElement> findAllByImageOrText(List<String> imageTemplatePaths, String text) {
        return this.visually.findAllByImageOrText(imageTemplatePaths, text);
    }

    public VisualElement findRelativeByText(String targetText, com.znsio.teswiz.entities.SpatialDirection direction, String anchorText) {
        return this.visually.findRelativeByText(targetText, direction, anchorText);
    }

    public VisualElement findRelativeByText(String targetText, com.znsio.teswiz.entities.SpatialDirection direction, VisualElement anchor) {
        return this.visually.findRelativeByText(targetText, direction, anchor);
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths, com.znsio.teswiz.entities.SpatialDirection direction, String anchorText) {
        return this.visually.findRelativeByImage(targetImagePaths, direction, anchorText);
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths, com.znsio.teswiz.entities.SpatialDirection direction, VisualElement anchor) {
        return this.visually.findRelativeByImage(targetImagePaths, direction, anchor);
    }

    public VisualElement findByText(String text, com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findByText(text, region);
    }

    public VisualElement findByImage(List<String> imageTemplatePaths, com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findByImage(imageTemplatePaths, region);
    }

    public List<VisualElement> findAllByText(String text, com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findAllByText(text, region);
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths, com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findAllByImage(imageTemplatePaths, region);
    }

    public WebElement findElement(VisualBy visualBy) {
        if (visualBy == null) {
            return null;
        }
        switch (visualBy.getType()) {
            case OCR_TEXT:
                VisualElement ocrElement = visualBy.getRegion() != null ? findByText(visualBy.getText(), visualBy.getRegion()) : findByText(visualBy.getText());
                return ocrElement != null ? ocrElement.toWebElement() : null;
            case IMAGE_TEMPLATE:
                VisualElement imageElement = visualBy.getRegion() != null ? findByImage(List.of(visualBy.getImagePath()), visualBy.getConfidenceThreshold(), visualBy.getRegion()) : findByImage(List.of(visualBy.getImagePath()), visualBy.getConfidenceThreshold());
                return imageElement != null ? imageElement.toWebElement() : null;
            case FALLBACK_TEXT_IMAGE:
            default:
                VisualElement fallbackElement = visualBy.getRegion() != null ? findByTextOrImage(visualBy.getText(), List.of(visualBy.getImagePath()), visualBy.getRegion()) : findByTextOrImage(visualBy.getText(), List.of(visualBy.getImagePath()));
                return fallbackElement != null ? fallbackElement.toWebElement() : null;
        }
    }

    public List<WebElement> findElements(VisualBy visualBy) {
        if (visualBy == null) {
            return Collections.emptyList();
        }
        List<VisualElement> visualElements;
        switch (visualBy.getType()) {
            case OCR_TEXT:
                visualElements = findAllByText(visualBy.getText());
                break;
            case IMAGE_TEMPLATE:
                visualElements = findAllByImage(List.of(visualBy.getImagePath()), visualBy.getConfidenceThreshold());
                break;
            case FALLBACK_TEXT_IMAGE:
            default:
                visualElements = findAllByTextOrImage(visualBy.getText(), List.of(visualBy.getImagePath()));
                break;
        }
        return visualElements.stream().map(VisualElement::toWebElement).collect(Collectors.toList());
    }
}

