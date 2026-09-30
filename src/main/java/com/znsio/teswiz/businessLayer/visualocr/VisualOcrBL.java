package com.znsio.teswiz.businessLayer.visualocr;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    // --- Query / Finder Methods (Non-asserting) ---

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

    // --- Pure Verification Methods (Explicit Assertions) ---

    public VisualElement verifyVisualElementIsPresentByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        return element;
    }

    public VisualOcrBL verifyVisualElementIsNotPresentByText(String elementName, String ocrText) {
        LOGGER.info(String.format("Verifying visual element '%s' is NOT present using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should NOT be present").isNull();
        return this;
    }

    public VisualElement verifyVisualElementIsPresentByImage(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = findVisualElementByImage(imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should be found").isNotNull();
        return element;
    }

    public VisualOcrBL verifyVisualElementIsNotPresentByImage(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Verifying visual element '%s' is NOT present using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = findVisualElementByImage(imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should NOT be present").isNull();
        return this;
    }

    public VisualElement verifyVisualElementIsPresentByFallbackTextOrImage(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imageTemplatePath));
        VisualElement element = findVisualElementByTextOrImage(ocrText, imageTemplatePath);
        assertThat(element).as("Visual element '" + elementName + "' matched by text/image should be found").isNotNull();
        return element;
    }

    public VisualElement verifyVisualElementIsPresentByFallbackImageOrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info(String.format("Verifying visual element '%s' is present using fallback image template '%s' or OCR text '%s'", elementName, imageTemplatePath, ocrText));
        VisualElement element = findVisualElementByImageOrText(imageTemplatePath, ocrText);
        assertThat(element).as("Visual element '" + elementName + "' matched by image/text should be found").isNotNull();
        return element;
    }

    // --- Conditional Action Methods (Non-asserting) ---

    public boolean tryVisuallyClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Attempting optional visual click on element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = findVisualElementByText(ocrText);
        if (element == null) {
            LOGGER.info(String.format("Optional visual element '%s' with OCR text '%s' was not found. Skipping click.", elementName, ocrText));
            return false;
        }
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before try visual click OCR text: " + ocrText);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After try visual click OCR text: " + ocrText);
        return true;
    }

    public boolean tryVisuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Attempting optional visual click on element '%s' using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = findVisualElementByImage(imageTemplatePath);
        if (element == null) {
            LOGGER.info(String.format("Optional visual element '%s' matched by image template '%s' was not found. Skipping click.", elementName, imageTemplatePath));
            return false;
        }
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before try visual click: " + elementName);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After try visual click: " + elementName);
        return true;
    }

    public boolean tryVisuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info(String.format("Attempting optional visual click on element '%s' using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imageTemplatePath));
        VisualElement element = findVisualElementByTextOrImage(ocrText, imageTemplatePath);
        if (element == null) {
            LOGGER.info(String.format("Optional visual element '%s' matched by text/image was not found. Skipping click.", elementName));
            return false;
        }
        String matchedOption = resolveMatchedOption(element, ocrText, imageTemplatePath, true);
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName, matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before try visual click: " + elementName + " via " + matchedOption);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After try visual click: " + elementName + " via " + matchedOption);
        return true;
    }

    public boolean tryVisuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info(String.format("Attempting optional visual click on element '%s' using fallback image template '%s' or OCR text '%s'", elementName, imageTemplatePath, ocrText));
        VisualElement element = findVisualElementByImageOrText(imageTemplatePath, ocrText);
        if (element == null) {
            LOGGER.info(String.format("Optional visual element '%s' matched by image/text was not found. Skipping click.", elementName));
            return false;
        }
        String matchedOption = resolveMatchedOption(element, ocrText, imageTemplatePath, false);
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName, matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before try visual click: " + elementName + " via " + matchedOption);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After try visual click: " + elementName + " via " + matchedOption);
        return true;
    }

    // --- Strict Action Methods (Asserting Mode) ---

    public VisualOcrBL visuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking element '%s' using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = verifyVisualElementIsPresentByImage(elementName, imageTemplatePath);
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click: " + elementName);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click: " + elementName);
        return this;
    }

    public VisualOcrBL visuallyClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually clicking element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = verifyVisualElementIsPresentByText(elementName, ocrText);
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click OCR text: " + ocrText);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click OCR text: " + ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually inspecting element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = verifyVisualElementIsPresentByText(elementName, ocrText);
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Inspecting OCR text: " + ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually inspecting element '%s' using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = verifyVisualElementIsPresentByImage(elementName, imageTemplatePath);
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Inspecting image template: " + imageTemplatePath);
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking element '%s' using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imageTemplatePath));
        VisualElement element = verifyVisualElementIsPresentByFallbackTextOrImage(elementName, ocrText, imageTemplatePath);
        String matchedOption = resolveMatchedOption(element, ocrText, imageTemplatePath, true);
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName, matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click: " + elementName + " via " + matchedOption);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click: " + elementName + " via " + matchedOption);
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info(String.format("Visually clicking element '%s' using fallback image template '%s' or OCR text '%s'", elementName, imageTemplatePath, ocrText));
        VisualElement element = verifyVisualElementIsPresentByFallbackImageOrText(elementName, imageTemplatePath, ocrText);
        String matchedOption = resolveMatchedOption(element, ocrText, imageTemplatePath, false);
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName, matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click: " + elementName + " via " + matchedOption);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click: " + elementName + " via " + matchedOption);
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
        LOGGER.info("Verifying findByTextOrImage throws VisualSubsystemDisabledException for text: " + text + ", image: " + imagePath);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByTextOrImage(text, List.of(imagePath)))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    public VisualOcrBL verifyOcrDisabledThrowsExceptionForImageOrText(String imagePath, String text) {
        LOGGER.info("Verifying findByImageOrText throws VisualSubsystemDisabledException for image: " + imagePath + ", text: " + text);
        assertThat(Runner.isOcrEnabled()).isFalse();
        assertThatThrownBy(() -> VisualOcrScreen.get().findVisualElementByImageOrText(List.of(imagePath), text))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
        return this;
    }

    private String resolveMatchedOption(VisualElement element, String ocrText, String imageTemplatePath, boolean ocrIsPrimary) {
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
