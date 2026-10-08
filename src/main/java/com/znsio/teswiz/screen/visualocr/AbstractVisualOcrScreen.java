package com.znsio.teswiz.screen.visualocr;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.tools.Wait;

/**
 * Shared implementation of {@link VisualOcrScreen} for the convention-resolved platform screens. Owns the single,
 * platform-independent choreography for every visual action - find, assert found, highlight, screenshot "before",
 * act, wait, screenshot "after" - so platform subclasses only supply the injected {@link Driver}/{@link Visual}
 * and any platform-specific post-action step (e.g. dismissing the on-screen keyboard after text entry).
 *
 * <p>
 * Finding delegates to the {@link Driver} facade; the facade's platform implementation decides how to locate and
 * act. This class never inspects the driver type.
 */
public abstract class AbstractVisualOcrScreen extends VisualOcrScreen {
    private static final Logger LOGGER = LogManager.getLogger(AbstractVisualOcrScreen.class.getName());

    protected final Driver driver;
    protected final Visual visually;

    protected AbstractVisualOcrScreen(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
        LOGGER.info("Initialized {}", screenName());
    }

    /**
     * Hook for platform-specific work after text is entered into a visual element (default: nothing). Mobile
     * platforms override this to dismiss the on-screen keyboard.
     */
    protected void afterTextEntry() {
        // no-op by default
    }

    private String screenName() {
        return getClass().getSimpleName();
    }

    // ------------------------------------------------------------------------
    // Finders (delegate to the Driver facade)
    // ------------------------------------------------------------------------

    @Override
    public final VisualElement findVisualElementByText(String text) {
        LOGGER.info("Finding visual element by text: {}", text);
        return driver.findByText(text);
    }

    @Override
    public final VisualElement findVisualElementByImage(List<String> imagePaths) {
        LOGGER.info("Finding visual element by image: {}", imagePaths);
        return driver.findByImage(imagePaths);
    }

    @Override
    public final VisualElement findVisualElementByTextOrImage(String text, List<String> imagePaths) {
        LOGGER.info("Finding visual element by text or image: {} / {}", text, imagePaths);
        return driver.findByTextOrImage(text, imagePaths);
    }

    @Override
    public final VisualElement findVisualElementByImageOrText(List<String> imagePaths, String text) {
        LOGGER.info("Finding visual element by image or text: {} / {}", imagePaths, text);
        return driver.findByImageOrText(imagePaths, text);
    }

    @Override
    public final List<VisualElement> findAllVisualElementsByText(String text) {
        LOGGER.info("Finding all visual elements by text: {}", text);
        return driver.findAllByText(text);
    }

    @Override
    public final List<VisualElement> findAllVisualElementsByImage(List<String> imagePaths) {
        LOGGER.info("Finding all visual elements by image: {}", imagePaths);
        return driver.findAllByImage(imagePaths);
    }

    @Override
    public final List<VisualElement> findAllVisualElementsByTextOrImage(String text, List<String> imagePaths) {
        LOGGER.info("Finding all visual elements by text or image: {} / {}", text, imagePaths);
        return driver.findAllByTextOrImage(text, imagePaths);
    }

    @Override
    public final List<VisualElement> findAllVisualElementsByImageOrText(List<String> imagePaths, String text) {
        LOGGER.info("Finding all visual elements by image or text: {} / {}", imagePaths, text);
        return driver.findAllByImageOrText(imagePaths, text);
    }

    @Override
    public final VisualElement findVisualElementRelativeByText(String targetText, SpatialDirection direction, String anchorText) {
        LOGGER.info("Finding visual element '{}' {} anchor text '{}'", targetText, direction.getDirection(), anchorText);
        return driver.findRelativeByText(targetText, direction, anchorText);
    }

    @Override
    public final VisualElement findVisualElementRelativeByImage(List<String> imagePaths, SpatialDirection direction, String anchorText) {
        LOGGER.info("Finding visual element matching image {} {} anchor text '{}'", imagePaths, direction.getDirection(), anchorText);
        return driver.findRelativeByImage(imagePaths, direction, anchorText);
    }

    @Override
    public final VisualElement findVisualElementByTextInRegion(String text, VisualRegion region) {
        LOGGER.info("Finding visual element '{}' in region {}", text, region);
        return driver.findByText(text, region);
    }

    @Override
    public final VisualElement findVisualElementByImageInRegion(List<String> imagePaths, VisualRegion region) {
        LOGGER.info("Finding visual element matching image {} in region {}", imagePaths, region);
        return driver.findByImage(imagePaths, region);
    }

    // ------------------------------------------------------------------------
    // Strict actions (fail if the element is not found)
    // ------------------------------------------------------------------------

    @Override
    public final VisualOcrScreen clickVisualElementByText(String elementName, String ocrText) {
        LOGGER.info("Visually clicking element '{}' using OCR text '{}'", elementName, ocrText);
        VisualElement element = requireFound(findVisualElementByText(ocrText), byOcrText(elementName, ocrText));
        return performAction(element, VisualElement::click,
                "Before visual click OCR text: " + ocrText, "After visual click OCR text: " + ocrText);
    }

    @Override
    public final VisualOcrScreen clickVisualElementByImage(String elementName, List<String> imagePaths) {
        LOGGER.info("Visually clicking element '{}' using image template '{}'", elementName, imagePaths);
        VisualElement element = requireFound(findVisualElementByImage(imagePaths), byImage(elementName, imagePaths));
        return performAction(element, VisualElement::click,
                "Before visual click: " + elementName, "After visual click: " + elementName);
    }

    @Override
    public final VisualOcrScreen clickVisualElementByTextOrImage(String elementName, String ocrText, List<String> imagePaths) {
        LOGGER.info("Visually clicking element '{}' using fallback OCR text '{}' or image template '{}'", elementName, ocrText, imagePaths);
        VisualElement element = requireFound(findVisualElementByTextOrImage(ocrText, imagePaths), byTextOrImage(elementName));
        return performAction(element, VisualElement::click,
                "Before visual click: " + elementName, "After visual click: " + elementName);
    }

    @Override
    public final VisualOcrScreen clickVisualElementByImageOrText(String elementName, List<String> imagePaths, String ocrText) {
        LOGGER.info("Visually clicking element '{}' using fallback image template '{}' or OCR text '{}'", elementName, imagePaths, ocrText);
        VisualElement element = requireFound(findVisualElementByImageOrText(imagePaths, ocrText), byImageOrText(elementName));
        return performAction(element, VisualElement::click,
                "Before visual click: " + elementName, "After visual click: " + elementName);
    }

    @Override
    public final VisualOcrScreen enterTextIntoVisualElementByText(String elementName, String textToEnter, String ocrText) {
        LOGGER.info("Visually entering text '{}' into element '{}' using OCR text '{}'", textToEnter, elementName, ocrText);
        VisualElement element = requireFound(findVisualElementByText(ocrText), byOcrText(elementName, ocrText));
        return performAction(element, el -> {
                    el.sendKeys(textToEnter);
                    afterTextEntry();
                },
                "Before visual enter text: " + ocrText, "After visual enter text: " + ocrText);
    }

    @Override
    public final VisualOcrScreen doubleClickVisualElementByText(String elementName, String ocrText) {
        LOGGER.info("Visually double-clicking element '{}' using OCR text '{}'", elementName, ocrText);
        VisualElement element = requireFound(findVisualElementByText(ocrText), byOcrText(elementName, ocrText));
        return performAction(element, VisualElement::doubleClick,
                "Before visual double-click OCR text: " + ocrText, "After visual double-click OCR text: " + ocrText);
    }

    @Override
    public final VisualOcrScreen hoverVisualElementByText(String elementName, String ocrText) {
        LOGGER.info("Visually hovering over element '{}' using OCR text '{}'", elementName, ocrText);
        VisualElement element = requireFound(findVisualElementByText(ocrText), byOcrText(elementName, ocrText));
        return performAction(element, VisualElement::hover,
                "Before visual hover OCR text: " + ocrText, "After visual hover OCR text: " + ocrText);
    }

    @Override
    public final VisualOcrScreen longPressVisualElementByText(String elementName, String ocrText) {
        LOGGER.info("Visually long-pressing element '{}' using OCR text '{}'", elementName, ocrText);
        VisualElement element = requireFound(findVisualElementByText(ocrText), byOcrText(elementName, ocrText));
        return performAction(element, VisualElement::longPress,
                "Before visual long-press OCR text: " + ocrText, "After visual long-press OCR text: " + ocrText);
    }

    @Override
    public final VisualOcrScreen swipeOnVisualElementByText(String elementName, Direction direction, String ocrText) {
        LOGGER.info("Visually swiping {} on element '{}' using OCR text '{}'", direction, elementName, ocrText);
        VisualElement element = requireFound(findVisualElementByText(ocrText), byOcrText(elementName, ocrText));
        return performAction(element, el -> el.swipe(direction),
                "Before visual swipe " + direction + " OCR text: " + ocrText,
                "After visual swipe " + direction + " OCR text: " + ocrText);
    }

    @Override
    public final VisualOcrScreen clickVisualElementAtIndexByText(String elementName, int index, String ocrText) {
        LOGGER.info("Visually clicking element '{}' at index {} using OCR text '{}'", elementName, index, ocrText);
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        assertThat(elements).as("Expected at least " + (index + 1) + " visual elements matching OCR text '" + ocrText + "'").hasSizeGreaterThan(index);
        return performAction(elements.get(index), VisualElement::click,
                "Before visual click at index " + index + " OCR text: " + ocrText,
                "After visual click at index " + index + " OCR text: " + ocrText);
    }

    @Override
    public final VisualOcrScreen clickVisualElementByPositionByText(String elementName, String positionText, String ocrText) {
        LOGGER.info("Visually clicking '{}' element '{}' using OCR text '{}'", positionText, elementName, ocrText);
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        assertThat(elements).as("Expected at least 1 visual element matching OCR text '" + ocrText + "'").isNotEmpty();
        VisualElement target = elements.get(parsePositionIndex(positionText, elements.size()));
        return performAction(target, VisualElement::click,
                "Before visual click position '" + positionText + "' OCR text: " + ocrText,
                "After visual click position '" + positionText + "' OCR text: " + ocrText);
    }

    @Override
    public final VisualOcrScreen clickVisualElementByPositionByImage(String elementName, String positionText, List<String> imagePaths) {
        LOGGER.info("Visually clicking '{}' element '{}' using image template '{}'", positionText, elementName, imagePaths);
        List<VisualElement> elements = findAllVisualElementsByImage(imagePaths);
        assertThat(elements).as("Expected at least 1 visual element matching image template '" + imagePaths + "'").isNotEmpty();
        VisualElement target = elements.get(parsePositionIndex(positionText, elements.size()));
        return performAction(target, VisualElement::click,
                "Before visual click position '" + positionText + "' image: " + imagePaths,
                "After visual click position '" + positionText + "' image: " + imagePaths);
    }

    @Override
    public final VisualOcrScreen clickVisualElementRelativeByText(String elementName, String targetText, SpatialDirection direction, String anchorText) {
        LOGGER.info("Visually clicking element '{}' with OCR text '{}' {} anchor text '{}'", elementName, targetText, direction.getDirection(), anchorText);
        VisualElement element = requireFound(findVisualElementRelativeByText(targetText, direction, anchorText),
                "Visual element '" + elementName + "' relative to '" + anchorText + "' should be found");
        return performAction(element, VisualElement::click,
                "Before visual click relative: " + targetText, "After visual click relative: " + targetText);
    }

    // ------------------------------------------------------------------------
    // Inspection (highlight + single screenshot, no action, no wait)
    // ------------------------------------------------------------------------

    @Override
    public final VisualOcrScreen inspectVisualElementByText(String elementName, String ocrText) {
        LOGGER.info("Visually inspecting element '{}' using OCR text '{}'", elementName, ocrText);
        VisualElement element = requireFound(findVisualElementByText(ocrText), byOcrText(elementName, ocrText));
        element.highlight();
        visually.takeScreenshot(screenName(), "Inspecting OCR text: " + ocrText);
        return this;
    }

    @Override
    public final VisualOcrScreen inspectVisualElementByImage(String elementName, List<String> imagePaths) {
        LOGGER.info("Visually inspecting element '{}' using image template '{}'", elementName, imagePaths);
        VisualElement element = requireFound(findVisualElementByImage(imagePaths), byImage(elementName, imagePaths));
        element.highlight();
        visually.takeScreenshot(screenName(), "Inspecting image template: " + imagePaths);
        return this;
    }

    // ------------------------------------------------------------------------
    // Conditional (try) actions (return whether the element was found and acted on)
    // ------------------------------------------------------------------------

    @Override
    public final boolean tryClickVisualElementByText(String elementName, String ocrText) {
        LOGGER.info("Attempting optional visual click on element '{}' using OCR text '{}'", elementName, ocrText);
        return tryPerformClick(findVisualElementByText(ocrText),
                "Before try visual click OCR text: " + ocrText, "After try visual click OCR text: " + ocrText);
    }

    @Override
    public final boolean tryClickVisualElementByImage(String elementName, List<String> imagePaths) {
        LOGGER.info("Attempting optional visual click on element '{}' using image template '{}'", elementName, imagePaths);
        return tryPerformClick(findVisualElementByImage(imagePaths),
                "Before try visual click: " + elementName, "After try visual click: " + elementName);
    }

    @Override
    public final boolean tryClickVisualElementByTextOrImage(String elementName, String ocrText, List<String> imagePaths) {
        LOGGER.info("Attempting optional visual click on element '{}' using fallback OCR text '{}' or image template '{}'", elementName, ocrText, imagePaths);
        return tryPerformClick(findVisualElementByTextOrImage(ocrText, imagePaths),
                "Before try visual click: " + elementName, "After try visual click: " + elementName);
    }

    @Override
    public final boolean tryClickVisualElementByImageOrText(String elementName, List<String> imagePaths, String ocrText) {
        LOGGER.info("Attempting optional visual click on element '{}' using fallback image template '{}' or OCR text '{}'", elementName, imagePaths, ocrText);
        return tryPerformClick(findVisualElementByImageOrText(imagePaths, ocrText),
                "Before try visual click: " + elementName, "After try visual click: " + elementName);
    }

    @Override
    public final boolean tryClickVisualElementRelativeByText(String elementName, String targetText, SpatialDirection direction, String anchorText) {
        LOGGER.info("Attempting optional visual click on element '{}' with OCR text '{}' {} anchor text '{}'", elementName, targetText, direction.getDirection(), anchorText);
        try {
            return tryPerformClick(findVisualElementRelativeByText(targetText, direction, anchorText),
                    "Before try visual click relative: " + targetText, "After try visual click relative: " + targetText);
        } catch (RuntimeException e) {
            LOGGER.debug("Optional relative visual click failed: {}", e.getMessage());
            return false;
        }
    }

    // ------------------------------------------------------------------------
    // Shared choreography
    // ------------------------------------------------------------------------

    /**
     * The single visual-action choreography shared by every platform: highlight the element, capture a "before"
     * screenshot, perform the action, wait the configured settle time, then capture an "after" screenshot.
     *
     * @param element   the located element to act on
     * @param action    the gesture to perform on the element
     * @param beforeTag screenshot tag captured before the action
     * @param afterTag  screenshot tag captured after the action and settle wait
     * @return this screen, for chaining
     */
    private VisualOcrScreen performAction(VisualElement element, Consumer<VisualElement> action,
                                          String beforeTag, String afterTag) {
        element.highlight();
        visually.takeScreenshot(screenName(), beforeTag);
        action.accept(element);
        waitForVisualActionIfConfigured();
        visually.takeScreenshot(screenName(), afterTag);
        return this;
    }

    private boolean tryPerformClick(VisualElement element, String beforeTag, String afterTag) {
        if (null == element) {
            return false;
        }
        performAction(element, VisualElement::click, beforeTag, afterTag);
        return true;
    }

    private static VisualElement requireFound(VisualElement element, String description) {
        assertThat(element).as(description).isNotNull();
        return element;
    }

    protected final void waitForVisualActionIfConfigured() {
        int waitSeconds = Setup.getIntegerValueFromConfigs(Setup.VISUAL_ACTION_WAIT_SECONDS);
        if (waitSeconds > 0) {
            Wait.waitFor(waitSeconds);
        }
    }

    protected final int parsePositionIndex(String positionText, int listSize) {
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

    private static boolean isOrdinalWord(String normalized) {
        return normalized.endsWith("st") || normalized.endsWith("nd")
                || normalized.endsWith("rd") || normalized.endsWith("th");
    }

    private static String byOcrText(String elementName, String ocrText) {
        return "Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found";
    }

    private static String byImage(String elementName, List<String> imagePaths) {
        return "Visual element '" + elementName + "' matched by image template '" + imagePaths + "' should be found";
    }

    private static String byTextOrImage(String elementName) {
        return "Visual element '" + elementName + "' matched by text/image should be found";
    }

    private static String byImageOrText(String elementName) {
        return "Visual element '" + elementName + "' matched by image/text should be found";
    }
}
