package com.znsio.teswiz.config.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.znsio.teswiz.exceptions.InvalidTestDataException;

/**
 * Tests how {@link AppPathResolver} reaches an app URL: a HEAD to validate it, then a streamed GET to download it,
 * both following redirects, against a local server.
 */
class AppPathResolverNetworkTest {

    private static final byte[] APP_CONTENT = "pretend this is an apk".getBytes(StandardCharsets.UTF_8);

    private final List<String> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/app.apk", this::serveApp);
        server.createContext("/moved.apk", this::redirectToApp);
        server.createContext("/gone.apk", exchange -> reply(exchange, 404, new byte[0]));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void serveApp(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
        reply(exchange, 200, APP_CONTENT);
    }

    private void redirectToApp(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath());
        exchange.getResponseHeaders().add("Location", baseUrl + "/app.apk");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static void reply(HttpExchange exchange, int status, byte[] body) throws IOException {
        boolean head = "HEAD".equals(exchange.getRequestMethod());
        exchange.sendResponseHeaders(status, head || body.length == 0 ? -1 : body.length);
        try (OutputStream out = exchange.getResponseBody()) {
            if (!head) {
                out.write(body);
            }
        }
    }

    @Test
    void anAppUrl_isValidatedWithAHead_thenDownloadedWithAGet(@TempDir Path directory) throws IOException {
        String resolved = AppPathResolver.resolveAppPath(baseUrl + "/app.apk", directory.toString());

        assertThat(Files.readAllBytes(Path.of(resolved))).isEqualTo(APP_CONTENT);
        assertThat(requests).containsExactly("HEAD /app.apk", "GET /app.apk");
    }

    @Test
    void aRedirectingAppUrl_isFollowedForBothTheHeadAndTheDownload(@TempDir Path directory) throws IOException {
        String resolved = AppPathResolver.resolveAppPath(baseUrl + "/moved.apk", directory.toString());

        assertThat(Files.readAllBytes(Path.of(resolved))).isEqualTo(APP_CONTENT);
        assertThat(requests).containsExactly("HEAD /moved.apk", "HEAD /app.apk", "GET /moved.apk", "GET /app.apk");
    }

    @Test
    void anAppUrlThatIsNotFound_isRejected_andNothingIsDownloaded(@TempDir Path directory) {
        assertThatThrownBy(() -> AppPathResolver.resolveAppPath(baseUrl + "/gone.apk", directory.toString()))
                .isInstanceOf(InvalidTestDataException.class).hasMessageContaining("URL is not accessible");

        assertThat(directory.resolve("gone.apk")).doesNotExist();
    }

    @Test
    void anAppUrlWithNobodyListening_isRejected(@TempDir Path directory) {
        server.stop(0);

        assertThatThrownBy(() -> AppPathResolver.resolveAppPath(baseUrl + "/app.apk", directory.toString()))
                .isInstanceOf(InvalidTestDataException.class).hasMessageContaining("Failed to make a connection");
    }
}
