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
}
