package com.znsio.teswiz.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Unit tests for one {@link HeartBeat} check: the API is "up" only when it answers 200. The check goes through
 * teswiz's shared fallback HTTP client, so a host with one unreachable address still reports "up" when another
 * address answers.
 */
class HeartBeatCheckTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/up", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/down", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void anApiAnswering200_isUp() {
        assertThat(new HeartBeat("persona", baseUrl + "/up", 1).checkHeartBeat()).isTrue();
    }

    @Test
    void anApiAnsweringAnError_isDown() {
        assertThat(new HeartBeat("persona", baseUrl + "/down", 1).checkHeartBeat()).isFalse();
    }

    @Test
    void anApiThatCannotBeReached_isDown() {
        server.stop(0);

        assertThat(new HeartBeat("persona", baseUrl + "/up", 1).checkHeartBeat()).isFalse();
    }

    @Test
    void aMalformedUrl_isDown() {
        assertThat(new HeartBeat("persona", "not a url", 1).checkHeartBeat()).isFalse();
    }
}
