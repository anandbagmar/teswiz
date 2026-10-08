package com.znsio.teswiz.businessLayer.visualocr;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.visualocr.VisualOcrScreen;

/**
 * Business layer for Visual OCR &amp; image-recognition interactions. Every public method is a thin,
 * well-traced orchestration over {@link VisualOcrScreen}, following the Feature -&gt; Step -&gt; BL -&gt; Screen
 * design pattern. Methods are grouped into: single-element finders, multi-element finders, presence
 * verification, count verification, strict actions, conditional (try) actions, position/relative/region
 * actions, and subsystem-disabled verification.
 */
public class VisualOcrBL {
    private static final Logger LOGGER = LogManager.getLogger(VisualOcrBL.class.getName());
    private static final String DEFAULT_USER_PERSONA = "DEFAULT";
    private static final String SUBSYSTEM_DISABLED_MESSAGE =
            "[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!";

    private final TestExecutionContext context;
    private final String currentUserPersona;
    private final Platform currentPlatform;

    public VisualOcrBL(String userPersona, Platform forPlatform) {
        long threadId = Thread.currentThread().getId();
        this.context = Runner.getTestExecutionContext(threadId);
        this.currentUserPersona = userPersona;
        this.currentPlatform = forPlatform;
        Runner.setCurrentDriverForUser(userPersona, forPlatform, context);
    }

    public VisualOcrBL() {
        this.context = null;
        this.currentUserPersona = DEFAULT_USER_PERSONA;
        this.currentPlatform = Platform.web;
    }

    // ------------------------------------------------------------------------
    // Single-element finders
    // ------------------------------------------------------------------------

    public VisualElement findVisualElementByText(String ocrText) {
        LOGGER.info("Finding visual element by OCR text '{}'", ocrText);
        VisualElement element = visualOcrScreen().findVisualElementByText(ocrText);
        LOGGER.info("Find by OCR text '{}' completed: {}", ocrText, describe(element));
        return element;
    }

    public VisualElement findVisualElementByImage(String imageTemplatePath) {
        LOGGER.info("Finding visual element by image template '{}'", imageTemplatePath);
        VisualElement element = visualOcrScreen().findVisualElementByImage(List.of(imageTemplatePath));
        LOGGER.info("Find by image template '{}' completed: {}", imageTemplatePath, describe(element));
        return element;
    }

    public VisualElement findVisualElementByImages(List<String> imageTemplatePaths) {
        LOGGER.info("Finding visual element by image templates {}", imageTemplatePaths);
        VisualElement element = visualOcrScreen().findVisualElementByImage(imageTemplatePaths);
        LOGGER.info("Find by image templates {} completed: {}", imageTemplatePaths, describe(element));
        return element;
    }

    public VisualElement findVisualElementByTextOrImage(String ocrText, String imageTemplatePath) {
        LOGGER.info("Finding visual element by OCR text '{}' falling back to image template '{}'", ocrText, imageTemplatePath);
        VisualElement element = visualOcrScreen().findVisualElementByTextOrImage(ocrText, List.of(imageTemplatePath));
        LOGGER.info("Find by OCR text '{}' or image template '{}' completed: {}", ocrText, imageTemplatePath, describe(element));
        return element;
    }

    public VisualElement findVisualElementByTextOrImages(String ocrText, List<String> imageTemplatePaths) {
        LOGGER.info("Finding visual element by OCR text '{}' falling back to image templates {}", ocrText, imageTemplatePaths);
        VisualElement element = visualOcrScreen().findVisualElementByTextOrImage(ocrText, imageTemplatePaths);
        LOGGER.info("Find by OCR text '{}' or image templates {} completed: {}", ocrText, imageTemplatePaths, describe(element));
        return element;
    }

    public VisualElement findVisualElementByImageOrText(String imageTemplatePath, String ocrText) {
        LOGGER.info("Finding visual element by image template '{}' falling back to OCR text '{}'", imageTemplatePath, ocrText);
        VisualElement element = visualOcrScreen().findVisualElementByImageOrText(List.of(imageTemplatePath), ocrText);
        LOGGER.info("Find by image template '{}' or OCR text '{}' completed: {}", imageTemplatePath, ocrText, describe(element));
        return element;
    }

    public VisualElement findVisualElementByImagesOrText(List<String> imageTemplatePaths, String ocrText) {
        LOGGER.info("Finding visual element by image templates {} falling back to OCR text '{}'", imageTemplatePaths, ocrText);
        VisualElement element = visualOcrScreen().findVisualElementByImageOrText(imageTemplatePaths, ocrText);
        LOGGER.info("Find by image templates {} or OCR text '{}' completed: {}", imageTemplatePaths, ocrText, describe(element));
        return element;
    }

    // ------------------------------------------------------------------------
    // Multi-element finders
    // ------------------------------------------------------------------------

    public List<VisualElement> findAllVisualElementsByText(String ocrText) {
        LOGGER.info("Finding all visual elements by OCR text '{}'", ocrText);
        List<VisualElement> elements = visualOcrScreen().findAllVisualElementsByText(ocrText);
        LOGGER.info("Find all by OCR text '{}' completed: {}", ocrText, describe(elements));
        return elements;
    }

    public List<VisualElement> findAllVisualElementsByImage(String imageTemplatePath) {
        LOGGER.info("Finding all visual elements by image template '{}'", imageTemplatePath);
        List<VisualElement> elements = visualOcrScreen().findAllVisualElementsByImage(List.of(imageTemplatePath));
        LOGGER.info("Find all by image template '{}' completed: {}", imageTemplatePath, describe(elements));
        return elements;
    }

    public List<VisualElement> findAllVisualElementsByImages(List<String> imageTemplatePaths) {
        LOGGER.info("Finding all visual elements by image templates {}", imageTemplatePaths);
        List<VisualElement> elements = visualOcrScreen().findAllVisualElementsByImage(imageTemplatePaths);
        LOGGER.info("Find all by image templates {} completed: {}", imageTemplatePaths, describe(elements));
        return elements;
    }

    public List<VisualElement> findAllVisualElementsByTextOrImage(String ocrText, String imageTemplatePath) {
        LOGGER.info("Finding all visual elements by OCR text '{}' falling back to image template '{}'", ocrText, imageTemplatePath);
        List<VisualElement> elements = visualOcrScreen().findAllVisualElementsByTextOrImage(ocrText, List.of(imageTemplatePath));
        LOGGER.info("Find all by OCR text '{}' or image template '{}' completed: {}", ocrText, imageTemplatePath, describe(elements));
        return elements;
    }

    public List<VisualElement> findAllVisualElementsByTextOrImages(String ocrText, List<String> imageTemplatePaths) {
        LOGGER.info("Finding all visual elements by OCR text '{}' falling back to image templates {}", ocrText, imageTemplatePaths);
        List<VisualElement> elements = visualOcrScreen().findAllVisualElementsByTextOrImage(ocrText, imageTemplatePaths);
        LOGGER.info("Find all by OCR text '{}' or image templates {} completed: {}", ocrText, imageTemplatePaths, describe(elements));
        return elements;
    }

    public List<VisualElement> findAllVisualElementsByImageOrText(String imageTemplatePath, String ocrText) {
        LOGGER.info("Finding all visual elements by image template '{}' falling back to OCR text '{}'", imageTemplatePath, ocrText);
        List<VisualElement> elements = findAllVisualElementsByImagePathsOrText(List.of(imageTemplatePath), ocrText);
        LOGGER.info("Find all by image template '{}' or OCR text '{}' completed: {}", imageTemplatePath, ocrText, describe(elements));
        return elements;
    }

    public List<VisualElement> findAllVisualElementsByImagesOrText(List<String> imageTemplatePaths, String ocrText) {
        LOGGER.info("Finding all visual elements by image templates {} falling back to OCR text '{}'", imageTemplatePaths, ocrText);
        List<VisualElement> elements = findAllVisualElementsByImagePathsOrText(imageTemplatePaths, ocrText);
        LOGGER.info("Find all by image templates {} or OCR text '{}' completed: {}", imageTemplatePaths, ocrText, describe(elements));
        return elements;
    }

    // ------------------------------------------------------------------------
    // Wait until visible
    //
    // Polls the matching finder until the element appears (non-null) or the wait budget elapses. The locator
    // engine returns a VisualElement only when it is matched on the current screen capture, so a non-null result
    // is the visibility signal. Each variant has an overload that takes an explicit maximum wait in seconds and
    // one that defaults the budget from configuration. Works for every engine (Selenium, Playwright-Java,
    // Playwright-TS, Appium) because it drives the already engine-agnostic findVisualElementBy* methods.
    // ------------------------------------------------------------------------

    public VisualElement waitUntilVisualElementIsVisibleByText(String ocrText) {
        return waitUntilVisualElementIsVisibleByText(ocrText, defaultMaxWaitSeconds());
    }

    public VisualElement waitUntilVisualElementIsVisibleByText(String ocrText, int maxWaitSeconds) {
        LOGGER.info("Waiting up to {}s for visual element to be visible by OCR text '{}'", maxWaitSeconds, ocrText);
        return pollUntilVisible(() -> visualOcrScreen().findVisualElementByText(ocrText), maxWaitSeconds,
                "OCR text '" + ocrText + "'");
    }

    public VisualElement waitUntilVisualElementIsVisibleByImage(String imageTemplatePath) {
        return waitUntilVisualElementIsVisibleByImage(imageTemplatePath, defaultMaxWaitSeconds());
    }

    public VisualElement waitUntilVisualElementIsVisibleByImage(String imageTemplatePath, int maxWaitSeconds) {
        LOGGER.info("Waiting up to {}s for visual element to be visible by image template '{}'", maxWaitSeconds, imageTemplatePath);
        return pollUntilVisible(() -> visualOcrScreen().findVisualElementByImage(List.of(imageTemplatePath)), maxWaitSeconds,
                "image template '" + imageTemplatePath + "'");
    }

    public VisualElement waitUntilVisualElementIsVisibleByTextOrImage(String ocrText, String imageTemplatePath) {
        return waitUntilVisualElementIsVisibleByTextOrImage(ocrText, imageTemplatePath, defaultMaxWaitSeconds());
    }

    public VisualElement waitUntilVisualElementIsVisibleByTextOrImage(String ocrText, String imageTemplatePath, int maxWaitSeconds) {
        LOGGER.info("Waiting up to {}s for visual element to be visible by OCR text '{}' or image template '{}'", maxWaitSeconds, ocrText, imageTemplatePath);
        return pollUntilVisible(() -> visualOcrScreen().findVisualElementByTextOrImage(ocrText, List.of(imageTemplatePath)), maxWaitSeconds,
                "OCR text '" + ocrText + "' or image template '" + imageTemplatePath + "'");
    }

    public VisualElement waitUntilVisualElementIsVisibleByImageOrText(String imageTemplatePath, String ocrText) {
        return waitUntilVisualElementIsVisibleByImageOrText(imageTemplatePath, ocrText, defaultMaxWaitSeconds());
    }

    public VisualElement waitUntilVisualElementIsVisibleByImageOrText(String imageTemplatePath, String ocrText, int maxWaitSeconds) {
        LOGGER.info("Waiting up to {}s for visual element to be visible by image template '{}' or OCR text '{}'", maxWaitSeconds, imageTemplatePath, ocrText);
        return pollUntilVisible(() -> visualOcrScreen().findVisualElementByImageOrText(List.of(imageTemplatePath), ocrText), maxWaitSeconds,
                "image template '" + imageTemplatePath + "' or OCR text '" + ocrText + "'");
    }

    // ------------------------------------------------------------------------
    // Presence verification
    // ------------------------------------------------------------------------

    public VisualOcrBL verifyVisualElementIsPresentByText(String elementName, String ocrText) {
        LOGGER.info("Verifying visual element '{}' is present using OCR text '{}'", elementName, ocrText);
        VisualElement element = visualOcrScreen().findVisualElementByText(ocrText);
        LOGGER.info("Presence check for '{}' by OCR text '{}' completed: {}", elementName, ocrText, describe(element));
        assertThat(element).as(presentByText(elementName, ocrText)).isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsNotPresentByText(String elementName, String ocrText) {
        LOGGER.info("Verifying visual element '{}' is NOT present using OCR text '{}'", elementName, ocrText);
        VisualElement element = visualOcrScreen().findVisualElementByText(ocrText);
        LOGGER.info("Absence check for '{}' by OCR text '{}' completed: {}", elementName, ocrText, describe(element));
        assertThat(element).as(notPresentByText(elementName, ocrText)).isNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByImage(String elementName, String imageTemplatePath) {
        LOGGER.info("Verifying visual element '{}' is present using image template '{}'", elementName, imageTemplatePath);
        VisualElement element = visualOcrScreen().findVisualElementByImage(List.of(imageTemplatePath));
        LOGGER.info("Presence check for '{}' by image template '{}' completed: {}", elementName, imageTemplatePath, describe(element));
        assertThat(element).as(presentByImage(elementName, imageTemplatePath)).isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsNotPresentByImage(String elementName, String imageTemplatePath) {
        LOGGER.info("Verifying visual element '{}' is NOT present using image template '{}'", elementName, imageTemplatePath);
        VisualElement element = visualOcrScreen().findVisualElementByImage(List.of(imageTemplatePath));
        LOGGER.info("Absence check for '{}' by image template '{}' completed: {}", elementName, imageTemplatePath, describe(element));
        assertThat(element).as(notPresentByImage(elementName, imageTemplatePath)).isNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByFallbackTextOrImage(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info("Verifying visual element '{}' is present using fallback OCR text '{}' or image template '{}'", elementName, ocrText, imageTemplatePath);
        VisualElement element = visualOcrScreen().findVisualElementByTextOrImage(ocrText, List.of(imageTemplatePath));
        LOGGER.info("Presence check for '{}' by OCR text '{}' or image template '{}' completed: {}", elementName, ocrText, imageTemplatePath, describe(element));
        assertThat(element).as("Visual element '" + elementName + "' matched by text/image should be found").isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByFallbackImageOrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info("Verifying visual element '{}' is present using fallback image template '{}' or OCR text '{}'", elementName, imageTemplatePath, ocrText);
        VisualElement element = visualOcrScreen().findVisualElementByImageOrText(List.of(imageTemplatePath), ocrText);
        LOGGER.info("Presence check for '{}' by image template '{}' or OCR text '{}' completed: {}", elementName, imageTemplatePath, ocrText, describe(element));
        assertThat(element).as("Visual element '" + elementName + "' matched by image/text should be found").isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentRelativeByText(String elementName, String targetText, String directionText, String anchorText) {
        LOGGER.info("Verifying visual element '{}' with OCR text '{}' present {} anchor text '{}'", elementName, targetText, directionText, anchorText);
        VisualElement element = visualOcrScreen().findVisualElementRelativeByText(targetText, SpatialDirection.fromString(directionText), anchorText);
        LOGGER.info("Relative presence check for '{}' ('{}' {} '{}') completed: {}", elementName, targetText, directionText, anchorText, describe(element));
        assertThat(element).as("Visual element '" + elementName + "' relative to '" + anchorText + "' should be found").isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentInRegion(String elementName, String ocrText, int x, int y, int width, int height) {
        VisualRegion region = VisualRegion.inRegion(x, y, width, height);
        LOGGER.info("Verifying visual element '{}' with OCR text '{}' is present in region {}", elementName, ocrText, region);
        VisualElement element = visualOcrScreen().findVisualElementByTextInRegion(ocrText, region);
        LOGGER.info("Region presence check for '{}' by OCR text '{}' in region {} completed: {}", elementName, ocrText, region, describe(element));
        assertThat(element).as("Visual element '" + elementName + "' in region " + region + " should be present").isNotNull();
        return this;
    }

    // ------------------------------------------------------------------------
    // Count verification
    // ------------------------------------------------------------------------

    public VisualOcrBL verifyVisualElementCountByText(String elementName, String ocrText, int expectedCount) {
        LOGGER.info("Verifying {} visual elements named '{}' present using OCR text '{}'", expectedCount, elementName, ocrText);
        List<VisualElement> elements = pollUntilExactCount(() -> findAllVisualElementsByText(ocrText), expectedCount);
        LOGGER.info("Count check for '{}' by OCR text '{}' completed: expected {}, {}", elementName, ocrText, expectedCount, describe(elements));
        assertThat(elements).as("Expected " + expectedCount + " visual elements matching OCR text '" + ocrText + "'").hasSize(expectedCount);
        return this;
    }

    public VisualOcrBL verifyVisualElementCountByImage(String elementName, String imageTemplatePath, int expectedCount) {
        LOGGER.info("Verifying {} visual elements named '{}' present using image template '{}'", expectedCount, elementName, imageTemplatePath);
        List<VisualElement> elements = pollUntilExactCount(() -> findAllVisualElementsByImage(imageTemplatePath), expectedCount);
        LOGGER.info("Count check for '{}' by image template '{}' completed: expected {}, {}", elementName, imageTemplatePath, expectedCount, describe(elements));
        assertThat(elements).as("Expected " + expectedCount + " visual elements matching image template '" + imageTemplatePath + "'").hasSize(expectedCount);
        return this;
    }

    public VisualOcrBL verifyAtLeastNVisualElementsPresentByText(String elementName, String ocrText, int minCount) {
        LOGGER.info("Verifying at least {} visual elements named '{}' present using OCR text '{}'", minCount, elementName, ocrText);
        List<VisualElement> elements = pollUntilAtLeastN(() -> findAllVisualElementsByText(ocrText), minCount);
        LOGGER.info("At-least-count check for '{}' by OCR text '{}' completed: expected at least {}, {}", elementName, ocrText, minCount, describe(elements));
        assertThat(elements).as("Expected at least " + minCount + " visual elements matching OCR text '" + ocrText + "'").hasSizeGreaterThanOrEqualTo(minCount);
        return this;
    }

    public VisualOcrBL verifyAtLeastNVisualElementsPresentByImage(String elementName, String imageTemplatePath, int minCount) {
        LOGGER.info("Verifying at least {} visual elements named '{}' present using image template '{}'", minCount, elementName, imageTemplatePath);
        List<VisualElement> elements = pollUntilAtLeastN(() -> findAllVisualElementsByImage(imageTemplatePath), minCount);
        LOGGER.info("At-least-count check for '{}' by image template '{}' completed: expected at least {}, {}", elementName, imageTemplatePath, minCount, describe(elements));
        assertThat(elements).as("Expected at least " + minCount + " visual elements matching image template '" + imageTemplatePath + "'").hasSizeGreaterThanOrEqualTo(minCount);
        return this;
    }

    // ------------------------------------------------------------------------
    // Strict actions (fail if the element is not found)
    // ------------------------------------------------------------------------

    public VisualOcrBL visuallyClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info("Visually clicking '{}' using OCR text '{}'", elementName, ocrText);
        visualOcrScreen().clickVisualElementByText(elementName, ocrText);
        LOGGER.info("Click on '{}' using OCR text '{}' completed", elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info("Visually clicking '{}' using image template '{}'", elementName, imageTemplatePath);
        visualOcrScreen().clickVisualElementByImage(elementName, List.of(imageTemplatePath));
        LOGGER.info("Click on '{}' using image template '{}' completed", elementName, imageTemplatePath);
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info("Visually clicking '{}' using fallback OCR text '{}' or image template '{}'", elementName, ocrText, imageTemplatePath);
        visualOcrScreen().clickVisualElementByTextOrImage(elementName, ocrText, List.of(imageTemplatePath));
        LOGGER.info("Click on '{}' using OCR text '{}' or image template '{}' completed", elementName, ocrText, imageTemplatePath);
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info("Visually clicking '{}' using fallback image template '{}' or OCR text '{}'", elementName, imageTemplatePath, ocrText);
        visualOcrScreen().clickVisualElementByImageOrText(elementName, List.of(imageTemplatePath), ocrText);
        LOGGER.info("Click on '{}' using image template '{}' or OCR text '{}' completed", elementName, imageTemplatePath, ocrText);
        return this;
    }

    public VisualOcrBL visuallyEnterTextUsingOcrText(String elementName, String textToEnter, String ocrText) {
        LOGGER.info("Visually entering text '{}' into '{}' using OCR text '{}'", textToEnter, elementName, ocrText);
        visualOcrScreen().enterTextIntoVisualElementByText(elementName, textToEnter, ocrText);
        LOGGER.info("Enter text into '{}' using OCR text '{}' completed", elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyDoubleClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info("Visually double-clicking '{}' using OCR text '{}'", elementName, ocrText);
        visualOcrScreen().doubleClickVisualElementByText(elementName, ocrText);
        LOGGER.info("Double-click on '{}' using OCR text '{}' completed", elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyHoverUsingOcrText(String elementName, String ocrText) {
        LOGGER.info("Visually hovering over '{}' using OCR text '{}'", elementName, ocrText);
        visualOcrScreen().hoverVisualElementByText(elementName, ocrText);
        LOGGER.info("Hover over '{}' using OCR text '{}' completed", elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyLongPressUsingOcrText(String elementName, String ocrText) {
        LOGGER.info("Visually long-pressing '{}' using OCR text '{}'", elementName, ocrText);
        visualOcrScreen().longPressVisualElementByText(elementName, ocrText);
        LOGGER.info("Long-press on '{}' using OCR text '{}' completed", elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallySwipeOnElementUsingOcrText(String elementName, String directionText, String ocrText) {
        LOGGER.info("Visually swiping '{}' on '{}' using OCR text '{}'", directionText, elementName, ocrText);
        visualOcrScreen().swipeOnVisualElementByText(elementName, Direction.valueOf(directionText.toUpperCase()), ocrText);
        LOGGER.info("Swipe '{}' on '{}' using OCR text '{}' completed", directionText, elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingOcrText(String elementName, String ocrText) {
        LOGGER.info("Visually inspecting '{}' using OCR text '{}'", elementName, ocrText);
        visualOcrScreen().inspectVisualElementByText(elementName, ocrText);
        LOGGER.info("Inspect of '{}' using OCR text '{}' completed", elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info("Visually inspecting '{}' using image template '{}'", elementName, imageTemplatePath);
        visualOcrScreen().inspectVisualElementByImage(elementName, List.of(imageTemplatePath));
        LOGGER.info("Inspect of '{}' using image template '{}' completed", elementName, imageTemplatePath);
        return this;
    }

    public VisualOcrBL visuallyClickElementAtIndexUsingOcrText(String elementName, int index, String ocrText) {
        LOGGER.info("Visually clicking '{}' at index {} using OCR text '{}'", elementName, index, ocrText);
        visualOcrScreen().clickVisualElementAtIndexByText(elementName, index, ocrText);
        LOGGER.info("Click on '{}' at index {} using OCR text '{}' completed", elementName, index, ocrText);
        return this;
    }

    public VisualOcrBL visuallyClickElementByPositionUsingOcrText(String elementName, String positionText, String ocrText) {
        LOGGER.info("Visually clicking '{}' at position '{}' using OCR text '{}'", elementName, positionText, ocrText);
        visualOcrScreen().clickVisualElementByPositionByText(elementName, positionText, ocrText);
        LOGGER.info("Click on '{}' at position '{}' using OCR text '{}' completed", elementName, positionText, ocrText);
        return this;
    }

    public VisualOcrBL visuallyClickElementByPositionUsingImageTemplate(String elementName, String positionText, String imageTemplatePath) {
        LOGGER.info("Visually clicking '{}' at position '{}' using image template '{}'", elementName, positionText, imageTemplatePath);
        visualOcrScreen().clickVisualElementByPositionByImage(elementName, positionText, List.of(imageTemplatePath));
        LOGGER.info("Click on '{}' at position '{}' using image template '{}' completed", elementName, positionText, imageTemplatePath);
        return this;
    }

    public VisualOcrBL visuallyClickUsingOcrTextRelative(String elementName, String targetText, String directionText, String anchorText) {
        LOGGER.info("Visually clicking '{}' with OCR text '{}' {} anchor text '{}'", elementName, targetText, directionText, anchorText);
        visualOcrScreen().clickVisualElementRelativeByText(elementName, targetText, SpatialDirection.fromString(directionText), anchorText);
        LOGGER.info("Relative click on '{}' ('{}' {} '{}') completed", elementName, targetText, directionText, anchorText);
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info("Visually finding all instances of '{}' using image template '{}'", elementName, imageTemplatePath);
        visualOcrScreen().inspectVisualElementByImage(elementName, List.of(imageTemplatePath));
        LOGGER.info("Find all instances of '{}' using image template '{}' completed", elementName, imageTemplatePath);
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingOcrText(String elementName, String ocrText) {
        LOGGER.info("Visually finding all instances of '{}' using OCR text '{}'", elementName, ocrText);
        visualOcrScreen().inspectVisualElementByText(elementName, ocrText);
        LOGGER.info("Find all instances of '{}' using OCR text '{}' completed", elementName, ocrText);
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info("Visually finding all instances of '{}' using fallback OCR text '{}' or image template '{}'", elementName, ocrText, imageTemplatePath);
        visualOcrScreen().clickVisualElementByTextOrImage(elementName, ocrText, List.of(imageTemplatePath));
        LOGGER.info("Find all instances of '{}' using OCR text '{}' or image template '{}' completed", elementName, ocrText, imageTemplatePath);
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info("Visually finding all instances of '{}' using fallback image template '{}' or OCR text '{}'", elementName, imageTemplatePath, ocrText);
        visualOcrScreen().clickVisualElementByImageOrText(elementName, List.of(imageTemplatePath), ocrText);
        LOGGER.info("Find all instances of '{}' using image template '{}' or OCR text '{}' completed", elementName, imageTemplatePath, ocrText);
        return this;
    }

    // ------------------------------------------------------------------------
    // Conditional (try) actions (return whether the action succeeded)
    // ------------------------------------------------------------------------

    public boolean tryVisuallyClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info("Attempting to visually click '{}' using OCR text '{}'", elementName, ocrText);
        boolean clicked = visualOcrScreen().tryClickVisualElementByText(elementName, ocrText);
        LOGGER.info("Attempt to click '{}' using OCR text '{}' completed: {}", elementName, ocrText, clicked ? "clicked" : "element not found");
        return clicked;
    }

    public boolean tryVisuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info("Attempting to visually click '{}' using image template '{}'", elementName, imageTemplatePath);
        boolean clicked = visualOcrScreen().tryClickVisualElementByImage(elementName, List.of(imageTemplatePath));
        LOGGER.info("Attempt to click '{}' using image template '{}' completed: {}", elementName, imageTemplatePath, clicked ? "clicked" : "element not found");
        return clicked;
    }

    public boolean tryVisuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info("Attempting to visually click '{}' using fallback OCR text '{}' or image template '{}'", elementName, ocrText, imageTemplatePath);
        boolean clicked = visualOcrScreen().tryClickVisualElementByTextOrImage(elementName, ocrText, List.of(imageTemplatePath));
        LOGGER.info("Attempt to click '{}' using OCR text '{}' or image template '{}' completed: {}", elementName, ocrText, imageTemplatePath, clicked ? "clicked" : "element not found");
        return clicked;
    }

    public boolean tryVisuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info("Attempting to visually click '{}' using fallback image template '{}' or OCR text '{}'", elementName, imageTemplatePath, ocrText);
        boolean clicked = visualOcrScreen().tryClickVisualElementByImageOrText(elementName, List.of(imageTemplatePath), ocrText);
        LOGGER.info("Attempt to click '{}' using image template '{}' or OCR text '{}' completed: {}", elementName, imageTemplatePath, ocrText, clicked ? "clicked" : "element not found");
        return clicked;
    }

    public boolean tryVisuallyClickUsingOcrTextRelative(String elementName, String targetText, String directionText, String anchorText) {
        LOGGER.info("Attempting to visually click '{}' with OCR text '{}' {} anchor text '{}'", elementName, targetText, directionText, anchorText);
        boolean clicked = visualOcrScreen().tryClickVisualElementRelativeByText(elementName, targetText, SpatialDirection.fromString(directionText), anchorText);
        LOGGER.info("Attempt to relatively click '{}' ('{}' {} '{}') completed: {}", elementName, targetText, directionText, anchorText, clicked ? "clicked" : "element not found");
        return clicked;
    }

    // ------------------------------------------------------------------------
    // Subsystem-disabled verification
    // ------------------------------------------------------------------------

    public VisualOcrBL verifyVisualOcrCapabilityDisabled() {
        LOGGER.info("Verifying IS_OCR_ENABLED is set to false");
        assertThat(Runner.isOcrEnabled()).isFalse();
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForText(String text) {
        LOGGER.info("Verifying findByText throws VisualSubsystemDisabledException for text: {}", text);
        return assertDisabledSubsystemThrows(() -> visualOcrScreen().findVisualElementByText(text));
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForImage(String imagePath) {
        LOGGER.info("Verifying findByImage throws VisualSubsystemDisabledException for image: {}", imagePath);
        return assertDisabledSubsystemThrows(() -> visualOcrScreen().findVisualElementByImage(List.of(imagePath)));
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForTextOrImage(String text, String imagePath) {
        LOGGER.info("Verifying findByTextOrImage throws VisualSubsystemDisabledException for text: {}, image: {}", text, imagePath);
        return assertDisabledSubsystemThrows(() -> visualOcrScreen().findVisualElementByTextOrImage(text, List.of(imagePath)));
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForImageOrText(String imagePath, String text) {
        LOGGER.info("Verifying findByImageOrText throws VisualSubsystemDisabledException for image: {}, text: {}", imagePath, text);
        return assertDisabledSubsystemThrows(() -> visualOcrScreen().findVisualElementByImageOrText(List.of(imagePath), text));
    }

    // ------------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------------

    public int parsePositionIndex(String positionText, int listSize) {
        if (positionText == null || positionText.isBlank()) {
            return 0;
        }
        String normalized = positionText.trim().toLowerCase();
        if ("first".equals(normalized) || "1st".equals(normalized)) {
            return 0;
        }
        if ("last".equals(normalized)) {
            return Math.max(0, listSize - 1);
        }
        if ("second".equals(normalized) || "2nd".equals(normalized)) {
            return 1;
        }
        if ("third".equals(normalized) || "3rd".equals(normalized)) {
            return 2;
        }
        if (isOrdinalWord(normalized)) {
            String digits = normalized.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) {
                return Integer.parseInt(digits) - 1;
            }
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ------------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------------

    private static VisualOcrScreen visualOcrScreen() {
        return VisualOcrScreen.get();
    }

    private static String describe(VisualElement element) {
        return element == null ? "no matching element found" : "found " + element;
    }

    private static String describe(List<VisualElement> elements) {
        return "found " + elements.size() + " matching element(s)";
    }

    private VisualOcrBL assertDisabledSubsystemThrows(Runnable disabledOperation) {
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(disabledOperation::run)
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining(SUBSYSTEM_DISABLED_MESSAGE);
        return this;
    }

    private List<VisualElement> findAllVisualElementsByImagePathsOrText(List<String> imageTemplatePaths, String ocrText) {
        return visualOcrScreen().findAllVisualElementsByImageOrText(imageTemplatePaths, ocrText);
    }

    /**
     * The default maximum wait budget (in seconds) for waitUntilVisible* when no explicit value is given.
     * Derived from the existing retry configuration (attempts x per-attempt delay) so wait-until-visible stays
     * consistent with the rest of the visual retry behaviour and needs no separate config property.
     *
     * @return the default wait budget in seconds, at least one
     */
    private int defaultMaxWaitSeconds() {
        int attempts = Math.max(1, Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS));
        int delaySeconds = Math.max(1, Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS));
        return attempts * delaySeconds;
    }

    /**
     * Polls a single-element finder until it returns a visible (non-null) element or the wait budget elapses,
     * asserting the element was found. A non-null result from the finder is the visibility signal: the locator
     * engine returns an element only when it is matched on the current screen capture.
     *
     * @param finder          supplies a fresh find result on each poll
     * @param maxWaitSeconds  the maximum time to wait, in seconds (at least one)
     * @param matcherText     a human description of the locator, for logging and assertion messages
     * @return the located visible element
     */
    private VisualElement pollUntilVisible(Supplier<VisualElement> finder, int maxWaitSeconds, String matcherText) {
        int budgetSeconds = Math.max(1, maxWaitSeconds);
        int delaySeconds = Math.max(1, Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS));
        long deadline = System.currentTimeMillis() + budgetSeconds * 1000L;
        VisualElement element = finder.get();
        while (element == null && System.currentTimeMillis() < deadline) {
            sleepQuietly(delaySeconds * 1000);
            element = finder.get();
        }
        LOGGER.info("Wait for visual element by {} completed: {}", matcherText, describe(element));
        assertThat(element)
                .as("Visual element matched by " + matcherText + " should become visible within " + budgetSeconds + "s")
                .isNotNull();
        return element;
    }

    private List<VisualElement> pollUntilAtLeastN(Supplier<List<VisualElement>> query, int minCount) {
        return pollUntil(query, elements -> elements.size() >= minCount);
    }

    private List<VisualElement> pollUntilExactCount(Supplier<List<VisualElement>> query, int expectedCount) {
        return pollUntil(query, elements -> elements.size() == expectedCount);
    }

    private List<VisualElement> pollUntil(Supplier<List<VisualElement>> query,
                                          Predicate<List<VisualElement>> satisfied) {
        int maxAttempts = Math.max(1, Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS));
        int retryDelayMs = Math.max(1, Setup.getIntegerValueFromConfigs(Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS)) * 1000;
        List<VisualElement> elements = Collections.emptyList();
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            elements = query.get();
            if (satisfied.test(elements)) {
                return elements;
            }
            if (attempt < maxAttempts) {
                sleepQuietly(retryDelayMs);
            }
        }
        return elements;
    }

    private static void sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean isOrdinalWord(String normalized) {
        return normalized.endsWith("st") || normalized.endsWith("nd")
                || normalized.endsWith("rd") || normalized.endsWith("th");
    }

    private static String presentByText(String elementName, String ocrText) {
        return "Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found";
    }

    private static String notPresentByText(String elementName, String ocrText) {
        return "Visual element '" + elementName + "' with OCR text '" + ocrText + "' should NOT be present";
    }

    private static String presentByImage(String elementName, String imageTemplatePath) {
        return "Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should be found";
    }

    private static String notPresentByImage(String elementName, String imageTemplatePath) {
        return "Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should NOT be present";
    }
}
