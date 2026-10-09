package com.znsio.teswiz.config.browser;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;

import com.znsio.teswiz.config.TeswizRuntimeConfiguration;

class WindowSizingResolverTest {

    @AfterEach
    void tearDown() {
        System.clearProperty(TeswizRuntimeConfiguration.DRIVER_VIEWPORT_WIDTH);
        System.clearProperty(TeswizRuntimeConfiguration.DRIVER_VIEWPORT_HEIGHT);
    }

    @Test
    void shouldResolveBuiltInDefaultViewportWhenNothingIsOverridden() {
        Dimension defaultViewport = WindowSizingResolver.defaultViewport();

        assertThat(defaultViewport.getWidth()).isEqualTo(1280);
        assertThat(defaultViewport.getHeight()).isEqualTo(960);
    }

    @Test
    void shouldResolveWidthAndHeightSeparatelyFromTheSameSourceAsTheDimension() {
        assertThat(WindowSizingResolver.defaultWidth()).isEqualTo(1280);
        assertThat(WindowSizingResolver.defaultHeight()).isEqualTo(960);
    }

    @Test
    void shouldHonourOverriddenViewportWidthAndHeight() {
        System.setProperty(TeswizRuntimeConfiguration.DRIVER_VIEWPORT_WIDTH, "1600");
        System.setProperty(TeswizRuntimeConfiguration.DRIVER_VIEWPORT_HEIGHT, "900");

        Dimension overridden = WindowSizingResolver.defaultViewport();

        assertThat(overridden.getWidth()).isEqualTo(1600);
        assertThat(overridden.getHeight()).isEqualTo(900);
    }
}
