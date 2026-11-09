package com.znsio.teswiz.web.playwright.screen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.example.consumer.model.OrderLine;
import com.example.consumer.screen.checkout.CheckoutScreen;
import com.example.consumer.screen.checkout.CheckoutSummary;
import com.znsio.teswiz.screen.ScreenShotScreen;
import com.znsio.teswiz.screen.generateddemo.GeneratedPlaywrightTsTestScreen;

class PlaywrightTsScreenActionExecutorTest {
    @Test
    void shouldTreatFrameworkScreenContractsAsScreens() {
        assertThat(PlaywrightTsScreenActionExecutor.isScreenContract(GeneratedPlaywrightTsTestScreen.class)).isTrue();
        assertThat(PlaywrightTsScreenActionExecutor.isScreenContract(ScreenShotScreen.class)).isTrue();
    }

    @Test
    void shouldTreatScreenContractsInConsumerNamespaceAsScreens() {
        assertThat(PlaywrightTsScreenActionExecutor.isScreenContract(CheckoutScreen.class)).isTrue();
    }

    @Test
    void shouldNotTreatConcreteValueTypesNextToConsumerScreensAsScreens() {
        assertThat(PlaywrightTsScreenActionExecutor.isScreenContract(CheckoutSummary.class)).isFalse();
    }

    @Test
    void shouldNotTreatTypesOutsideAScreenPackageAsScreens() {
        assertThat(PlaywrightTsScreenActionExecutor.isScreenContract(OrderLine.class)).isFalse();
        assertThat(PlaywrightTsScreenActionExecutor.isScreenContract(String.class)).isFalse();
        assertThat(PlaywrightTsScreenActionExecutor.isScreenContract(boolean.class)).isFalse();
    }
}
