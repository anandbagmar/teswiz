package com.znsio.teswiz.filters.apitraffic;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ApiCallFileNamer} — builds the per-call file name in the
 * form {@code NN-METHOD-sanitized-endpoint.log}.
 */
class ApiCallFileNamerTest {

    @Test
    void fileName_zeroPadsSingleDigitIndex() {
        String name = ApiCallFileNamer.fileName(3, "POST", "/api/v1/balance");
        assertThat(name).startsWith("03-POST-");
    }

    @Test
    void fileName_upperCasesMethod() {
        String name = ApiCallFileNamer.fileName(1, "post", "/api/v1/balance");
        assertThat(name).startsWith("01-POST-");
    }

    @Test
    void fileName_sanitizesEndpointAndDropsLeadingSlash() {
        String name = ApiCallFileNamer.fileName(1, "GET", "/api/v1/wallet/balance");
        assertThat(name).isEqualTo("01-GET-api-v1-wallet-balance.log");
    }

    @Test
    void fileName_stripsQueryString() {
        String name = ApiCallFileNamer.fileName(2, "POST", "/authenticate?token=secret&id=42");
        assertThat(name).isEqualTo("02-POST-authenticate.log");
    }

    @Test
    void fileName_endsWithLogExtension() {
        String name = ApiCallFileNamer.fileName(1, "DELETE", "/session/abc");
        assertThat(name).endsWith(".log");
    }
}
