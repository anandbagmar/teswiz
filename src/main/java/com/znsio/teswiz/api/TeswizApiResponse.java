package com.znsio.teswiz.api;

import com.znsio.teswiz.tools.JsonSchemaValidator;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public class TeswizApiResponse {
    private final int statusCode;
    private final String responseBody;
    private final byte[] responseBodyBytes;
    private final Map<String, String> headers;
    private final long responseTimeInMs;

    public TeswizApiResponse(int statusCode, String responseBody, byte[] responseBodyBytes, Map<String, String> headers, long responseTimeInMs) {
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
}
