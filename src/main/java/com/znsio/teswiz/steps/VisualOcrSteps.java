package com.znsio.teswiz.steps;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Drivers;
import com.znsio.teswiz.runner.VisualElement;

import static com.znsio.teswiz.tools.Wait.waitFor;
import io.cucumber.java.en.When;

public class VisualOcrSteps {

    private static final Logger LOGGER = LogManager.getLogger(VisualOcrSteps.class.getName());

    @When("I visually click {string} using image template {string}")
    public void iVisuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking element '%s' using image template '%s'", elementName, imageTemplatePath));
        Driver driver = Drivers.getDriverForCurrentUser(Thread.currentThread().getId());
        VisualElement element = driver.findByImage(List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should be found").isNotNull();
        element.highlight();
        driver.getVisual().checkWindow("VisualOcrSteps", "Before visual click: " + elementName);
        element.click();
        waitFor(5);
        driver.getVisual().checkWindow("VisualOcrSteps", "After visual click: " + elementName);
    }

    @When("I visually click {string} using OCR text {string}")
    public void iVisuallyClickUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually clicking element '%s' using OCR text '%s'", elementName, ocrText));
        Driver driver = Drivers.getDriverForCurrentUser(Thread.currentThread().getId());
        VisualElement element = driver.findByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        driver.getVisual().checkWindow("VisualOcrSteps", "Before visual click OCR text: " + ocrText);
        element.click();
        waitFor(5);
        driver.getVisual().checkWindow("VisualOcrSteps", "After visual click OCR text: " + ocrText);
    }

    @When("I visually inspect {string} using OCR text {string}")
    public void iVisuallyInspectUsingOcrText(String elementName, String ocrText) {
        LOGGER.info(String.format("Visually inspecting element '%s' using OCR text '%s'", elementName, ocrText));
        Driver driver = Drivers.getDriverForCurrentUser(Thread.currentThread().getId());
        VisualElement element = driver.findByText(ocrText);
        assertThat(element).as("Visual element '" + elementName + "' with OCR text '" + ocrText + "' should be found").isNotNull();
        element.highlight();
        driver.getVisual().checkWindow("VisualOcrSteps", "Inspecting OCR text: " + ocrText);
    }

    @When("I visually inspect {string} using image template {string}")
    public void iVisuallyInspectUsingImageTemplate(String elementName, String imageTemplatePath) {
        LOGGER.info(String.format("Visually inspecting element '%s' using image template '%s'", elementName, imageTemplatePath));
        Driver driver = Drivers.getDriverForCurrentUser(Thread.currentThread().getId());
        VisualElement element = driver.findByImage(List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by image template '" + imageTemplatePath + "' should be found").isNotNull();
        element.highlight();
        driver.getVisual().checkWindow("VisualOcrSteps", "Inspecting image template: " + imageTemplatePath);
    }

    @When("I visually click {string} using fallback OCR text {string} or image template {string}")
    public void iVisuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        LOGGER.info(String.format("Visually clicking element '%s' using fallback OCR text '%s' or image template '%s'", elementName, ocrText, imageTemplatePath));
        Driver driver = Drivers.getDriverForCurrentUser(Thread.currentThread().getId());
        VisualElement element = driver.findByTextOrImage(ocrText, List.of(imageTemplatePath));
        assertThat(element).as("Visual element '" + elementName + "' matched by text/image should be found").isNotNull();
        String matchedOption = element.getLabel().startsWith("OCR:")
                ? "fallback OCR text '" + ocrText + "'"
                : "image template '" + imageTemplatePath + "'";
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName, matchedOption, element.getLabel()));
        element.highlight();
        driver.getVisual().checkWindow("VisualOcrSteps", "Before visual click: " + elementName + " via " + matchedOption);
        element.click();
        waitFor(5);
        driver.getVisual().checkWindow("VisualOcrSteps", "After visual click: " + elementName + " via " + matchedOption);
    }

    @When("I visually click {string} using fallback image template {string} or OCR text {string}")
    public void iVisuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        LOGGER.info(String.format("Visually clicking element '%s' using fallback image template '%s' or OCR text '%s'", elementName, imageTemplatePath, ocrText));
        Driver driver = Drivers.getDriverForCurrentUser(Thread.currentThread().getId());
        VisualElement element = driver.findByImageOrText(List.of(imageTemplatePath), ocrText);
        assertThat(element).as("Visual element '" + elementName + "' matched by image/text should be found").isNotNull();
        String matchedOption = element.getLabel().startsWith("OCR:")
                ? "OCR text '" + ocrText + "'"
                : "fallback image template '" + imageTemplatePath + "'";
        LOGGER.info(String.format("Visual element '%s' resolved using %s (matched label: '%s')", elementName, matchedOption, element.getLabel()));
        element.highlight();
        driver.getVisual().checkWindow("VisualOcrSteps", "Before visual click: " + elementName + " via " + matchedOption);
        element.click();
        waitFor(5);
        driver.getVisual().checkWindow("VisualOcrSteps", "After visual click: " + elementName + " via " + matchedOption);
    }
}
