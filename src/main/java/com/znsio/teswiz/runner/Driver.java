package com.znsio.teswiz.runner;

import static com.znsio.teswiz.tools.Wait.waitFor;
import static java.util.Collections.singletonList;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.openqa.selenium.NoSuchElementException;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.stream.Collectors;

import org.openqa.selenium.interactions.Sequence;

import org.apache.commons.lang3.NotImplementedException;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.WrapsElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.exceptions.FileNotUploadedException;
import com.znsio.teswiz.exceptions.InvalidTestDataException;

import com.znsio.teswiz.entities.Direction;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
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
    private ElementWaiter elementWaiter;

    public Driver(String testName, Platform forPlatform, String userPersona, String appName,
            AppiumDriver appiumDriver) {
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

    /**
     * Bounded, non-throwing element-visibility/presence/text waits. See {@link ElementWaiter}. Lazily created and
     * cached; engine-agnostic (drives the underlying {@link WebDriver} via {@link By}).
     *
     * @return the per-driver {@link ElementWaiter}
     */
    public ElementWaiter elementWaiter() {
        if (elementWaiter == null) {
            elementWaiter = new ElementWaiter(driver);
        }
        return elementWaiter;
    }

    /**
     * Bounded, non-throwing visibility check. Convenience delegate to {@link ElementWaiter#isElementVisible(By, int)}.
     *
     * @param locator               the element locator (any {@link By}, including {@code PlaywrightBy.*})
     * @param numberOfSecondsToWait the bound, in seconds
     * @return {@code true} if visible within the bound, else {@code false}
     */
    public boolean isElementVisible(By locator, int numberOfSecondsToWait) {
        return elementWaiter().isElementVisible(locator, numberOfSecondsToWait);
    }

    /**
     * Bounded, non-throwing presence check. Convenience delegate to
     * {@link ElementWaiter#isElementPresentWithin(By, int)}.
     *
     * @param locator               the element locator (any {@link By})
     * @param numberOfSecondsToWait the bound, in seconds
     * @return {@code true} if present within the bound, else {@code false}
     */
    public boolean isElementPresentWithin(By locator, int numberOfSecondsToWait) {
        return elementWaiter().isElementPresentWithin(locator, numberOfSecondsToWait);
    }

    /**
     * Bounded, non-throwing text-presence check. Convenience delegate to
     * {@link ElementWaiter#waitTillTextIsPresent(By, String, int)}.
     *
     * @param locator               the element locator (any {@link By})
     * @param text                  the text expected to be present in the element
     * @param numberOfSecondsToWait the bound, in seconds
     * @return {@code true} if the text is present within the bound, else {@code false}
     */
    public boolean waitTillTextIsPresent(By locator, String text, int numberOfSecondsToWait) {
        return elementWaiter().waitTillTextIsPresent(locator, text, numberOfSecondsToWait);
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
        mobileGestures().scroll(fromPoint, toPoint);
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
        mobileGestures().scrollDownByScreenSize();
    }

    public void scrollVertically(int fromPercentScreenHeight, int toPercentScreenHeight, int percentScreenWidth) {
        mobileGestures().scrollVertically(fromPercentScreenHeight, toPercentScreenHeight, percentScreenWidth);
    }

    public void tapOnMiddleOfScreen() {
        if (this.type.equals(Driver.APPIUM_DRIVER)) {
            tapOnMiddleOfScreenOnDevice();
        } else {
            simulateMouseMovementOnBrowser();
        }
    }

    private void tapOnMiddleOfScreenOnDevice() {
        mobileGestures().tapOnMiddleOfScreenOnDevice();
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

    public void swipeRight() {
        mobileGestures().swipeRight();
    }

    public void swipeLeft() {
        mobileGestures().swipeLeft();
    }

    public void swipeByPassingPercentageAttributes(int percentScreenHeight, int fromPercentScreenWidth,
            int toPercentScreenWidth) {
        mobileGestures().swipeByPassingPercentageAttributes(percentScreenHeight, fromPercentScreenWidth,
                toPercentScreenWidth);
    }

    public void openNotifications() {
        LOGGER.info("Fetching the NOTIFICATIONS on the device: ");
        waitFor(3);
        mobileGestures().openNotifications();
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
        mobileGestures().putAppInBackgroundFor(numberOfSeconds);
    }

    public void bringAppInForeground() {
        mobileGestures().bringAppInForeground();
    }

    public void goToDeepLinkUrl(String url, String packageName) {
        mobileGestures().goToDeepLinkUrl(url, packageName);
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
        mobileGestures().pushFileToDevice(filePathToPush, devicePath);
    }

    public void allowPermission(By element) {
        waitForClickabilityOf(element);
        if (Runner.getPlatform().equals(Platform.android)) {
            findElement(element).click();
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

    private MobileGestures mobileGestures;

    private MobileGestures mobileGestures() {
        if (this.mobileGestures == null) {
            this.mobileGestures = new MobileGestures((AppiumDriver) this.driver);
        }
        return this.mobileGestures;
    }

    private ElementHighlighter elementHighlighter;

    private ElementHighlighter elementHighlighter() {
        if (this.elementHighlighter == null) {
            this.elementHighlighter = new ElementHighlighter(driver,
                    APPIUM_DRIVER.equals(type) || driver instanceof AppiumDriver);
        }
        return this.elementHighlighter;
    }

    public org.openqa.selenium.Rectangle getActiveHighlightBounds() {
        return elementHighlighter().getActiveHighlightBounds();
    }

    public void clearHighlight() {
        elementHighlighter().clearHighlight();
    }

    public void highlightElement(WebElement element) {
        elementHighlighter().highlightElement(element);
    }

    public void highlightVisualElement(int x, int y, int width, int height) {
        elementHighlighter().highlightVisualElement(x, y, width, height);
    }

    public void highlightVisualElement(VisualElement visualElement) {
        elementHighlighter().highlightVisualElement(visualElement);
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
        if (Proxy.isProxyClass(element.getClass())
                && Proxy.getInvocationHandler(element) instanceof ElementInvocationHandler) {
            return element;
        }
        if (element.getClass().getName().contains("Playwright")) {
            return element;
        }
        return (WebElement) Proxy.newProxyInstance(Driver.class.getClassLoader(),
                new Class<?>[] { WebElement.class, WrapsElement.class }, new ElementInvocationHandler(element));
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
            if ("click".equals(methodName) || "sendKeys".equals(methodName) || "clear".equals(methodName)
                    || "submit".equals(methodName)) {
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
                        LOGGER.info(String.format(
                                "Calculated mobile viewport scale factor: %.2f (screenshot width: %d px, window width: %d px)",
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
                Object result = js.executeScript(
                        "return window.innerWidth || document.documentElement.clientWidth || document.body.clientWidth;");
                if (result instanceof Number num && num.doubleValue() > 0) {
                    double viewportWidth = num.doubleValue();
                    double scale = (double) screenshotImageWidth / viewportWidth;
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info(String.format(
                                "Calculated viewport scale factor: %.2f (screenshot width: %d px, viewport width: %.0f px)",
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
        throw new NoSuchElementException("scrollTillElementIntoView: element '" + elementId + "' was not visible after "
                + maxScrollAttempts + " scroll attempts");
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
            findElement(locator).sendKeys(filePath);
        } catch (Exception e) {
            throw new FileNotUploadedException(
                    String.format("Error in uploading the file: '%s%s%s", filePath, TO, Runner.getPlatform().name()),
                    e);
        }
    }

    /**
     * This method injects the media to browserstack to perform, image scanning eg: QRcode,barcode etc Throws
     * NotImplementedException if platform is NOT android, and cloudName is NOT browserstack
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
     * This method injects the already uploaded media in browserstack(media url) to browserstack real device, image
     * scanning eg: QRcode,barcode etc Throws NotImplementedException if platform is NOT android, and cloudName is NOT
     * browserstack
     *
     * @param browserStackMediaUrl is a media url generated after uploading a file to browserstack cloud using BS API
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
        mobileGestures().doubleTap(element);
    }

    public void flick() {
        mobileGestures().flick();
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

    public void pinchAndZoomIn(WebElement element) {
        mobileGestures().pinchAndZoomIn(element);
    }

    public void pinchAndZoomOut(WebElement element) {
        mobileGestures().pinchAndZoomOut(element);
    }

    public void multiTouchOnElements(WebElement firstElement, WebElement secondElement) {
        mobileGestures().multiTouchOnElements(firstElement, secondElement);
    }

    public void relaunchApp() {
        mobileGestures().relaunchApp();
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
        return mobileGestures().getClipboardText();
    }

    public void setClipboardText(String text) {
        mobileGestures().setClipboardText(text);
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
            LOGGER.warn("Best-effort web view context switch failed on {}: {}", driverForPlatform, e.getMessage());
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
            LOGGER.warn("Best-effort native context switch failed on {}: {}", driverForPlatform, e.getMessage());
        }
    }

    public void clickAndWaitForElement(By elementToClick, By elementToWaitFor) {
        clickAndWaitForElement(elementToClick, elementToWaitFor, DriverDefaults.clickRetryAttempts(),
                DriverDefaults.waitTimeoutSeconds());
    }

    public void clickAndWaitForElement(By elementToClick, By elementToWaitFor, int maxAttempts,
            int timeoutPerAttemptInSeconds) {
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
        throw new RuntimeException("Unable to click '" + elementToClick + "' and reach '" + elementToWaitFor + "'",
                lastFailure);
    }

    public void clickWithFallbackAndWaitForDisappearance(By primary, By fallback, By elementToDisappear,
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
        return this.visually.findByImage(Arrays.asList(imageTemplatePaths));
    }

    public VisualElement findByImage(List<String> imageTemplatePaths) {
        return this.visually.findByImage(imageTemplatePaths);
    }

    public VisualElement findByImage(List<String> imageTemplatePaths, double confidenceThreshold) {
        return this.visually.findByImage(imageTemplatePaths, confidenceThreshold);
    }

    public VisualElement findByImage(List<String> imageTemplatePaths, double confidenceThreshold,
            com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findByImage(imageTemplatePaths, confidenceThreshold, region);
    }

    public VisualElement findByTextOrImage(String text, String... imageTemplatePaths) {
        return this.visually.findByTextOrImage(text, Arrays.asList(imageTemplatePaths));
    }

    public VisualElement findByTextOrImage(String text, List<String> imageTemplatePaths) {
        return this.visually.findByTextOrImage(text, imageTemplatePaths);
    }

    public VisualElement findByTextOrImage(String text, List<String> imageTemplatePaths,
            com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findByTextOrImage(text, imageTemplatePaths, region);
    }

    public VisualElement findByImageOrText(List<String> imageTemplatePaths, String text) {
        return this.visually.findByImageOrText(imageTemplatePaths, text);
    }

    public List<VisualElement> findAllByText(String text) {
        return this.visually.findAllByText(text);
    }

    public List<VisualElement> findAllByImage(String... imageTemplatePaths) {
        return this.visually.findAllByImage(Arrays.asList(imageTemplatePaths));
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths) {
        return this.visually.findAllByImage(imageTemplatePaths);
    }

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths, double confidenceThreshold) {
        return this.visually.findAllByImage(imageTemplatePaths, confidenceThreshold);
    }

    public List<VisualElement> findAllByTextOrImage(String text, String... imageTemplatePaths) {
        return this.visually.findAllByTextOrImage(text, Arrays.asList(imageTemplatePaths));
    }

    public List<VisualElement> findAllByTextOrImage(String text, List<String> imageTemplatePaths) {
        return this.visually.findAllByTextOrImage(text, imageTemplatePaths);
    }

    public List<VisualElement> findAllByImageOrText(List<String> imageTemplatePaths, String text) {
        return this.visually.findAllByImageOrText(imageTemplatePaths, text);
    }

    public VisualElement findRelativeByText(String targetText, com.znsio.teswiz.entities.SpatialDirection direction,
            String anchorText) {
        return this.visually.findRelativeByText(targetText, direction, anchorText);
    }

    public VisualElement findRelativeByText(String targetText, com.znsio.teswiz.entities.SpatialDirection direction,
            VisualElement anchor) {
        return this.visually.findRelativeByText(targetText, direction, anchor);
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths,
            com.znsio.teswiz.entities.SpatialDirection direction, String anchorText) {
        return this.visually.findRelativeByImage(targetImagePaths, direction, anchorText);
    }

    public VisualElement findRelativeByImage(List<String> targetImagePaths,
            com.znsio.teswiz.entities.SpatialDirection direction, VisualElement anchor) {
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

    public List<VisualElement> findAllByImage(List<String> imageTemplatePaths,
            com.znsio.teswiz.entities.VisualRegion region) {
        return this.visually.findAllByImage(imageTemplatePaths, region);
    }

    public WebElement findElement(VisualBy visualBy) {
        if (visualBy == null) {
            return null;
        }
        switch (visualBy.getType()) {
            case OCR_TEXT:
                VisualElement ocrElement = visualBy.getRegion() != null
                        ? this.visually.findByText(visualBy.getText(), visualBy.getRegion())
                        : this.visually.findByText(visualBy.getText());
                return ocrElement != null ? ocrElement.toWebElement() : null;
            case IMAGE_TEMPLATE:
                VisualElement imageElement = visualBy.getRegion() != null
                        ? this.visually.findByImage(List.of(visualBy.getImagePath()), visualBy.getConfidenceThreshold(),
                                visualBy.getRegion())
                        : this.visually.findByImage(List.of(visualBy.getImagePath()),
                                visualBy.getConfidenceThreshold());
                return imageElement != null ? imageElement.toWebElement() : null;
            case FALLBACK_TEXT_IMAGE:
            default:
                VisualElement fallbackElement = visualBy.getRegion() != null
                        ? this.visually.findByTextOrImage(visualBy.getText(), List.of(visualBy.getImagePath()),
                                visualBy.getRegion())
                        : this.visually.findByTextOrImage(visualBy.getText(), List.of(visualBy.getImagePath()));
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
                visualElements = this.visually.findAllByText(visualBy.getText());
                break;
            case IMAGE_TEMPLATE:
                visualElements = this.visually.findAllByImage(List.of(visualBy.getImagePath()),
                        visualBy.getConfidenceThreshold());
                break;
            case FALLBACK_TEXT_IMAGE:
            default:
                visualElements = this.visually.findAllByTextOrImage(visualBy.getText(),
                        List.of(visualBy.getImagePath()));
                break;
        }
        return visualElements.stream().map(VisualElement::toWebElement).collect(Collectors.toList());
    }
}
