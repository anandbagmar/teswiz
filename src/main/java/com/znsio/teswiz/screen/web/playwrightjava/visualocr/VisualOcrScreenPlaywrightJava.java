package com.znsio.teswiz.screen.web.playwrightjava.visualocr;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Visual;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.visualocr.VisualOcrScreen;

public class VisualOcrScreenPlaywrightJava extends VisualOcrScreen {
    private static final String SCREEN_NAME = VisualOcrScreenPlaywrightJava.class.getSimpleName();
    private static final Logger LOGGER = LogManager.getLogger(SCREEN_NAME);

    private final Driver driver;
    private final Visual visually;

    public VisualOcrScreenPlaywrightJava(Driver driver, Visual visually) {
        this.driver = driver;
        this.visually = visually;
        LOGGER.info("Initialized " + SCREEN_NAME + " for Playwright Java web engine");
    }

    @Override
    public VisualElement findVisualElementByText(String text) {
        LOGGER.info("Playwright Java - Finding visual element by text: " + text);
        return driver.findByText(text);
    }

    @Override
    public VisualElement findVisualElementByImage(List<String> imagePaths) {
        LOGGER.info("Playwright Java - Finding visual element by image: " + imagePaths);
        return driver.findByImage(imagePaths);
    }

    @Override
    public VisualElement findVisualElementByTextOrImage(String text, List<String> imagePaths) {
        LOGGER.info("Playwright Java - Finding visual element by text or image: " + text + " / " + imagePaths);
        return driver.findByTextOrImage(text, imagePaths);
    }

    @Override
    public VisualElement findVisualElementByImageOrText(List<String> imagePaths, String text) {
        LOGGER.info("Playwright Java - Finding visual element by image or text: " + imagePaths + " / " + text);
        return driver.findByImageOrText(imagePaths, text);
    }

    @Override
    public List<VisualElement> findAllVisualElementsByText(String text) {
        LOGGER.info("Playwright Java - Finding all visual elements by text: " + text);
        return driver.findAllByText(text);
    }

    @Override
    public List<VisualElement> findAllVisualElementsByImage(List<String> imagePaths) {
        LOGGER.info("Playwright Java - Finding all visual elements by image: " + imagePaths);
        return driver.findAllByImage(imagePaths);
    }

    @Override
    public List<VisualElement> findAllVisualElementsByTextOrImage(String text, List<String> imagePaths) {
        LOGGER.info("Playwright Java - Finding all visual elements by text or image: " + text + " / " + imagePaths);
        return driver.findAllByTextOrImage(text, imagePaths);
    }

    @Override
    public List<VisualElement> findAllVisualElementsByImageOrText(List<String> imagePaths, String text) {
        LOGGER.info("Playwright Java - Finding all visual elements by image or text: " + imagePaths + " / " + text);
        return driver.findAllByImageOrText(imagePaths, text);
    }

    @Override
    public VisualElement findVisualElementRelativeByText(String targetText, SpatialDirection direction, String anchorText) {
        LOGGER.info(String.format("Playwright Java - Finding visual element '%s' %s anchor text '%s'", targetText, direction.getDirection(), anchorText));
        return driver.findRelativeByText(targetText, direction, anchorText);
    }

    @Override
    public VisualElement findVisualElementRelativeByImage(List<String> imagePaths, SpatialDirection direction, String anchorText) {
        LOGGER.info(String.format("Playwright Java - Finding visual element matching image %s %s anchor text '%s'", imagePaths, direction.getDirection(), anchorText));
        return driver.findRelativeByImage(imagePaths, direction, anchorText);
    }

    @Override
    public VisualElement findVisualElementByTextInRegion(String text, VisualRegion region) {
        LOGGER.info(String.format("Playwright Java - Finding visual element '%s' in region %s", text, region));
        return driver.findByText(text, region);
    }

    @Override
    public VisualElement findVisualElementByImageInRegion(List<String> imagePaths, VisualRegion region) {
        LOGGER.info(String.format("Playwright Java - Finding visual element matching image %s in region %s", imagePaths, region));
        return driver.findByImage(imagePaths, region);
    }

    @Override
    public VisualOcrScreen clickVisualElementByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually clicking element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual click OCR text: " + ocrText);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual click OCR text: " + ocrText);
        return this;
    }

    @Override
    public VisualOcrScreen clickVisualElementByImage(String elementName, List<String> imagePaths) {
        LOGGER.info(String.format("Playwright Java - Visually clicking element '%s' using image template '%s'", elementName, imagePaths));
        VisualElement element = findVisualElementByImage(imagePaths);
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imagePaths + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual click: " + elementName);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual click: " + elementName);
        return this;
    }

    @Override
    public VisualOcrScreen clickVisualElementByTextOrImage(String elementName, String ocrText, List<String> imagePaths) {
        LOGGER.info(String.format("Playwright Java - Visually clicking element '%s' using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imagePaths));
        VisualElement element = findVisualElementByTextOrImage(ocrText, imagePaths);
        assertThat(element).as("Visual element '" + elementName + "' matched by text/image should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual click: " + elementName);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual click: " + elementName);
        return this;
    }

    @Override
    public VisualOcrScreen clickVisualElementByImageOrText(String elementName, List<String> imagePaths, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually clicking element '%s' using fallback image template '%s' or OCR text '%s'", elementName, imagePaths, ocrText));
        VisualElement element = findVisualElementByImageOrText(imagePaths, ocrText);
        assertThat(element).as("Visual element '" + elementName + "' matched by image/text should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual click: " + elementName);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual click: " + elementName);
        return this;
    }

    @Override
    public VisualOcrScreen enterTextIntoVisualElementByText(String elementName, String textToEnter, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually clicking element '%s' using OCR text '%s' and entering text '%s'", elementName, ocrText, textToEnter));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual enter text: " + ocrText);
        element.sendKeys(textToEnter);
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual enter text: " + ocrText);
        return this;
    }

    @Override
    public VisualOcrScreen inspectVisualElementByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually inspecting element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Inspecting OCR text: " + ocrText);
        driver.clearHighlight();
        return this;
    }

    @Override
    public VisualOcrScreen inspectVisualElementByImage(String elementName, List<String> imagePaths) {
        LOGGER.info(String.format("Playwright Java - Visually inspecting element '%s' using image template '%s'", elementName, imagePaths));
        VisualElement element = findVisualElementByImage(imagePaths);
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imagePaths + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Inspecting image template: " + imagePaths);
        driver.clearHighlight();
        return this;
    }

    @Override
    public VisualOcrScreen doubleClickVisualElementByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually double-clicking element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual double-click OCR text: " + ocrText);
        element.doubleClick();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual double-click OCR text: " + ocrText);
        return this;
    }

    @Override
    public VisualOcrScreen hoverVisualElementByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually hovering over element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual hover OCR text: " + ocrText);
        element.hover();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual hover OCR text: " + ocrText);
        return this;
    }

    @Override
    public VisualOcrScreen longPressVisualElementByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually long-pressing element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual long-press OCR text: " + ocrText);
        element.longPress();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual long-press OCR text: " + ocrText);
        return this;
    }

    @Override
    public VisualOcrScreen swipeOnVisualElementByText(String elementName, Direction direction, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually swiping %s on element '%s' using OCR text '%s'", direction, elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual swipe " + direction + " OCR text: " + ocrText);
        element.swipe(direction);
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual swipe " + direction + " OCR text: " + ocrText);
        return this;
    }

    @Override
    public VisualOcrScreen clickVisualElementAtIndexByText(String elementName, int index, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually clicking element '%s' at index %d using OCR text '%s'", elementName, index, ocrText));
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        assertThat(elements).as("Expected at least " + (index + 1) + " visual elements matching OCR text '" + ocrText + "'").hasSizeGreaterThan(index);
        VisualElement target = elements.get(index);
        target.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual click at index " + index + " OCR text: " + ocrText);
        target.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual click at index " + index + " OCR text: " + ocrText);
        return this;
    }

    @Override
    public VisualOcrScreen clickVisualElementByPositionByText(String elementName, String positionText, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Visually clicking '%s' element '%s' using OCR text '%s'", positionText, elementName, ocrText));
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        assertThat(elements).as("Expected at least 1 visual element matching OCR text '" + ocrText + "'").isNotEmpty();
        int targetIndex = parsePositionIndex(positionText, elements.size());
        VisualElement target = elements.get(targetIndex);
        target.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual click position '" + positionText + "' OCR text: " + ocrText);
        target.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual click position '" + positionText + "' OCR text: " + ocrText);
        return this;
    }

    @Override
    public VisualOcrScreen clickVisualElementByPositionByImage(String elementName, String positionText, List<String> imagePaths) {
        LOGGER.info(String.format("Playwright Java - Visually clicking '%s' element '%s' using image template '%s'", positionText, elementName, imagePaths));
        List<VisualElement> elements = findAllVisualElementsByImage(imagePaths);
        assertThat(elements).as("Expected at least 1 visual element matching image template '" + imagePaths + "'").isNotEmpty();
        int targetIndex = parsePositionIndex(positionText, elements.size());
        VisualElement target = elements.get(targetIndex);
        target.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual click position '" + positionText + "' image: " + imagePaths);
        target.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual click position '" + positionText + "' image: " + imagePaths);
        return this;
    }

    @Override
    public VisualOcrScreen clickVisualElementRelativeByText(String elementName, String targetText, SpatialDirection direction, String anchorText) {
        LOGGER.info(String.format("Playwright Java - Visually clicking element '%s' with OCR text '%s' %s anchor text '%s'", elementName, targetText, direction.getDirection(), anchorText));
        VisualElement element = findVisualElementRelativeByText(targetText, direction, anchorText);
        assertThat(element).as("Visual element '" + elementName + "' relative to '" + anchorText + "' should be found").isNotNull();
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before visual click relative: " + targetText);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After visual click relative: " + targetText);
        return this;
    }

    @Override
    public boolean tryClickVisualElementByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Attempting optional visual click on element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        if (element == null) return false;
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before try visual click OCR text: " + ocrText);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After try visual click OCR text: " + ocrText);
        return true;
    }

    @Override
    public boolean tryClickVisualElementByImage(String elementName, List<String> imagePaths) {
        LOGGER.info(String.format("Playwright Java - Attempting optional visual click on element '%s' using image template '%s'", elementName, imagePaths));
        VisualElement element = findVisualElementByImage(imagePaths);
        if (element == null) return false;
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before try visual click: " + elementName);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After try visual click: " + elementName);
        return true;
    }

    @Override
    public boolean tryClickVisualElementByTextOrImage(String elementName, String ocrText, List<String> imagePaths) {
        LOGGER.info(String.format("Playwright Java - Attempting optional visual click on element '%s' using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imagePaths));
        VisualElement element = findVisualElementByTextOrImage(ocrText, imagePaths);
        if (element == null) return false;
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before try visual click: " + elementName);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After try visual click: " + elementName);
        return true;
    }

    @Override
    public boolean tryClickVisualElementByImageOrText(String elementName, List<String> imagePaths, String ocrText) {
        LOGGER.info(String.format("Playwright Java - Attempting optional visual click on element '%s' using fallback image template '%s' or OCR text '%s'", elementName, imagePaths, ocrText));
        VisualElement element = findVisualElementByImageOrText(imagePaths, ocrText);
        if (element == null) return false;
        element.highlight();
        visually.checkWindow(SCREEN_NAME, "Before try visual click: " + elementName);
        element.click();
        driver.clearHighlight();
        waitForVisualActionIfConfigured();
        visually.checkWindow(SCREEN_NAME, "After try visual click: " + elementName);
        return true;
    }

    @Override
    public boolean tryClickVisualElementRelativeByText(String elementName, String targetText, SpatialDirection direction, String anchorText) {
        LOGGER.info(String.format("Playwright Java - Attempting optional visual click on element '%s' with OCR text '%s' %s anchor text '%s'", elementName, targetText, direction.getDirection(), anchorText));
        try {
            VisualElement element = findVisualElementRelativeByText(targetText, direction, anchorText);
            if (element == null) return false;
            element.highlight();
            visually.checkWindow(SCREEN_NAME, "Before try visual click relative: " + targetText);
            element.click();
            driver.clearHighlight();
            waitForVisualActionIfConfigured();
            visually.checkWindow(SCREEN_NAME, "After try visual click relative: " + targetText);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int parsePositionIndex(String positionText, int listSize) {
        if (positionText == null || positionText.isBlank()) return 0;
        String normalized = positionText.trim().toLowerCase();
        if ("first".equals(normalized) || "1st".equals(normalized)) return 0;
        if ("last".equals(normalized)) return Math.max(0, listSize - 1);
        if ("second".equals(normalized) || "2nd".equals(normalized)) return 1;
        if ("third".equals(normalized) || "3rd".equals(normalized)) return 2;
        if (normalized.endsWith("st") || normalized.endsWith("nd") || normalized.endsWith("rd") || normalized.endsWith("th")) {
            String digits = normalized.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) return Integer.parseInt(digits) - 1;
        }
        try { return Integer.parseInt(normalized); } catch (NumberFormatException e) { return 0; }
    }
}
