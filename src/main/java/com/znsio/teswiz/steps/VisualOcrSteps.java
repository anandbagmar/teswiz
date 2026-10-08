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

    @When("I visually enter {string} into {string} using OCR text {string}")
    public void iVisuallyEnterTextIntoUsingOcrText(String textToEnter, String elementName, String ocrText) {
        new VisualOcrBL().visuallyEnterTextUsingOcrText(elementName, textToEnter, ocrText);
    }

    @When("I visually double-click {string} using OCR text {string}")
    public void iVisuallyDoubleClickUsingOcrText(String elementName, String ocrText) {
        new VisualOcrBL().visuallyDoubleClickUsingOcrText(elementName, ocrText);
    }

    @When("I visually hover over {string} using OCR text {string}")
    public void iVisuallyHoverUsingOcrText(String elementName, String ocrText) {
        new VisualOcrBL().visuallyHoverUsingOcrText(elementName, ocrText);
    }

    @When("I visually long-press {string} using OCR text {string}")
    public void iVisuallyLongPressUsingOcrText(String elementName, String ocrText) {
        new VisualOcrBL().visuallyLongPressUsingOcrText(elementName, ocrText);
    }

    @When("I visually swipe {string} on {string} using OCR text {string}")
    public void iVisuallySwipeOnElementUsingOcrText(String directionText, String elementName, String ocrText) {
        new VisualOcrBL().visuallySwipeOnElementUsingOcrText(elementName, directionText, ocrText);
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

    @Then("I verify {int} visual elements are present using OCR text {string}")
    public void iVerifyVisualElementsArePresentUsingOcrText(int expectedCount, String ocrText) {
        new VisualOcrBL().verifyVisualElementCountByText(ocrText, ocrText, expectedCount);
    }

    @Then("I verify {int} visual elements are present using image template {string}")
    public void iVerifyVisualElementsArePresentUsingImageTemplate(int expectedCount, String imageTemplatePath) {
        new VisualOcrBL().verifyVisualElementCountByImage(imageTemplatePath, imageTemplatePath, expectedCount);
    }

    @Then("I verify at least {int} visual elements are present using OCR text {string}")
    public void iVerifyAtLeastNVisualElementsArePresentUsingOcrText(int minCount, String ocrText) {
        new VisualOcrBL().verifyAtLeastNVisualElementsPresentByText(ocrText, ocrText, minCount);
    }

    @Then("I verify at least {int} visual elements are present using image template {string}")
    public void iVerifyAtLeastNVisualElementsArePresentUsingImageTemplate(int minCount, String imageTemplatePath) {
        new VisualOcrBL().verifyAtLeastNVisualElementsPresentByImage(imageTemplatePath, imageTemplatePath, minCount);
    }

    @When("I visually find all instances of {string} using image template {string}")
    public void iVisuallyFindAllInstancesUsingImageTemplate(String elementName, String imageTemplatePath) {
        new VisualOcrBL().visuallyFindAllInstancesUsingImageTemplate(elementName, imageTemplatePath);
    }

    @When("I visually find all instances of {string} using OCR text {string}")
    public void iVisuallyFindAllInstancesUsingOcrText(String elementName, String ocrText) {
        new VisualOcrBL().visuallyFindAllInstancesUsingOcrText(elementName, ocrText);
    }

    @When("I visually inspect all instances of {string} using image template {string}")
    public void iVisuallyInspectAllInstancesUsingImageTemplate(String elementName, String imageTemplatePath) {
        new VisualOcrBL().visuallyFindAllInstancesUsingImageTemplate(elementName, imageTemplatePath);
    }

    @When("I visually inspect all instances of {string} using OCR text {string}")
    public void iVisuallyInspectAllInstancesUsingOcrText(String elementName, String ocrText) {
        new VisualOcrBL().visuallyFindAllInstancesUsingOcrText(elementName, ocrText);
    }

    @When("I visually find all instances of {string} using fallback OCR text {string} or image template {string}")
    public void iVisuallyFindAllInstancesUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        new VisualOcrBL().visuallyFindAllInstancesUsingFallbackOcrTextOrImageTemplate(elementName, ocrText, imageTemplatePath);
    }

    @When("I visually find all instances of {string} using fallback image template {string} or OCR text {string}")
    public void iVisuallyFindAllInstancesUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        new VisualOcrBL().visuallyFindAllInstancesUsingFallbackImageTemplateOrOcrText(elementName, imageTemplatePath, ocrText);
    }

    @When("I visually inspect all instances of {string} using fallback OCR text {string} or image template {string}")
    public void iVisuallyInspectAllInstancesUsingFallbackOcrTextOrImageTemplate(String elementName, String ocrText, String imageTemplatePath) {
        new VisualOcrBL().visuallyFindAllInstancesUsingFallbackOcrTextOrImageTemplate(elementName, ocrText, imageTemplatePath);
    }

    @When("I visually inspect all instances of {string} using fallback image template {string} or OCR text {string}")
    public void iVisuallyInspectAllInstancesUsingFallbackImageTemplateOrOcrText(String elementName, String imageTemplatePath, String ocrText) {
        new VisualOcrBL().visuallyFindAllInstancesUsingFallbackImageTemplateOrOcrText(elementName, imageTemplatePath, ocrText);
    }




    @When("I visually click element at index {int} matching OCR text {string}")
    public void iVisuallyClickElementAtIndexMatchingOcrText(int index, String ocrText) {
        new VisualOcrBL().visuallyClickElementAtIndexUsingOcrText(ocrText, index, ocrText);
    }

    @When("I visually click the {string} element matching OCR text {string}")
    public void iVisuallyClickThePositionElementMatchingOcrText(String positionText, String ocrText) {
        new VisualOcrBL().visuallyClickElementByPositionUsingOcrText(ocrText, positionText, ocrText);
    }

    @When("I visually click the {string} element matching image template {string}")
    public void iVisuallyClickThePositionElementMatchingImageTemplate(String positionText, String imageTemplatePath) {
        new VisualOcrBL().visuallyClickElementByPositionUsingImageTemplate(imageTemplatePath, positionText, imageTemplatePath);
    }

    @When("I visually click {string} using OCR text {string} {string} {string}")
    public void iVisuallyClickUsingOcrTextRelative(String elementName, String targetText, String directionText, String anchorText) {
        new VisualOcrBL().visuallyClickUsingOcrTextRelative(elementName, targetText, directionText, anchorText);
    }


    @Then("I verify visual element {string} is present using OCR text {string} {string} {string}")
    public void iVerifyVisualElementIsPresentUsingOcrTextRelative(String elementName, String targetText, String directionText, String anchorText) {
        new VisualOcrBL().verifyVisualElementIsPresentRelativeByText(elementName, targetText, directionText, anchorText);
    }

    @Then("I verify visual element {string} is present using OCR text {string} within region {int} {int} {int} {int}")
    public void iVerifyVisualElementIsPresentUsingOcrTextInRegion(String elementName, String ocrText, int x, int y, int width, int height) {
        new VisualOcrBL().verifyVisualElementIsPresentInRegion(elementName, ocrText, x, y, width, height);
    }

    @When("I wait until visual element is visible using OCR text {string}")
    public void iWaitUntilVisualElementIsVisibleUsingOcrText(String ocrText) {
        new VisualOcrBL().waitUntilVisualElementIsVisibleByText(ocrText);
    }

    @When("I wait until visual element is visible using OCR text {string} within {int} seconds")
    public void iWaitUntilVisualElementIsVisibleUsingOcrTextWithinSeconds(String ocrText, int maxWaitSeconds) {
        new VisualOcrBL().waitUntilVisualElementIsVisibleByText(ocrText, maxWaitSeconds);
    }

    @When("I wait until visual element is visible using image template {string}")
    public void iWaitUntilVisualElementIsVisibleUsingImageTemplate(String imageTemplatePath) {
        new VisualOcrBL().waitUntilVisualElementIsVisibleByImage(imageTemplatePath);
    }

    @When("I wait until visual element is visible using image template {string} within {int} seconds")
    public void iWaitUntilVisualElementIsVisibleUsingImageTemplateWithinSeconds(String imageTemplatePath, int maxWaitSeconds) {
        new VisualOcrBL().waitUntilVisualElementIsVisibleByImage(imageTemplatePath, maxWaitSeconds);
    }

    @When("I wait until visual element is visible using fallback OCR text {string} or image template {string}")
    public void iWaitUntilVisualElementIsVisibleUsingFallbackOcrTextOrImageTemplate(String ocrText, String imageTemplatePath) {
        new VisualOcrBL().waitUntilVisualElementIsVisibleByTextOrImage(ocrText, imageTemplatePath);
    }

    @When("I wait until visual element is visible using fallback OCR text {string} or image template {string} within {int} seconds")
    public void iWaitUntilVisualElementIsVisibleUsingFallbackOcrTextOrImageTemplateWithinSeconds(String ocrText, String imageTemplatePath, int maxWaitSeconds) {
        new VisualOcrBL().waitUntilVisualElementIsVisibleByTextOrImage(ocrText, imageTemplatePath, maxWaitSeconds);
    }

    @When("I wait until visual element is visible using fallback image template {string} or OCR text {string}")
    public void iWaitUntilVisualElementIsVisibleUsingFallbackImageTemplateOrOcrText(String imageTemplatePath, String ocrText) {
        new VisualOcrBL().waitUntilVisualElementIsVisibleByImageOrText(imageTemplatePath, ocrText);
    }

    @When("I wait until visual element is visible using fallback image template {string} or OCR text {string} within {int} seconds")
    public void iWaitUntilVisualElementIsVisibleUsingFallbackImageTemplateOrOcrTextWithinSeconds(String imageTemplatePath, String ocrText, int maxWaitSeconds) {
        new VisualOcrBL().waitUntilVisualElementIsVisibleByImageOrText(imageTemplatePath, ocrText, maxWaitSeconds);
    }
}

