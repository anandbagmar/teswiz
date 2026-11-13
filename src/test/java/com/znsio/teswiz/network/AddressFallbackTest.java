package com.znsio.teswiz.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AddressFallback}: the rule every teswiz connection follows when a host has several addresses.
 * Each address is tried once, in order. An address that fails while connecting (nothing sent yet) is skipped and the
 * next one is tried; any other failure ends the call at once, so a request is never sent twice. Whenever an address
 * had to be skipped, one {@link NetworkNote} is recorded.
 */
class AddressFallbackTest {

    private static final InetAddress FIRST = address(10, 0, 0, 1);
    private static final InetAddress SECOND = address(10, 0, 0, 2);
    private static final InetAddress THIRD = address(10, 0, 0, 3);

    private final NetworkNotes notes = new NetworkNotes();
    private final AddressFallback fallback = new AddressFallback(notes);
    private final List<InetAddress> tried = new ArrayList<>();

    private static InetAddress address(int a, int b, int c, int d) {
        try {
            return InetAddress.getByAddress(new byte[] {(byte) a, (byte) b, (byte) c, (byte) d});
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private AddressFallback.Attempt<String> unreachable(InetAddress... unreachableAddresses) {
        List<InetAddress> down = List.of(unreachableAddresses);
        return address -> {
            tried.add(address);
            if (down.contains(address)) {
                throw new AddressFallback.NotConnectedException(new SocketTimeoutException("connect timed out"));
            }
            return "ok via " + address.getHostAddress();
        };
    }

    @Test
    void theFirstAddressWorking_meansNoOtherIsTried_andNoNote() throws IOException {
        String result = fallback.run("slack", "hooks.slack.com", 443, List.of(FIRST, SECOND), unreachable());

        assertThat(result).isEqualTo("ok via 10.0.0.1");
        assertThat(tried).containsExactly(FIRST);
        assertThat(notes.notes()).isEmpty();
    }

    @Test
    void anUnreachableFirstAddress_isSkipped_andTheNextOneIsUsed() throws IOException {
        String result = fallback.run("slack", "hooks.slack.com", 443, List.of(FIRST, SECOND, THIRD),
                unreachable(FIRST));

        assertThat(result).isEqualTo("ok via 10.0.0.2");
        assertThat(tried).containsExactly(FIRST, SECOND);
    }

    @Test
    void aSkippedAddress_isRecordedAsANote_namingTheAddressThatWorked() throws IOException {
        fallback.run("slack", "hooks.slack.com", 443, List.of(FIRST, SECOND), unreachable(FIRST));

        assertThat(notes.notes()).singleElement().satisfies(note -> {
            assertThat(note.purpose()).isEqualTo("slack");
            assertThat(note.host()).isEqualTo("hooks.slack.com");
            assertThat(note.port()).isEqualTo(443);
            assertThat(note.unreachable()).extracting(NetworkNote.UnreachableAddress::address)
                    .containsExactly("10.0.0.1");
            assertThat(note.unreachable().get(0).reason()).contains("SocketTimeoutException");
            assertThat(note.connectedAddress()).isEqualTo("10.0.0.2");
        });
    }

    @Test
    void everyAddressUnreachable_throwsTheOriginalFailure_andRecordsANoteWithNoConnection() {
        assertThatThrownBy(() -> fallback.run("slack", "hooks.slack.com", 443, List.of(FIRST, SECOND),
                unreachable(FIRST, SECOND))).isInstanceOf(SocketTimeoutException.class)
                .hasMessage("connect timed out");

        assertThat(tried).containsExactly(FIRST, SECOND);
        assertThat(notes.notes()).singleElement().satisfies(note -> {
            assertThat(note.connected()).isFalse();
            assertThat(note.unreachable()).hasSize(2);
        });
    }

    @Test
    void aFailureAfterConnecting_endsTheCallAtOnce_soTheRequestIsNeverSentTwice() {
        AddressFallback.Attempt<String> failsAfterSending = address -> {
            tried.add(address);
            throw new IOException("unexpected end of stream");
        };

        assertThatThrownBy(() -> fallback.run("slack", "hooks.slack.com", 443, List.of(FIRST, SECOND),
                failsAfterSending)).isInstanceOf(IOException.class).hasMessage("unexpected end of stream");

        assertThat(tried).containsExactly(FIRST);
        assertThat(notes.notes()).isEmpty();
    }

    @Test
    void aFailureAfterConnectingToTheSecondAddress_stillRecordsTheSkippedFirstOne() {
        AddressFallback.Attempt<String> firstDownSecondBreaks = address -> {
            tried.add(address);
            if (FIRST.equals(address)) {
                throw new AddressFallback.NotConnectedException(new SocketTimeoutException("connect timed out"));
            }
            throw new IOException("unexpected end of stream");
        };

        assertThatThrownBy(() -> fallback.run("api", "wallet.example", 443, List.of(FIRST, SECOND, THIRD),
                firstDownSecondBreaks)).hasMessage("unexpected end of stream");

        assertThat(tried).containsExactly(FIRST, SECOND);
        assertThat(notes.notes()).singleElement()
                .satisfies(note -> assertThat(note.connectedAddress()).isEqualTo("10.0.0.2"));
    }

    @Test
    void noAddressAtAll_isAFailure() {
        assertThatThrownBy(() -> fallback.run("slack", "hooks.slack.com", 443, List.of(), unreachable()))
                .isInstanceOf(UnknownHostException.class).hasMessageContaining("hooks.slack.com");
    }
}
