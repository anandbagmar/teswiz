package com.znsio.teswiz.api;

import java.io.File;
import java.util.Map;

public interface ApiEngineClient {
    TeswizApiResponse get(String url, Map<String, Object> queryParams, Map<String, String> headers);

    TeswizApiResponse post(String url, Object body, Map<String, String> headers);

    TeswizApiResponse postMultipart(String url, Map<String, Object> formFields, Map<String, File> files, Map<String, String> headers);

    TeswizApiResponse put(String url, Object body, Map<String, String> headers);

    TeswizApiResponse patch(String url, Object body, Map<String, String> headers);

    TeswizApiResponse delete(String url, Map<String, String> headers);

    TeswizApiResponse head(String url, Map<String, String> headers);

    TeswizApiResponse options(String url, Map<String, String> headers);
}
