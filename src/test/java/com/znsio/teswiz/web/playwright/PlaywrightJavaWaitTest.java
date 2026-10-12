package com.znsio.teswiz.web.playwright;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.WaitForSelectorState;

class PlaywrightJavaWaitTest {

    private Locator mockLocatorWhereFirstReturnsSelf() {
        Locator locator = Mockito.mock(Locator.class);
        when(locator.first()).thenReturn(locator);
        return locator;
    }

    @Test
    void untilVisibleReturnsTrueWhenWaitForSucceeds() {
        Locator locator = mockLocatorWhereFirstReturnsSelf();
        doNothing().when(locator).waitFor(any());

        boolean result = PlaywrightJavaWait.untilVisible(locator, Duration.ofSeconds(5));

        assertThat(result).isTrue();
    }

    @Test
    void untilVisibleReturnsFalseWhenWaitForTimesOut() {
        Locator locator = mockLocatorWhereFirstReturnsSelf();
        doThrow(new PlaywrightException("Timeout 5000ms exceeded")).when(locator).waitFor(any());

        boolean result = PlaywrightJavaWait.untilVisible(locator, Duration.ofSeconds(5));

        assertThat(result).isFalse();
    }

    @Test
    void untilVisibleRequestsVisibleStateWithTheGivenTimeout() {
        Locator locator = mockLocatorWhereFirstReturnsSelf();
        doNothing().when(locator).waitFor(any());

        PlaywrightJavaWait.untilVisible(locator, Duration.ofSeconds(7));

        ArgumentCaptor<Locator.WaitForOptions> options = ArgumentCaptor.forClass(Locator.WaitForOptions.class);
        verify(locator).waitFor(options.capture());
        assertThat(options.getValue().state).isEqualTo(WaitForSelectorState.VISIBLE);
        assertThat(options.getValue().timeout).isEqualTo(7000.0);
    }

    @Test
    void untilPresentRequestsAttachedState() {
        Locator locator = mockLocatorWhereFirstReturnsSelf();
        doNothing().when(locator).waitFor(any());

        PlaywrightJavaWait.untilPresent(locator, Duration.ofSeconds(3));

        ArgumentCaptor<Locator.WaitForOptions> options = ArgumentCaptor.forClass(Locator.WaitForOptions.class);
        verify(locator).waitFor(options.capture());
        assertThat(options.getValue().state).isEqualTo(WaitForSelectorState.ATTACHED);
    }

    @Test
    void untilHiddenRequestsHiddenState() {
        Locator locator = mockLocatorWhereFirstReturnsSelf();
        doNothing().when(locator).waitFor(any());

        PlaywrightJavaWait.untilHidden(locator, Duration.ofSeconds(3));

        ArgumentCaptor<Locator.WaitForOptions> options = ArgumentCaptor.forClass(Locator.WaitForOptions.class);
        verify(locator).waitFor(options.capture());
        assertThat(options.getValue().state).isEqualTo(WaitForSelectorState.HIDDEN);
    }

    @Test
    void untilHiddenReturnsFalseOnTimeout() {
        Locator locator = mockLocatorWhereFirstReturnsSelf();
        doThrow(new PlaywrightException("Timeout")).when(locator).waitFor(any());

        boolean result = PlaywrightJavaWait.untilHidden(locator, Duration.ofSeconds(1));

        assertThat(result).isFalse();
    }
}
