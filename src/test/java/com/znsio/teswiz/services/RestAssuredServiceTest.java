package com.znsio.teswiz.services;

import com.sun.net.httpserver.HttpServer;
import com.znsio.teswiz.exceptions.EnvironmentSetupException;
import com.znsio.teswiz.filters.EnvironmentIssueFilter;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@code EnvironmentIssueFilter} is registered globally on {@link RestAssured} by
 * {@code Setup.registerEnvironmentIssueFilterIfEnabled()} (once per run, gated by
 * {@code DISABLE_ENVIRONMENT_ISSUE_FILTER}), not per-request by {@link RestAssuredService}.
 * These tests exercise that same global-registration mechanism directly.
 */
class RestAssuredServiceTest {

    private static HttpServer server;
    private static String serviceUnavailableUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/unavailable", exchange -> {
            byte[] body = "Service Unavailable".getBytes();
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        serviceUnavailableUrl = "http://localhost:" + server.getAddress().getPort() + "/unavailable";
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @AfterEach
    void clearFilters() {
        RestAssured.replaceFiltersWith(List.of());
    }

    @Test
    void throwsEnvironmentSetupExceptionWhenFilterIsRegistered() {
        RestAssured.filters(new EnvironmentIssueFilter());

        assertThatThrownBy(() -> RestAssuredService.getHttpResponse(serviceUnavailableUrl))
                .isInstanceOf(EnvironmentSetupException.class)
                .hasMessageContaining("503");
    }

    @Test
    void doesNotThrowWhenFilterIsNotRegistered() {
        Response response = RestAssuredService.getHttpResponse(serviceUnavailableUrl);

        assertThat(response.getStatusCode()).isEqualTo(503);
    }
}
