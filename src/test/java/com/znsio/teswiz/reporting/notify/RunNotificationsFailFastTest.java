package com.znsio.teswiz.reporting.notify;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.znsio.teswiz.exceptions.NotificationException;

/**
 * Unit tests for the opt-in fail-fast policy in {@link RunNotifications}. By default a notification problem is
 * swallowed so a transient Slack/SMTP blip never flips a green run red. When {@code notify.fail.on.error=true}, any
 * outcome that means "the run wanted to notify but could not" — a missing destination for an enabled channel, or an
 * actual delivery failure — escalates to a {@link NotificationException} so the run fails fast. A fully-delivered
 * (or deliberately skipped-as-disabled) set of outcomes never escalates, regardless of the flag.
 */
class RunNotificationsFailFastTest {

    @Test
    void failOnErrorDisabledNeverThrowsEvenWhenDeliveryFailed() {
        Map<String, NotificationOutcome> outcomes = Map.of("run.slack", NotificationOutcome.FAILED);

        assertThatCode(() -> RunNotifications.escalateIfConfigured(outcomes, false)).doesNotThrowAnyException();
    }

    @Test
    void failOnErrorEscalatesAFailedDelivery() {
        Map<String, NotificationOutcome> outcomes = Map.of("run.email", NotificationOutcome.FAILED);

        assertThatThrownBy(() -> RunNotifications.escalateIfConfigured(outcomes, true))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("run.email");
    }

    @Test
    void failOnErrorEscalatesAMissingDestinationForAnEnabledChannel() {
        Map<String, NotificationOutcome> outcomes = Map.of("run.slack", NotificationOutcome.SKIPPED_NO_DESTINATION);

        assertThatThrownBy(() -> RunNotifications.escalateIfConfigured(outcomes, true))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("run.slack");
    }

    @Test
    void failOnErrorDoesNotEscalateWhenEverythingWasSentOrDisabled() {
        Map<String, NotificationOutcome> outcomes = new LinkedHashMap<>();
        outcomes.put("run.slack", NotificationOutcome.SENT);
        outcomes.put("run.email", NotificationOutcome.SKIPPED_DISABLED);

        assertThatCode(() -> RunNotifications.escalateIfConfigured(outcomes, true)).doesNotThrowAnyException();
    }

    @Test
    void anEmptyOutcomeMapNeverEscalates() {
        assertThat(Map.<String, NotificationOutcome>of()).isEmpty();
        assertThatCode(() -> RunNotifications.escalateIfConfigured(Map.of(), true)).doesNotThrowAnyException();
    }
}
