package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.Setup;

class VisualSubsystemTest {

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        Setup.load("configs/teswiz/teswiz_config.properties.template");
        Setup.loadAndUpdateConfigParameters("configs/teswiz/teswiz_config.properties.template");
        System.setProperty("APPLITOOLS_CONFIGURATION", "./configs/applitools_config.json");
        System.setProperty(Setup.IS_VISUAL, "false");
        long threadId = Thread.currentThread().getId();
        com.znsio.teswiz.testng.TestNgTestExecutionContextFactory.create("visual-subsystem-test", 1);
    }

    @Test
    void whenOcrDisabled_callingFindByTextThrowsVisualSubsystemDisabledException() {
        System.setProperty(Setup.IS_OCR_ENABLED, "false");
        assertThat(Runner.isOcrEnabled()).isFalse();

        com.znsio.teswiz.runner.Visual visual = new com.znsio.teswiz.runner.Visual("WebDriver",
                com.znsio.teswiz.entities.Platform.web, null, "testName", "userPersona", "appName");

        assertThatThrownBy(() -> visual.findByText("SPIN"))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
    }

    @Test
    void whenOcrDisabled_callingFindByImageThrowsVisualSubsystemDisabledException() {
        System.setProperty(Setup.IS_OCR_ENABLED, "false");

        com.znsio.teswiz.runner.Visual visual = new com.znsio.teswiz.runner.Visual("WebDriver",
                com.znsio.teswiz.entities.Platform.web, null, "testName", "userPersona", "appName");

        assertThatThrownBy(() -> visual.findByImage(List.of("invalid_path.png")))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
    }

    @Test
    void whenOcrDisabled_callingFindByTextOrImageThrowsVisualSubsystemDisabledException() {
        System.setProperty(Setup.IS_OCR_ENABLED, "false");

        com.znsio.teswiz.runner.Visual visual = new com.znsio.teswiz.runner.Visual("WebDriver",
                com.znsio.teswiz.entities.Platform.web, null, "testName", "userPersona", "appName");

        assertThatThrownBy(() -> visual.findByTextOrImage("SPIN", List.of("spin.png")))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
    }

    @Test
    void whenOcrDisabled_callingFindByImageOrTextThrowsVisualSubsystemDisabledException() {
        System.setProperty(Setup.IS_OCR_ENABLED, "false");

        com.znsio.teswiz.runner.Visual visual = new com.znsio.teswiz.runner.Visual("WebDriver",
                com.znsio.teswiz.entities.Platform.web, null, "testName", "userPersona", "appName");

        assertThatThrownBy(() -> visual.findByImageOrText(List.of("spin.png"), "SPIN"))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
    }

    @Test
    void testSpatialDirectionParsing() {
        assertThat(com.znsio.teswiz.entities.SpatialDirection.fromString("above")).isEqualTo(com.znsio.teswiz.entities.SpatialDirection.ABOVE);
        assertThat(com.znsio.teswiz.entities.SpatialDirection.fromString("below")).isEqualTo(com.znsio.teswiz.entities.SpatialDirection.BELOW);
        assertThat(com.znsio.teswiz.entities.SpatialDirection.fromString("left of")).isEqualTo(com.znsio.teswiz.entities.SpatialDirection.LEFT_OF);
        assertThat(com.znsio.teswiz.entities.SpatialDirection.fromString("right of")).isEqualTo(com.znsio.teswiz.entities.SpatialDirection.RIGHT_OF);
        assertThat(com.znsio.teswiz.entities.SpatialDirection.fromString("near")).isEqualTo(com.znsio.teswiz.entities.SpatialDirection.NEAR);
    }

    @Test
    void testVisualByCreation() {
        com.znsio.teswiz.runner.VisualBy ocrBy = com.znsio.teswiz.runner.VisualBy.ocr("Login");
        assertThat(ocrBy.getType()).isEqualTo(com.znsio.teswiz.runner.VisualBy.VisualByType.OCR_TEXT);
        assertThat(ocrBy.getText()).isEqualTo("Login");

        com.znsio.teswiz.runner.VisualBy imageBy = com.znsio.teswiz.runner.VisualBy.image("logo.png", 0.90);
        assertThat(imageBy.getType()).isEqualTo(com.znsio.teswiz.runner.VisualBy.VisualByType.IMAGE_TEMPLATE);
        assertThat(imageBy.getImagePath()).isEqualTo("logo.png");
        assertThat(imageBy.getConfidenceThreshold()).isEqualTo(0.90);
    }

    @Test
    void testVisualElementToWebElementProxy() {
        com.znsio.teswiz.runner.VisualElement element = new com.znsio.teswiz.runner.VisualElement(10, 20, 100, 50, "TestLabel", null);
        org.openqa.selenium.WebElement webElement = element.toWebElement();
        assertThat(webElement).isNotNull();
        assertThat(webElement.getText()).isEqualTo("TestLabel");
        assertThat(webElement.isDisplayed()).isTrue();
        assertThat(webElement.getLocation().getX()).isEqualTo(10);
        assertThat(webElement.getLocation().getY()).isEqualTo(20);
        assertThat(webElement.getSize().getWidth()).isEqualTo(100);
        assertThat(webElement.getSize().getHeight()).isEqualTo(50);
    }

    @Test
    void testParsePositionIndex() {
        com.znsio.teswiz.businessLayer.visualocr.VisualOcrBL bl = new com.znsio.teswiz.businessLayer.visualocr.VisualOcrBL();
        assertThat(bl.parsePositionIndex("first", 5)).isEqualTo(0);
        assertThat(bl.parsePositionIndex("1st", 5)).isEqualTo(0);
        assertThat(bl.parsePositionIndex("last", 5)).isEqualTo(4);
        assertThat(bl.parsePositionIndex("second", 5)).isEqualTo(1);
        assertThat(bl.parsePositionIndex("2nd", 5)).isEqualTo(1);
        assertThat(bl.parsePositionIndex("3rd", 5)).isEqualTo(2);
        assertThat(bl.parsePositionIndex("4th", 5)).isEqualTo(3);
        assertThat(bl.parsePositionIndex("2", 5)).isEqualTo(2);
    }
}


