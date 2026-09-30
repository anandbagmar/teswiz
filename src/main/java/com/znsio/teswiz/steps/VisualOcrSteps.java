package com.znsio.teswiz.steps;

import com.znsio.teswiz.businessLayer.visualocr.VisualOcrBL;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class VisualOcrSteps {

    @When("I visually click {string} using image template {string}")
    public void iVisuallyClickUsingImageTemplate(String elementName, String imageTemplatePath) {
        new VisualOcrBL().visuallyClickUsingImageTemplate(elementName, imageTemplatePath);
    }

    @When("I visually click {string} using OCR text {string}")
    public void iVisuallyClickUsingOcrText(String elementName, String ocrText) {
        new VisualOcrBL().visuallyClickUsingOcrText(elementName, ocrText);
    }

    @When("I visually inspect {string} using OCR text {string}")
    public void iVisuallyInspectUsingOcrText(String elementName, String ocrText) {
        new VisualOcrBL().visuallyInspectUsingOcrText(elementName, ocrText);
    }

    @When("I visually inspect {string} using image template {string}")
    public void iVisuallyInspectUsingImageTemplate(String elementName, String imageTemplatePath) {
        new VisualOcrBL().visuallyInspectUsingImageTemplate(elementName, imageTemplatePath);
    }

    @When("I visually click {string} using fallback OCR text {string} or image template {string}")
    public void iVisuallyClickUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        new VisualOcrBL().visuallyClickUsingFallbackOcrTextOrImageTemplate(elementName, ocrText, imageTemplatePath);
    }

    @When("I visually click {string} using fallback image template {string} or OCR text {string}")
    public void iVisuallyClickUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        new VisualOcrBL().visuallyClickUsingFallbackImageTemplateOrOcrText(elementName, imageTemplatePath, ocrText);
    }

    @Given("Visual OCR capability is explicitly disabled")
    public void visualOcrCapabilityIsExplicitlyDisabled() {
        new VisualOcrBL().verifyVisualOcrCapabilityDisabled();
    }

    @Then("attempting to find visual element by text {string} should throw VisualSubsystemDisabledException")
    public void attemptingToFindVisualElementByTextShouldThrowVisualSubsystemDisabledException(String text) {
        new VisualOcrBL().verifyOcrDisabledThrowsExceptionForText(text);
    }

    @Then("attempting to find visual element by image {string} should throw VisualSubsystemDisabledException")
    public void attemptingToFindVisualElementByImageShouldThrowVisualSubsystemDisabledException(String imagePath) {
        new VisualOcrBL().verifyOcrDisabledThrowsExceptionForImage(imagePath);
    }

    @Then("attempting to find visual element by text {string} or image {string} should throw VisualSubsystemDisabledException")
    public void attemptingToFindVisualElementByTextOrImageShouldThrowVisualSubsystemDisabledException(String text, String imagePath) {
        new VisualOcrBL().verifyOcrDisabledThrowsExceptionForTextOrImage(text, imagePath);
    }

    @Then("attempting to find visual element by image {string} or text {string} should throw VisualSubsystemDisabledException")
    public void attemptingToFindVisualElementByImageOrTextShouldThrowVisualSubsystemDisabledException(String imagePath, String text) {
        new VisualOcrBL().verifyOcrDisabledThrowsExceptionForImageOrText(imagePath, text);
    }
}
