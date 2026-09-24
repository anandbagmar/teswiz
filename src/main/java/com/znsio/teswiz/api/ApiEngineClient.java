package com.znsio.teswiz.api;

import java.util.Map;

public interface ApiEngineClient {
    TeswizApiResponse get(String url, Map<String, Object> queryParams, Map<String, String> headers);

    TeswizApiResponse post(String url, Object body, Map<String, String> headers);

    TeswizApiResponse patch(String url, Object body, Map<String, String> headers);

    TeswizApiResponse delete(String url, Map<String, String> headers);
}
