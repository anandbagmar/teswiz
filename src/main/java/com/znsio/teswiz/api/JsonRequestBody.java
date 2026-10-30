package com.znsio.teswiz.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.znsio.teswiz.exceptions.InvalidTestDataException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Turns a {@code Map} or {@code Collection} request body into JSON the same way for every API engine, so switching
 * {@code API_ENGINE} never changes what goes on the wire.
 *
 * <p>The JSON matches what RestAssured itself sends for a map body (Jackson): explicit {@code null} fields and
 * {@code null} list elements are kept, numbers are written as given ({@code 1.0} stays {@code 1.0}), and map key order is
 * preserved. org.json values nested inside ({@code JSONObject}, {@code JSONArray}, {@code JSONObject.NULL}) are written
 * as the JSON they hold.
 */
final class JsonRequestBody {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(orgJsonValues());

    private JsonRequestBody() {
    }

    /**
     * @return true when the body is a map or collection that this class serialises
     */
    static boolean isStructured(Object body) {
        return body instanceof Map || body instanceof Collection;
    }

    /**
     * @param body a map or collection request body
     * @return the body as a JSON string
     * @throws InvalidTestDataException when the body holds a value that cannot be written as JSON
     */
    static String toJson(Object body) {
        try {
            return MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new InvalidTestDataException("Request body cannot be written as JSON: " + e.getOriginalMessage(), e);
        }
    }

    private static SimpleModule orgJsonValues() {
        SimpleModule module = new SimpleModule("teswiz-org-json-values");
        module.addSerializer(JSONObject.class, new RawJsonSerializer<>());
        module.addSerializer(JSONArray.class, new RawJsonSerializer<>());
        module.addSerializer(JSONObject.NULL.getClass(), new OrgJsonNullSerializer());
        return module;
    }

    /** Writes an org.json object or array as the JSON text it already holds. */
    private static final class RawJsonSerializer<T> extends JsonSerializer<T> {
        @Override
        public void serialize(T value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeRawValue(value.toString());
        }
    }

    /** Writes org.json's {@code JSONObject.NULL} sentinel as a JSON {@code null}. */
    private static final class OrgJsonNullSerializer extends JsonSerializer<Object> {
        @Override
        public void serialize(Object value, JsonGenerator generator, SerializerProvider provider) throws IOException {
            generator.writeNull();
        }
    }
}
