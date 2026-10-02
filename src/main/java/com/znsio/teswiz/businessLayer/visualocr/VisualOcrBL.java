package com.znsio.teswiz.businessLayer.visualocr;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.Platform;
import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Drivers;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.screen.visualocr.VisualOcrScreen;
import static com.znsio.teswiz.tools.Wait.waitFor;

public class VisualOcrBL {
    private static final Logger LOGGER = LogManager.getLogger(VisualOcrBL.class.getName());
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
        this.currentUserPersona = "DEFAULT";
        this.currentPlatform = Platform.web;
    }

    // --- Query / Finder Methods (Returns VisualElement) ---

    public VisualElement findVisualElementByText(String ocrText) {
        return VisualOcrScreen.get().findVisualElementByText(ocrText);
    }

    public VisualElement findVisualElementByImage(String imageTemplatePath) {
        return VisualOcrScreen.get().findVisualElementByImage(List.of(imageTemplatePath));
    }

    public VisualElement findVisualElementByTextOrImage(String ocrText, String imageTemplatePath) {
        return VisualOcrScreen.get().findVisualElementByTextOrImage(ocrText, List.of(imageTemplatePath));
    }

    public VisualElement findVisualElementByImageOrText(String imageTemplatePath, String ocrText) {
        return VisualOcrScreen.get().findVisualElementByImageOrText(List.of(imageTemplatePath), ocrText);
    }

    // --- Pure Verification Methods (Explicit Assertions, Returns VisualOcrBL for
    // chaining) ---

    public VisualOcrBL verifyVisualElementIsPresentByText(String elementName, String ocrText) {
        LOGGER.info(
                String.format("Verifying visual element '%s' is present using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found")
                .isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsNotPresentByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Verifying visual element '%s' is NOT present using OCR text '%s'", elementName,
                ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element)
                .as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should NOT be present")
                .isNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByImage(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using image template '%s'", elementName,
                imageTemplatePath));
        VisualElement element = findVisualElementByImage(imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath
                + "' should be found").isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsNotPresentByImage(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Verifying visual element '%s' is NOT present using image template '%s'", elementName,
                imageTemplatePath));
        VisualElement element = findVisualElementByImage(imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath
                + "' should NOT be present").isNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByFallbackTextOrImage(String elementName, String ocrText,
            String imageTemplatePath) {
        LOGGER.info(String.format(
                "Verifying visual element '%s' is present using fallback OCR text '%s' or image template '%s'",
                elementName, ocrText, imageTemplatePath));
        VisualElement element = findVisualElementByTextOrImage(ocrText, imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by text/image should be found")
                .isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentByFallbackImageOrText(String elementName, String imageTemplatePath,
            String ocrText) {
        LOGGER.info(String.format(
                "Verifying visual element '%s' is present using fallback image template '%s' or OCR text '%s'",
                elementName, imageTemplatePath, ocrText));
        VisualElement element = findVisualElementByImageOrText(imageTemplatePath, ocrText);
        assertThat(element).as("Visual element '" + elementName + "' matched by image/text should be found")
                .isNotNull();
        return this;
    }

    // --- Conditional Action Methods (Non-asserting, Returns boolean) ---

    public boolean tryVisuallyClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Attempting optional visual click on element '%s' using OCR text '%s'", elementName,
                ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        if (element == null) {
            LOGGER.info(String.format("Optional visual element '%s' with OCR text '%s' was not found. Skipping click.",
                    elementName, ocrText));
            return false;
        }
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before try visual click OCR text: " + ocrText);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After try visual click OCR text: " + ocrText);
        return true;
    }

    public boolean tryVisuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Attempting optional visual click on element '%s' using image template '%s'",
                elementName, imageTemplatePath));
        VisualElement element = findVisualElementByImage(imageTemplatePath);
        if (element == null) {
            LOGGER.info(String.format(
                    "Optional visual element '%s' matched by image template '%s' was not found. Skipping click.",
                    elementName, imageTemplatePath));
            return false;
        }
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before try visual click: " + elementName);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After try visual click: " + elementName);
        return true;
    }

    public boolean tryVisuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText,
            String imageTemplatePath) {
        LOGGER.info(String.format(
                "Attempting optional visual click on element '%s' using fallback OCR text '%s' or image template '%s'",
                elementName, ocrText, imageTemplatePath));
        VisualElement element = findVisualElementByTextOrImage(ocrText, imageTemplatePath);
        if (element == null) {
            LOGGER.info(String.format(
                    "Optional visual element '%s' matched by text/image was not found. Skipping click.", elementName));
            return false;
        }
        String matchedOption = resolveMatchedOption(element, ocrText, imageTemplatePath, true);
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName,
                matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Before try visual click: " + elementName + " via " + matchedOption);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "After try visual click: " + elementName + " via " + matchedOption);
        return true;
    }

    public boolean tryVisuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath,
            String ocrText) {
        LOGGER.info(String.format(
                "Attempting optional visual click on element '%s' using fallback image template '%s' or OCR text '%s'",
                elementName, imageTemplatePath, ocrText));
        VisualElement element = findVisualElementByImageOrText(imageTemplatePath, ocrText);
        if (element == null) {
            LOGGER.info(String.format(
                    "Optional visual element '%s' matched by image/text was not found. Skipping click.", elementName));
            return false;
        }
        String matchedOption = resolveMatchedOption(element, ocrText, imageTemplatePath, false);
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName,
                matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Before try visual click: " + elementName + " via " + matchedOption);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "After try visual click: " + elementName + " via " + matchedOption);
        return true;
    }

    // --- Strict Action Methods (Asserting Mode, Returns VisualOcrBL for chaining)
    // ---

    public VisualOcrBL visuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking element '%s' using image template '%s'", elementName,
                imageTemplatePath));
        VisualElement element = findVisualElementByImage(imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath
                + "' should be found").isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click: " + elementName);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click: " + elementName);
        return this;
    }

    public VisualOcrBL visuallyClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually clicking element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found")
                .isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click OCR text: " + ocrText);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click OCR text: " + ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually inspecting element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found")
                .isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Inspecting OCR text: " + ocrText);
        getDriver().clearHighlight();
        return this;
    }

    public VisualOcrBL visuallyInspectUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually inspecting element '%s' using image template '%s'", elementName,
                imageTemplatePath));
        VisualElement element = findVisualElementByImage(imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath
                + "' should be found").isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Inspecting image template: " + imageTemplatePath);
        getDriver().clearHighlight();
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText,
            String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking element '%s' using fallback OCR text '%s' or image template '%s'",
                elementName, ocrText, imageTemplatePath));
        VisualElement element = findVisualElementByTextOrImage(ocrText, imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by text/image should be found")
                .isNotNull();
        String matchedOption = resolveMatchedOption(element, ocrText, imageTemplatePath, true);
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName,
                matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Before visual click: " + elementName + " via " + matchedOption);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "After visual click: " + elementName + " via " + matchedOption);
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath,
            String ocrText) {
        LOGGER.info(String.format("Visually clicking element '%s' using fallback image template '%s' or OCR text '%s'",
                elementName, imageTemplatePath, ocrText));
        VisualElement element = findVisualElementByImageOrText(imageTemplatePath, ocrText);
        assertThat(element).as("Visual element '" + elementName + "' matched by image/text should be found")
                .isNotNull();
        String matchedOption = resolveMatchedOption(element, ocrText, imageTemplatePath, false);
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName,
                matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Before visual click: " + elementName + " via " + matchedOption);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "After visual click: " + elementName + " via " + matchedOption);
        return this;
    }

    // --- Subsystem Disabled Verification Methods ---

    public VisualOcrBL verifyVisualOcrCapabilityDisabled() {
        assertThat(Runner.isOcrEnabled()).isFalse();
        LOGGER.info("Verified IS_OCR_ENABLED is set to false");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForText(String text) {
        LOGGER.info("Verifying findByText throws VisualSubsystemDisabledException for text: " + text);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByText(text))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForImage(String imagePath) {
        LOGGER.info("Verifying findByImage throws VisualSubsystemDisabledException for image: " + imagePath);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByImage(List.of(imagePath)))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForTextOrImage(String text, String imagePath) {
        LOGGER.info("Verifying findByTextOrImage throws VisualSubsystemDisabledException for text: " + text
                + ", image: " + imagePath);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByTextOrImage(text, List.of(imagePath)))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForImageOrText(String imagePath, String text) {
        LOGGER.info("Verifying findByImageOrText throws VisualSubsystemDisabledException for image: " + imagePath
                + ", text: " + text);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByImageOrText(List.of(imagePath), text))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public List<VisualElement> findAllVisualElementsByText(String ocrText) {
        return VisualOcrScreen.get().findAllVisualElementsByText(ocrText);
    }

    public List<VisualElement> findAllVisualElementsByImage(String imageTemplatePath) {
        return VisualOcrScreen.get().findAllVisualElementsByImage(List.of(imageTemplatePath));
    }

    public List<VisualElement> findAllVisualElementsByTextOrImage(String ocrText, String imageTemplatePath) {
        return VisualOcrScreen.get().findAllVisualElementsByTextOrImage(ocrText, List.of(imageTemplatePath));
    }

    public List<VisualElement> findAllVisualElementsByImageOrText(String imageTemplatePath, String ocrText) {
        return VisualOcrScreen.get().findAllVisualElementsByImageOrText(List.of(imageTemplatePath), ocrText);
    }

    public VisualOcrBL verifyVisualElementCountByText(String elementName, String ocrText, int expectedCount) {
        LOGGER.info(String.format("Verifying %d visual elements named '%s' present using OCR text '%s'", expectedCount,
                elementName, ocrText));
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        assertThat(elements)
                .as("Expected " + expectedCount + " visual elements matching OCR text '" + ocrText + "'")
                .hasSize(expectedCount);
        return this;
    }

    public VisualOcrBL verifyVisualElementCountByImage(String elementName, String imageTemplatePath,
            int expectedCount) {
        LOGGER.info(String.format("Verifying %d visual elements named '%s' present using image template '%s'",
                expectedCount, elementName, imageTemplatePath));
        List<VisualElement> elements = findAllVisualElementsByImage(imageTemplatePath);
        assertThat(elements)
                .as("Expected " + expectedCount + " visual elements matching image template '" + imageTemplatePath
                        + "'")
                .hasSize(expectedCount);
        return this;
    }

    public VisualOcrBL verifyAtLeastNVisualElementsPresentByText(String elementName, String ocrText, int minCount) {
        LOGGER.info(String.format("Verifying at least %d visual elements named '%s' present using OCR text '%s'",
                minCount, elementName, ocrText));
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        LOGGER.info(String.format("Found %d visual elements named '%s' using OCR text '%s'", elements.size(),
                elementName, ocrText));
        assertThat(elements)
                .as("Expected at least " + minCount + " visual elements matching OCR text '" + ocrText + "'")
                .hasSizeGreaterThanOrEqualTo(minCount);
        return this;
    }

    public VisualOcrBL verifyAtLeastNVisualElementsPresentByImage(String elementName, String imageTemplatePath,
            int minCount) {
        LOGGER.info(String.format("Verifying at least %d visual elements named '%s' present using image template '%s'",
                minCount, elementName, imageTemplatePath));
        List<VisualElement> elements = findAllVisualElementsByImage(imageTemplatePath);
        LOGGER.info(String.format("Found %d visual elements named '%s' using image template '%s'", elements.size(),
                elementName, imageTemplatePath));
        assertThat(elements)
                .as("Expected at least " + minCount + " visual elements matching image template '" + imageTemplatePath
                        + "'")
                .hasSizeGreaterThanOrEqualTo(minCount);
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually finding all instances of '%s' using image template '%s'", elementName,
                imageTemplatePath));
        List<VisualElement> elements = findAllVisualElementsByImage(imageTemplatePath);
        assertThat(elements)
                .as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath
                        + "' should have at least 1 instance on screen")
                .isNotEmpty();
        LOGGER.info(String.format("Found %d instances of visual element '%s' using image template '%s'", elements.size(),
                elementName, imageTemplatePath));

        for (VisualElement element : elements) {
            element.highlight();
        }
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Found " + elements.size() + " instances of " + elementName + " via image template");
        getDriver().clearHighlight();
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually finding all instances of '%s' using OCR text '%s'", elementName, ocrText));
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        LOGGER.info(String.format("Found %d instances of visual element '%s' using OCR text '%s'", elements.size(),
                elementName, ocrText));
        assertThat(elements)
                .as("Visual element '" + elementName + "' matched by OCR text '" + ocrText
                        + "' should have at least 1 instance on screen")
                .isNotEmpty();

        for (VisualElement element : elements) {
            element.highlight();
        }
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Found " + elements.size() + " instances of " + elementName + " via OCR text");
        getDriver().clearHighlight();
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText,
            String imageTemplatePath) {
        LOGGER.info(String.format(
                "Visually finding all instances of '%s' using fallback OCR text '%s' or image template '%s'",
                elementName, ocrText, imageTemplatePath));
        List<VisualElement> elements = findAllVisualElementsByTextOrImage(ocrText, imageTemplatePath);
        assertThat(elements)
                .as("Visual element '" + elementName
                        + "' matched by fallback text/image should have at least 1 instance on screen")
                .isNotEmpty();

        for (VisualElement element : elements) {
            element.highlight();
        }
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Found " + elements.size() + " instances of " + elementName + " via fallback text/image");
        getDriver().clearHighlight();
        return this;
    }

    public VisualOcrBL visuallyFindAllInstancesUsingFallbackImageTemplateOrOcrText(String elementName,
            String imageTemplatePath, String ocrText) {
        LOGGER.info(String.format(
                "Visually finding all instances of '%s' using fallback image template '%s' or OCR text '%s'",
                elementName, imageTemplatePath, ocrText));
        List<VisualElement> elements = findAllVisualElementsByImageOrText(imageTemplatePath, ocrText);
        assertThat(elements)
                .as("Visual element '" + elementName
                        + "' matched by fallback image/text should have at least 1 instance on screen")
                .isNotEmpty();

        for (VisualElement element : elements) {
            element.highlight();
        }
        LOGGER.info(String.format("Found %d instances of visual element '%s' using fallback image template '%s' or OCR text '%s'",
                elements.size(), elementName, imageTemplatePath, ocrText));
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Found " + elements.size() + " instances of " + elementName + " via fallback image/text");
        getDriver().clearHighlight();
        return this;
    }

    public VisualOcrBL visuallyClickElementAtIndexUsingOcrText(String elementName, int index, String ocrText) {
        LOGGER.info(String.format("Visually clicking element '%s' at index %d using OCR text '%s'", elementName, index,
                ocrText));
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        assertThat(elements)
                .as("Expected at least " + (index + 1) + " visual elements matching OCR text '" + ocrText + "'")
                .hasSizeGreaterThan(index);

        VisualElement target = elements.get(index);
        target.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Before visual click at index " + index + " OCR text: " + ocrText);
        target.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "After visual click at index " + index + " OCR text: " + ocrText);
        return this;
    }

    public VisualOcrBL visuallyClickElementByPositionUsingOcrText(String elementName, String positionText,
            String ocrText) {
        LOGGER.info(String.format("Visually clicking '%s' element '%s' using OCR text '%s'", positionText, elementName,
                ocrText));
        List<VisualElement> elements = findAllVisualElementsByText(ocrText);
        assertThat(elements)
                .as("Expected at least 1 visual element matching OCR text '" + ocrText + "'")
                .isNotEmpty();

        int targetIndex = parsePositionIndex(positionText, elements.size());
        assertThat(targetIndex)
                .as("Invalid position index '" + positionText + "' for list of size " + elements.size())
                .isGreaterThanOrEqualTo(0)
                .isLessThan(elements.size());

        VisualElement target = elements.get(targetIndex);
        target.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Before visual click position '" + positionText + "' OCR text: " + ocrText);
        target.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "After visual click position '" + positionText + "' OCR text: " + ocrText);
        return this;
    }

    public VisualOcrBL visuallyClickElementByPositionUsingImageTemplate(String elementName, String positionText,
            String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking '%s' element '%s' using image template '%s'", positionText,
                elementName, imageTemplatePath));
        List<VisualElement> elements = findAllVisualElementsByImage(imageTemplatePath);
        assertThat(elements)
                .as("Expected at least 1 visual element matching image template '" + imageTemplatePath + "'")
                .isNotEmpty();

        int targetIndex = parsePositionIndex(positionText, elements.size());
        assertThat(targetIndex)
                .as("Invalid position index '" + positionText + "' for list of size " + elements.size())
                .isGreaterThanOrEqualTo(0)
                .isLessThan(elements.size());

        VisualElement target = elements.get(targetIndex);
        target.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "Before visual click position '" + positionText + "' image: " + imageTemplatePath);
        target.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                "After visual click position '" + positionText + "' image: " + imageTemplatePath);
        return this;
    }

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
        if (normalized.endsWith("st") || normalized.endsWith("nd") || normalized.endsWith("rd")
                || normalized.endsWith("th")) {
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

    public VisualElement findVisualElementRelativeByText(String targetText, String directionText, String anchorText) {
        com.znsio.teswiz.entities.SpatialDirection direction = com.znsio.teswiz.entities.SpatialDirection
                .fromString(directionText);
        return VisualOcrScreen.get().findVisualElementRelativeByText(targetText, direction, anchorText);
    }

    public VisualOcrBL visuallyClickUsingOcrTextRelative(String elementName, String targetText, String directionText,
            String anchorText) {
        LOGGER.info(String.format("Visually clicking element '%s' with OCR text '%s' %s anchor text '%s'", elementName,
                targetText, directionText, anchorText));
        VisualElement element = findVisualElementRelativeByText(targetText, directionText, anchorText);
        assertThat(element).as("Visual element '" + elementName + "' relative to '" + anchorText + "' should be found")
                .isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click relative: " + targetText);
        element.click();
        getDriver().clearHighlight();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click relative: " + targetText);
        return this;
    }

    public boolean tryVisuallyClickUsingOcrTextRelative(String elementName, String targetText, String directionText,
            String anchorText) {
        LOGGER.info(
                String.format("Attempting optional visual click on element '%s' with OCR text '%s' %s anchor text '%s'",
                        elementName, targetText, directionText, anchorText));
        try {
            VisualElement element = findVisualElementRelativeByText(targetText, directionText, anchorText);
            if (element == null) {
                return false;
            }
            element.highlight();
            getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                    "Before try visual click relative: " + targetText);
            element.click();
            getDriver().clearHighlight();
            waitFor(5);
            getDriver().getVisual().checkWindow(getClass().getSimpleName(),
                    "After try visual click relative: " + targetText);
            return true;
        } catch (Exception e) {
            LOGGER.info(String.format("Optional relative element '%s' was not found. Skipping click.", targetText));
            return false;
        }
    }

    public VisualOcrBL verifyVisualElementIsPresentRelativeByText(String elementName, String targetText,
            String directionText, String anchorText) {
        LOGGER.info(String.format("Verifying visual element '%s' with OCR text '%s' present %s anchor text '%s'",
                elementName, targetText, directionText, anchorText));
        VisualElement element = findVisualElementRelativeByText(targetText, directionText, anchorText);
        assertThat(element).as("Visual element '" + elementName + "' relative to '" + anchorText + "' should be found")
                .isNotNull();
        return this;
    }

    public VisualOcrBL verifyVisualElementIsPresentInRegion(String elementName, String ocrText, int x, int y, int width,
            int height) {
        com.znsio.teswiz.entities.VisualRegion region = com.znsio.teswiz.entities.VisualRegion.inRegion(x, y, width,
                height);
        LOGGER.info(String.format("Verifying visual element '%s' with OCR text '%s' is present in region %s",
                elementName, ocrText, region));
        VisualElement element = VisualOcrScreen.get().findVisualElementByTextInRegion(ocrText, region);
        assertThat(element).as("Visual element '" + elementName + "' in region " + region + " should be present")
                .isNotNull();
        return this;
    }

    private String resolveMatchedOption(VisualElement element, String ocrText, String imageTemplatePath,
            boolean ocrIsPrimary) {
        boolean matchedByOcr = element.getLabel().startsWith("OCR:");
        if (ocrIsPrimary) {
            return matchedByOcr ? "fallback OCR text '" + ocrText + "'" : "image template '" + imageTemplatePath + "'";
        } else {
            return matchedByOcr ? "OCR text '" + ocrText + "'" : "fallback image template '" + imageTemplatePath + "'";
        }
    }

    private Driver getDriver() {
        return Drivers.getDriverForCurrentUser(Thread.currentThread().getId());
    }
}
