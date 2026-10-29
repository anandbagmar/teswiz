package com.znsio.teswiz.api;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.znsio.teswiz.exceptions.InvalidTestDataException;
import com.znsio.teswiz.tools.JsonSchemaValidator;

public class TeswizApiResponse {
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final int statusCode;
    private final String responseBody;
    private final byte[] responseBodyBytes;
    private final Map<String, String> headers;
    private final long responseTimeInMs;

    public TeswizApiResponse(int statusCode, String responseBody, byte[] responseBodyBytes, Map<String, String> headers,
            long responseTimeInMs) {
        this.statusCode = statusCode;
        this.responseBody = responseBody != null ? responseBody : "";
        this.responseBodyBytes = responseBodyBytes != null ? responseBodyBytes : new byte[0];
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
        this.responseTimeInMs = responseTimeInMs;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public byte[] getResponseBodyAsBytes() {
        return responseBodyBytes;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public long getResponseTimeInMs() {
        return responseTimeInMs;
    }

    public JSONObject asJsonObject() {
        return new JSONObject(responseBody);
    }

    public JSONArray asJsonArray() {
        return new JSONArray(responseBody);
    }

    public boolean matchesJsonSchema(String schemaResourcePath) {
        JsonSchemaValidator.validateJsonFileAgainstSchema("api-response", responseBody, schemaResourcePath);
        return true;
    }

    /**
     * Maps the JSON response body to an instance of the given type using Jackson. The caller supplies the target type,
     * so no engine-specific or consumer-specific type is referenced here. Use {@link #as(TypeReference)} for a generic
     * type such as {@code List<Foo>}.
     *
     * @param type the target type to map the JSON body to
     * @param <T>  the target type
     * @return the mapped instance
     * @throws InvalidTestDataException if the body is not valid JSON or cannot be mapped to the type
     */
    public <T> T as(Class<T> type) {
        try {
            return JSON_MAPPER.readValue(responseBody, type);
        } catch (Exception e) {
            throw new InvalidTestDataException(
                    "Could not map the JSON response body to " + type.getName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Maps the JSON response body to the given generic type using Jackson (e.g.
     * {@code new TypeReference<List<Foo>>(){}}). The caller supplies the target type, so no engine-specific or
     * consumer-specific type is referenced here.
     *
     * @param typeReference the generic target type to map the JSON body to
     * @param <T>           the target type
     * @return the mapped instance
     * @throws InvalidTestDataException if the body is not valid JSON or cannot be mapped to the type
     */
    public <T> T as(TypeReference<T> typeReference) {
        try {
            return JSON_MAPPER.readValue(responseBody, typeReference);
        } catch (Exception e) {
            throw new InvalidTestDataException(
                    "Could not map the JSON response body to " + typeReference.getType() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Parses the response body as an XML {@link Document}. The parser is configured to disable external entity
     * resolution (XXE-safe), since the body is untrusted server data. Mirrors {@link #asJsonObject()} for XML suppliers
     * and is engine-agnostic (it reads the raw body string, independent of RestAssured or Playwright).
     *
     * @return the parsed XML document
     * @throws InvalidTestDataException if the body is not well-formed XML
     */
    public Document asXmlDocument() {
        try {
            return newSecureDocumentBuilder().parse(new InputSource(new StringReader(responseBody)));
        } catch (Exception e) {
            throw new InvalidTestDataException("Could not parse the response body as XML: " + e.getMessage(), e);
        }
    }

    /**
     * Evaluates the given XPath expression against the XML response body and returns the matched text (an element's
     * text or an attribute value). Returns an empty string when the path matches nothing, so an absent value reads as a
     * clean negative rather than throwing. Mirrors reading a JSON field, but for XML suppliers.
     *
     * @param xpathExpression the XPath to evaluate (e.g. {@code /GameResponse/Init/@status})
     * @return the matched text, or an empty string if the path matches nothing
     * @throws InvalidTestDataException if the body is not well-formed XML or the XPath is invalid
     */
    public String xmlString(String xpathExpression) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            return (String) xpath.evaluate(xpathExpression, asXmlDocument(), XPathConstants.STRING);
        } catch (InvalidTestDataException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTestDataException("Could not evaluate XPath '" + xpathExpression
                    + "' against the XML response body: " + e.getMessage(), e);
        }
    }

    private static DocumentBuilder newSecureDocumentBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setExpandEntityReferences(false);
        return factory.newDocumentBuilder();
    }
}
