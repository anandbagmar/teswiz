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

    public VisualOcrBL visuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking element '%s' using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = VisualOcrScreen.get().findVisualElementByImage(List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should be found").isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click: " + elementName);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click: " + elementName);
        return this;
    }

    public VisualOcrBL visuallyClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually clicking element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = VisualOcrScreen.get().findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click OCR text: " + ocrText);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click OCR text: " + ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually inspecting element '%s' using OCR text '%s'", elementName, ocrText));
        VisualElement element = VisualOcrScreen.get().findVisualElementByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Inspecting OCR text: " + ocrText);
        return this;
    }

    public VisualOcrBL visuallyInspectUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually inspecting element '%s' using image template '%s'", elementName, imageTemplatePath));
        VisualElement element = VisualOcrScreen.get().findVisualElementByImage(List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should be found").isNotNull();
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Inspecting image template: " + imageTemplatePath);
        return this;
    }

    public VisualOcrBL visuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking element '%s' using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imageTemplatePath));
        VisualElement element = VisualOcrScreen.get().findVisualElementByTextOrImage(ocrText, List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by text/image should be found").isNotNull();
        String matchedOption = element.getLabel().startsWith("OCR:")
                ? "fallback OCR text '" + ocrText + "'"
                : "image template '" + imageTemplatePath + "'";
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
        VisualElement element = VisualOcrScreen.get().findVisualElementByImageOrText(List.of(imageTemplatePath), ocrText);
        assertThat(element).as("Visual element '" + elementName + "' matched by image/text should be found").isNotNull();
        String matchedOption = element.getLabel().startsWith("OCR:")
                ? "OCR text '" + ocrText + "'"
                : "fallback image template '" + imageTemplatePath + "'";
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName, matchedOption, element.getLabel()));
        element.highlight();
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "Before visual click: " + elementName + " via " + matchedOption);
        element.click();
        waitFor(5);
        getDriver().getVisual().checkWindow(getClass().getSimpleName(), "After visual click: " + elementName + " via " + matchedOption);
        return this;
    }

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

    private Driver getDriver() {
        return Drivers.getDriverForCurrentUser(Thread.currentThread().getId());
    }
}
