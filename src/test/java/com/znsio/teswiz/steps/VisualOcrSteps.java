package com.znsio.teswiz.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.businessLayer.visualocr.VisualOcrBL;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.Setup;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class VisualOcrSteps {
    private static final Logger LOGGER = LogManager.getLogger(VisualOcrSteps.class.getName());

    @Given("Visual OCR capability is explicitly disabled")
    public void visualOcrCapabilityIsExplicitlyDisabled() {
        System.setProperty(Setup.IS_OCR_ENABLED, "false");
        assertThat(Runner.isOcrEnabled()).isFalse();
        LOGGER.info("Verified IS_OCR_ENABLED is set to false");
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
