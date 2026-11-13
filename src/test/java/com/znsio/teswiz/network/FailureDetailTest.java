package com.znsio.teswiz.network;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FailureDetail}: the one-line description of a delivery failure that goes into the logs. It
 * must name the exception type (a bare "HTTP connect timed out" does not say what failed), show the root cause, and
 * never leak a secret such as a Slack webhook URL.
 */
class FailureDetailTest {

    private static final String SECRET_WEBHOOK = "https://hooks.slack.com/services/T000/B000/SECRETVALUE";

    @Test
    void namesTheExceptionTypeAndMessage() {
        String detail = FailureDetail.describe(new HttpConnectTimeoutException("HTTP connect timed out"));

        assertThat(detail).isEqualTo("HttpConnectTimeoutException: HTTP connect timed out");
    }

    @Test
    void namesTheTypeWhenThereIsNoMessage() {
        String detail = FailureDetail.describe(new IOException());

        assertThat(detail).isEqualTo("IOException");
    }

    @Test
    void addsTheRootCauseWhenItDiffersFromTheException() {
        IOException failure = new IOException("send failed", new ConnectException("Network is unreachable"));

        String detail = FailureDetail.describe(failure);

        assertThat(detail).isEqualTo("IOException: send failed (root cause: ConnectException: Network is unreachable)");
    }

    @Test
    void doesNotRepeatTheRootCauseWhenTheExceptionIsTheRoot() {
        String detail = FailureDetail.describe(new ConnectException("refused"));

        assertThat(detail).doesNotContain("root cause");
    }

    @Test
    void redactsTheSecretFromTheDescription() {
        IllegalArgumentException failure = new IllegalArgumentException("Illegal character in " + SECRET_WEBHOOK);

        String detail = FailureDetail.describe(failure, SECRET_WEBHOOK);

        assertThat(detail).doesNotContain("SECRETVALUE").contains("IllegalArgumentException").contains("<redacted>");
    }

    @Test
    void redactsTheSecretFromARootCauseMessageToo() {
        IOException failure = new IOException("send failed",
                new IllegalArgumentException("bad uri " + SECRET_WEBHOOK));

        String detail = FailureDetail.describe(failure, SECRET_WEBHOOK);

        assertThat(detail).doesNotContain("SECRETVALUE");
    }

    @Test
    void aBlankSecretLeavesTheDescriptionUnchanged() {
        String detail = FailureDetail.describe(new IOException("boom"), "");

        assertThat(detail).isEqualTo("IOException: boom");
    }
}
