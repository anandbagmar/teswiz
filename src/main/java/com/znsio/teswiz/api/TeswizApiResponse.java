package com.znsio.teswiz.api;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;

public class TeswizApiResponse {
    private final int statusCode;
    private final String responseBody;
    private final byte[] responseBodyBytes;
    private final Map<String, String> headers;

    public TeswizApiResponse(int statusCode, String responseBody, byte[] responseBodyBytes, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.responseBody = responseBody != null ? responseBody : "";
        this.responseBodyBytes = responseBodyBytes != null ? responseBodyBytes : new byte[0];
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
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

    public JSONObject asJsonObject() {
        return new JSONObject(responseBody);
    }

    public JSONArray asJsonArray() {
        return new JSONArray(responseBody);
    }
}
