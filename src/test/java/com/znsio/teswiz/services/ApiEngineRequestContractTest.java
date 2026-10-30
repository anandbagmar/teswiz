package com.znsio.teswiz.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.znsio.teswiz.api.PlaywrightApiManager;
import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.Setup;

/**
 * Pins what each API engine actually puts on the wire, against a local HTTP server that records the raw request.
 *
 * <p>
 * Every {@code API_ENGINE} value ({@code rest-assured}, {@code playwright-java}, {@code playwright-ts}) must send the
 * same bytes for the same call, so a project can switch {@code API_ENGINE} without its payloads changing. Both
 * Playwright values are routed to the same Java client by {@code ApiService}; running {@code playwright-ts} here pins
 * that routing, so a future separate TypeScript API path could not diverge unnoticed. In particular a {@code Map}/{@code Collection} body keeps explicit {@code null} fields and decimal
 * numbers as written (Jackson semantics, which is what RestAssured itself used for a map body), a {@code null} body is
 * sent as an empty body, a {@code null} query-param map adds no query, and an already-encoded query string in the URL
 * is passed through without being encoded again.
 */
class ApiEngineRequestContractTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String SAMPLE_CONFIG = "./configs/ajio/ajio_local_android_config.properties";

    private static HttpServer server;
    private static String baseUrl;
    private static volatile String lastRawQuery;
    private static volatile String lastBody;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/echo", ApiEngineRequestContractTest::recordAndReply);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort() + "/echo";
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @BeforeEach
    void setUp() {
        System.clearProperty(Setup.API_ENGINE);
        lastRawQuery = null;
        lastBody = null;
    }

    @AfterEach
    void tearDown() {
        System.clearProperty(Setup.API_ENGINE);
        PlaywrightApiManager.closeContextForCurrentThread();
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void mapBodyKeepsNullFieldsAtEveryLevel(String apiEngine) throws IOException {
        useEngine(apiEngine);

        ApiService.post(baseUrl, bodyWithNulls(), Map.of());

        JsonNode sent = JSON.readTree(lastBody);
        assertThat(sent.has("currencyCode")).as("top-level null kept").isTrue();
        assertThat(sent.get("currencyCode").isNull()).isTrue();
        assertThat(sent.get("jackpot").has("contributionType")).as("nested-map null kept").isTrue();
        assertThat(sent.get("details").get(0).has("amount")).as("null inside a list kept").isTrue();
        assertThat(sent.get("kept").asText()).isEqualTo("x");
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void mapBodyKeepsDecimalNumbersAsWritten(String apiEngine) throws IOException {
        useEngine(apiEngine);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("amount", 1.0);

        ApiService.post(baseUrl, body, Map.of());

        assertThat(lastBody).isEqualTo("{\"amount\":1.0}");
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void collectionBodyKeepsNullElementsAndFields(String apiEngine) throws IOException {
        useEngine(apiEngine);
        Map<String, Object> element = new LinkedHashMap<>();
        element.put("id", null);
        List<Object> body = new ArrayList<>(Arrays.asList(element, null));

        ApiService.post(baseUrl, body, Map.of());

        JsonNode sent = JSON.readTree(lastBody);
        assertThat(sent.size()).isEqualTo(2);
        assertThat(sent.get(0).has("id")).isTrue();
        assertThat(sent.get(1).isNull()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void nullBodyIsSentAsAnEmptyBody(String apiEngine) throws IOException {
        useEngine(apiEngine);

        TeswizApiResponse response = ApiService.post(baseUrl, null, Map.of("content-type", "application/json"));

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(lastBody).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void nullQueryParamMapAddsNoQuery(String apiEngine) throws IOException {
        useEngine(apiEngine);

        TeswizApiResponse response = ApiService.get(baseUrl, null, Map.of());

        assertThat(response.getStatusCode()).isEqualTo(200);
        assertThat(lastRawQuery).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void alreadyEncodedQueryInUrlIsNotEncodedAgain(String apiEngine) throws IOException {
        useEngine(apiEngine);

        ApiService.post(baseUrl + "?token=a%3Ab%2Fc&name=x%20y", null, Map.of());

        assertThat(lastRawQuery).isEqualTo("token=a%3Ab%2Fc&name=x%20y");
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void queryParamValuesWithReservedCharactersAreEncodedOnce(String apiEngine) throws IOException {
        useEngine(apiEngine);
        Map<String, Object> queryParams = new LinkedHashMap<>();
        queryParams.put("token", "a:b/c");
        queryParams.put("name", "x y");

        ApiService.get(baseUrl, queryParams, Map.of());

        // A space may legitimately go out as '+' (Playwright) or '%20' (RestAssured); what must hold on both engines is
        // that each value is encoded exactly once ('%' itself never re-encoded) and decodes back to what was given.
        assertThat(lastRawQuery).doesNotContain("%25");
        assertThat(decodedQuery(lastRawQuery)).containsExactly(Map.entry("token", "a:b/c"), Map.entry("name", "x y"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void unencodedSpaceInUrlIsEncodedOnce(String apiEngine) throws IOException {
        useEngine(apiEngine);

        ApiService.post(baseUrl + "?name=x y", null, Map.of());

        assertThat(lastRawQuery).isEqualTo("name=x%20y");
    }

    @ParameterizedTest
    @ValueSource(strings = { "rest-assured", "playwright-java", "playwright-ts" })
    void mapBodyWithOrgJsonValuesIsSentAsJson(String apiEngine) throws IOException {
        useEngine(apiEngine);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("object", new JSONObject("{\"a\":1}"));
        body.put("array", new JSONArray("[1,null]"));
        body.put("orgJsonNull", JSONObject.NULL);

        ApiService.post(baseUrl, body, Map.of());

        assertThat(lastBody).isEqualTo("{\"object\":{\"a\":1},\"array\":[1,null],\"orgJsonNull\":null}");
    }

    /**
     * Selects the engine the way a real run does - through the loaded config file - and fails fast if it did not take
     * effect, so a case can never silently run on the default engine instead of the one it names.
     */
    private void useEngine(String apiEngine) throws IOException {
        String content = Files.readString(Path.of(SAMPLE_CONFIG)) + System.lineSeparator() + Setup.API_ENGINE + "="
                + apiEngine + System.lineSeparator();
        Path configFile = Files.writeString(tempDir.resolve("config.properties"), content);
        Setup.load(configFile.toString());
        Setup.loadAndUpdateConfigParameters(configFile.toString());
        assertThat(Runner.getApiEngine().getConfigValue()).as("active API engine").isEqualTo(apiEngine);
    }

    private static Map<String, String> decodedQuery(String rawQuery) {
        Map<String, String> decoded = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            String[] nameAndValue = pair.split("=", 2);
            decoded.put(URLDecoder.decode(nameAndValue[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(nameAndValue[1], StandardCharsets.UTF_8));
        }
        return decoded;
    }

    private static Map<String, Object> bodyWithNulls() {
        Map<String, Object> jackpot = new LinkedHashMap<>();
        jackpot.put("contributionType", null);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("amount", null);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("currencyCode", null);
        body.put("kept", "x");
        body.put("jackpot", jackpot);
        body.put("details", List.of(detail));
        return body;
    }

    private static void recordAndReply(HttpExchange exchange) throws IOException {
        lastRawQuery = exchange.getRequestURI().getRawQuery();
        try (InputStream in = exchange.getRequestBody()) {
            lastBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] reply = "{}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, reply.length);
        exchange.getResponseBody().write(reply);
        exchange.close();
    }
}
