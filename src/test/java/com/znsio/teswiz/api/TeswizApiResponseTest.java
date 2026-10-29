package com.znsio.teswiz.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.znsio.teswiz.exceptions.InvalidTestDataException;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the additive, engine-agnostic body accessors on {@link TeswizApiResponse}: typed JSON mapping
 * ({@code as(Class)} / {@code as(TypeReference)}) and XML reading ({@code asXmlDocument()} / {@code xmlString(xpath)}).
 * These work off the raw response-body string, so they are independent of the RestAssured or Playwright engine that
 * produced the response.
 */
class TeswizApiResponseTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Launch(@JsonProperty("launch_url") String launchUrl, @JsonProperty("id") int id) {
    }

    private static TeswizApiResponse responseWithBody(String body) {
        return new TeswizApiResponse(200, body, body == null ? null : body.getBytes(), Map.of(), 0L);
    }

    // --- as(Class) ---

    @Test
    void asMapsJsonBodyToType() {
        TeswizApiResponse response = responseWithBody("{\"launch_url\":\"https://x?bpToken=abc\",\"id\":7}");

        Launch launch = response.as(Launch.class);

        assertThat(launch.launchUrl()).isEqualTo("https://x?bpToken=abc");
        assertThat(launch.id()).isEqualTo(7);
    }

    @Test
    void asIgnoresUnknownJsonProperties() {
        TeswizApiResponse response = responseWithBody("{\"launch_url\":\"u\",\"id\":1,\"extra\":\"ignored\"}");

        Launch launch = response.as(Launch.class);

        assertThat(launch.launchUrl()).isEqualTo("u");
    }

    @Test
    void asFailsFastOnNonJsonBody() {
        TeswizApiResponse response = responseWithBody("<not-json/>");

        assertThatThrownBy(() -> response.as(Launch.class))
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("JSON");
    }

    // --- as(TypeReference) ---

    @Test
    void asTypeReferenceMapsGenericCollection() {
        TeswizApiResponse response = responseWithBody("[{\"launch_url\":\"a\",\"id\":1},{\"launch_url\":\"b\",\"id\":2}]");

        List<Launch> launches = response.as(new TypeReference<List<Launch>>() {
        });

        assertThat(launches).hasSize(2);
        assertThat(launches.get(1).id()).isEqualTo(2);
    }

    // --- asXmlDocument() ---

    @Test
    void asXmlDocumentParsesBody() {
        TeswizApiResponse response = responseWithBody(
                "<GameResponse><Init status=\"success\"/></GameResponse>");

        Document document = response.asXmlDocument();

        assertThat(document.getDocumentElement().getNodeName()).isEqualTo("GameResponse");
    }

    @Test
    void asXmlDocumentFailsFastOnNonXmlBody() {
        TeswizApiResponse response = responseWithBody("not xml at all");

        assertThatThrownBy(response::asXmlDocument)
                .isInstanceOf(InvalidTestDataException.class)
                .hasMessageContaining("XML");
    }

    // --- xmlString(xpath) ---

    @Test
    void xmlStringReadsAttributeByXpath() {
        TeswizApiResponse response = responseWithBody(
                "<GameResponse><Init status=\"success\"/></GameResponse>");

        String status = response.xmlString("/GameResponse/Init/@status");

        assertThat(status).isEqualTo("success");
    }

    @Test
    void xmlStringReturnsEmptyForAbsentPath() {
        TeswizApiResponse response = responseWithBody(
                "<GameResponse><Init status=\"success\"/></GameResponse>");

        String missing = response.xmlString("/GameResponse/DoesNotExist/@status");

        assertThat(missing).isEmpty();
    }

    @Test
    void xmlStringReadsNestedElementAttribute() {
        TeswizApiResponse response = responseWithBody(
                "<GameResponse><Header><Customer ccy_code=\"GBP\"/></Header></GameResponse>");

        String currency = response.xmlString("/GameResponse/Header/Customer/@ccy_code");

        assertThat(currency).isEqualTo("GBP");
    }
}
