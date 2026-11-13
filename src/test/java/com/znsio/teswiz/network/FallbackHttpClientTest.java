package com.znsio.teswiz.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for {@link FallbackHttpClient}: the HTTP client teswiz's own outgoing calls share. When the host has
 * several addresses and one cannot be reached, it connects to the next one (each address once, with a short connect
 * timeout) and records a network note. It never tries another address once the request has been sent, so a request
 * is never sent twice. These tests use real sockets on this machine: a server on {@code 127.0.0.1}, and
 * {@code 192.0.2.1} (an address reserved for documentation, which no machine answers) as the unreachable one.
 */
class FallbackHttpClientTest {

    private static final String HOST = "fallback.test";
    private static final Duration SHORT_CONNECT = Duration.ofMillis(300);
    private static final Duration CALL_TIMEOUT = Duration.ofSeconds(5);
    private static final AddressFallbackSettings ON = new AddressFallbackSettings(true, SHORT_CONNECT);
    private static final AddressFallbackSettings OFF = new AddressFallbackSettings(false, SHORT_CONNECT);
    private static final InetAddress UNREACHABLE = literal("192.0.2.1");
    private static final InetAddress ALSO_UNREACHABLE = literal("192.0.2.2");
    private static final InetAddress LOCAL = literal("127.0.0.1");

    private final NetworkNotes notes = new NetworkNotes();
    private final AtomicInteger hits = new AtomicInteger();
    private final List<String> resolved = new ArrayList<>();
    private HttpServer server;
    private int port;

    private static InetAddress literal(String address) {
        try {
            return InetAddress.getByName(address);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(LOCAL, 0), 0);
        server.createContext("/", this::answer);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void answer(HttpExchange exchange) throws IOException {
        hits.incrementAndGet();
        if ("/redirect".equals(exchange.getRequestURI().getPath())) {
            exchange.getResponseHeaders().add("Location", "http://" + HOST + ":" + port + "/target");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
            return;
        }
        byte[] requestBody = exchange.getRequestBody().readAllBytes();
        String marker = exchange.getRequestHeaders().getFirst("X-Marker");
        byte[] reply = ("ok " + new String(requestBody, StandardCharsets.UTF_8) + " " + marker)
                .getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, reply.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(reply);
        }
    }

    private FallbackHttpClient client(AddressFallbackSettings settings, ProxySettings proxy,
            InetAddress... addresses) {
        AddressResolver resolver = host -> {
            resolved.add(host);
            return List.of(addresses);
        };
        return new FallbackHttpClient("test", settings, proxy, resolver, notes);
    }

    private String url(String path) {
        return "http://" + HOST + ":" + port + path;
    }

    @Test
    void anUnreachableFirstAddress_isSkipped_andTheCallIsAnsweredByTheNextOne() throws IOException {
        FallbackHttpClient client = client(ON, ProxySettings.none(), UNREACHABLE, LOCAL);

        HttpResponse response = client.send(HttpRequest.get(url("/"), CALL_TIMEOUT));

        assertThat(response.status()).isEqualTo(200);
        assertThat(hits).hasValue(1);
        assertThat(notes.notes()).singleElement().satisfies(note -> {
            assertThat(note.purpose()).isEqualTo("test");
            assertThat(note.host()).isEqualTo(HOST);
            assertThat(note.unreachable()).extracting(NetworkNote.UnreachableAddress::address)
                    .containsExactly("192.0.2.1");
            assertThat(note.connectedAddress()).isEqualTo("127.0.0.1");
        });
    }

    @Test
    void aReachableFirstAddress_meansNoNote() throws IOException {
        FallbackHttpClient client = client(ON, ProxySettings.none(), LOCAL, UNREACHABLE);

        HttpResponse response = client.send(HttpRequest.get(url("/"), CALL_TIMEOUT));

        assertThat(response.status()).isEqualTo(200);
        assertThat(notes.notes()).isEmpty();
    }

    @Test
    void everyAddressUnreachable_failsTheCall_andRecordsANote() {
        FallbackHttpClient client = client(ON, ProxySettings.none(), UNREACHABLE, ALSO_UNREACHABLE);

        assertThatThrownBy(() -> client.send(HttpRequest.get(url("/"), CALL_TIMEOUT)))
                .isInstanceOf(IOException.class);

        assertThat(notes.notes()).singleElement().satisfies(note -> {
            assertThat(note.connected()).isFalse();
            assertThat(note.unreachable()).hasSize(2);
        });
    }

    @Test
    void aFailureAfterTheRequestWasSent_isNeverRetriedOnAnotherAddress() throws IOException {
        try (ServerSocket dropsAfterReading = new ServerSocket(0, 50, LOCAL)) {
            AtomicInteger accepted = new AtomicInteger();
            Thread acceptor = closeEachConnectionAfterTheRequest(dropsAfterReading, accepted);
            FallbackHttpClient client = client(ON, ProxySettings.none(), LOCAL, LOCAL);
            String brokenUrl = "http://" + HOST + ":" + dropsAfterReading.getLocalPort() + "/";

            assertThatThrownBy(() -> client.send(HttpRequest.post(brokenUrl, "application/json",
                    "{}".getBytes(StandardCharsets.UTF_8), CALL_TIMEOUT))).isInstanceOf(IOException.class);

            acceptor.join(1000);
            assertThat(accepted).hasValue(1);
            assertThat(notes.notes()).isEmpty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void withFallbackOff_onlyTheFirstAddressIsTried() {
        FallbackHttpClient client = client(OFF, ProxySettings.none(), UNREACHABLE, LOCAL);

        assertThatThrownBy(() -> client.send(HttpRequest.get(url("/"), Duration.ofMillis(500))))
                .isInstanceOf(IOException.class);

        assertThat(hits).hasValue(0);
        assertThat(notes.notes()).isEmpty();
    }

    @Test
    void thePostBodyAndHeaders_reachTheServer() throws IOException {
        FallbackHttpClient client = client(ON, ProxySettings.none(), LOCAL);

        HttpResponse response = client.send(HttpRequest.post(url("/"), "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8), CALL_TIMEOUT).withHeader("X-Marker", "m1"));

        assertThat(response.bodyText()).isEqualTo("ok hello m1");
    }

    @Test
    void aRedirect_isNotFollowedUnlessAsked() throws IOException {
        FallbackHttpClient client = client(ON, ProxySettings.none(), LOCAL);

        HttpResponse notFollowed = client.send(HttpRequest.get(url("/redirect"), CALL_TIMEOUT));
        HttpResponse followed = client.send(HttpRequest.get(url("/redirect"), CALL_TIMEOUT).followingRedirects());

        assertThat(notFollowed.status()).isEqualTo(302);
        assertThat(notFollowed.header("Location")).contains(url("/target"));
        assertThat(followed.status()).isEqualTo(200);
    }

    @Test
    void withAProxy_theProxyLooksUpTheHost_soThereIsNoFallback() throws IOException {
        ProxySettings proxy = ProxySettings.resolve(Map.of("PROXY_URL", "http://127.0.0.1:" + port)::get,
                Map.<String, String>of()::get);
        FallbackHttpClient client = client(ON, proxy, UNREACHABLE, LOCAL);

        HttpResponse response = client.send(HttpRequest.get(url("/"), CALL_TIMEOUT));

        assertThat(response.status()).isEqualTo(200);
        assertThat(resolved).isEmpty();
        assertThat(notes.notes()).isEmpty();
    }

    private static Thread closeEachConnectionAfterTheRequest(ServerSocket serverSocket, AtomicInteger accepted) {
        Thread acceptor = new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    try (Socket socket = serverSocket.accept()) {
                        accepted.incrementAndGet();
                        readRequestHead(socket.getInputStream());
                    }
                }
            } catch (IOException ignored) {
                // the server socket was closed at the end of the test
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
        return acceptor;
    }

    private static void readRequestHead(InputStream in) throws IOException {
        int matched = 0;
        byte[] endOfHead = "\r\n\r\n".getBytes(StandardCharsets.US_ASCII);
        int next;
        while (matched < endOfHead.length && (next = in.read()) != -1) {
            matched = next == endOfHead[matched] ? matched + 1 : (next == endOfHead[0] ? 1 : 0);
        }
    }
}
