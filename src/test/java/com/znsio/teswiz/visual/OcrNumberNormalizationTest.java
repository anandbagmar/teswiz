package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OcrService#normalizeNumber(String)} - the normalisation/parsing that turns an OCR'd
 * numeric string (plain, grouped with thousands separators, currency-prefixed, or garbage) into a
 * {@link BigDecimal}, returning {@link Optional#empty()} rather than throwing for anything non-numeric.
 */
class OcrNumberNormalizationTest {

    @Test
    void parsesPlainInteger() {
        assertThat(OcrService.normalizeNumber("1234")).contains(new BigDecimal("1234"));
    }

    @Test
    void parsesPlainDecimal() {
        assertThat(OcrService.normalizeNumber("12.50")).contains(new BigDecimal("12.50"));
    }

    @Test
    void parsesGroupedThousandsWithDecimal() {
        assertThat(OcrService.normalizeNumber("1,234.56")).contains(new BigDecimal("1234.56"));
    }

    @Test
    void parsesMultipleThousandsGroups() {
        assertThat(OcrService.normalizeNumber("1,000,000")).contains(new BigDecimal("1000000"));
    }

    @Test
    void stripsCurrencyPrefixAndWhitespace() {
        assertThat(OcrService.normalizeNumber("  $1,234.56 ")).contains(new BigDecimal("1234.56"));
        assertThat(OcrService.normalizeNumber("\u20ac99.99")).contains(new BigDecimal("99.99"));
    }

    @Test
    void stripsCurrencySuffix() {
        assertThat(OcrService.normalizeNumber("250.00 USD")).contains(new BigDecimal("250.00"));
    }

    @Test
    void parsesNegativeValue() {
        assertThat(OcrService.normalizeNumber("-42.5")).contains(new BigDecimal("-42.5"));
    }

    @Test
    void blankOrNullReadReturnsEmpty() {
        assertThat(OcrService.normalizeNumber(null)).isEmpty();
        assertThat(OcrService.normalizeNumber("")).isEmpty();
        assertThat(OcrService.normalizeNumber("   ")).isEmpty();
    }

    @Test
    void garbageReadReturnsEmpty() {
        assertThat(OcrService.normalizeNumber("abc")).isEmpty();
        assertThat(OcrService.normalizeNumber("SPIN")).isEmpty();
    }

    @Test
    void loneSeparatorsReturnEmpty() {
        assertThat(OcrService.normalizeNumber(".")).isEmpty();
        assertThat(OcrService.normalizeNumber("-")).isEmpty();
        assertThat(OcrService.normalizeNumber(",")).isEmpty();
    }
}
