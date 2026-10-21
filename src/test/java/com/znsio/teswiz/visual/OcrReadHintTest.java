package com.znsio.teswiz.visual;

import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_RAW_LINE;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SINGLE_BLOCK;
import static net.sourceforge.tess4j.ITessAPI.TessPageSegMode.PSM_SINGLE_LINE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link OcrReadHint}: the factory defaults, the digits preset, immutability of the fluent
 * {@code with*} copies, the page-segmentation to Tesseract-mode mapping, and the upscale-factor guard.
 */
class OcrReadHintTest {

    @Test
    void defaultsUseSingleBlockDefaultUpscaleAndNoWhitelist() {
        OcrReadHint hint = OcrReadHint.defaults();
        assertThat(hint.getPageSegmentation()).isEqualTo(OcrReadHint.PageSegmentation.SINGLE_BLOCK);
        assertThat(hint.getUpscaleFactor()).isEqualTo(OcrReadHint.DEFAULT_UPSCALE_FACTOR);
        assertThat(hint.hasCharacterWhitelist()).isFalse();
        assertThat(hint.getCharacterWhitelist()).isNull();
    }

    @Test
    void digitsUseSingleLineAndNumericWhitelist() {
        OcrReadHint hint = OcrReadHint.digits();
        assertThat(hint.getPageSegmentation()).isEqualTo(OcrReadHint.PageSegmentation.SINGLE_LINE);
        assertThat(hint.hasCharacterWhitelist()).isTrue();
        assertThat(hint.getCharacterWhitelist()).isEqualTo(OcrReadHint.NUMERIC_WHITELIST);
    }

    @Test
    void singleLineHasNoWhitelist() {
        OcrReadHint hint = OcrReadHint.singleLine();
        assertThat(hint.getPageSegmentation()).isEqualTo(OcrReadHint.PageSegmentation.SINGLE_LINE);
        assertThat(hint.hasCharacterWhitelist()).isFalse();
    }

    @Test
    void withCopiesAreIndependentOfTheOriginal() {
        OcrReadHint base = OcrReadHint.defaults();
        OcrReadHint changed = base
                .withPageSegmentation(OcrReadHint.PageSegmentation.RAW_LINE)
                .withUpscaleFactor(5.0)
                .withCharacterWhitelist("ABC");

        // original is untouched
        assertThat(base.getPageSegmentation()).isEqualTo(OcrReadHint.PageSegmentation.SINGLE_BLOCK);
        assertThat(base.getUpscaleFactor()).isEqualTo(OcrReadHint.DEFAULT_UPSCALE_FACTOR);
        assertThat(base.hasCharacterWhitelist()).isFalse();

        // copy carries the new values
        assertThat(changed.getPageSegmentation()).isEqualTo(OcrReadHint.PageSegmentation.RAW_LINE);
        assertThat(changed.getUpscaleFactor()).isEqualTo(5.0);
        assertThat(changed.getCharacterWhitelist()).isEqualTo("ABC");
    }

    @Test
    void nonPositiveUpscaleFactorFallsBackToDefault() {
        assertThat(OcrReadHint.defaults().withUpscaleFactor(0).getUpscaleFactor()).isEqualTo(OcrReadHint.DEFAULT_UPSCALE_FACTOR);
        assertThat(OcrReadHint.defaults().withUpscaleFactor(-2).getUpscaleFactor()).isEqualTo(OcrReadHint.DEFAULT_UPSCALE_FACTOR);
    }

    @Test
    void blankWhitelistIsTreatedAsNoWhitelist() {
        OcrReadHint hint = OcrReadHint.digits().withCharacterWhitelist("   ");
        assertThat(hint.hasCharacterWhitelist()).isFalse();
        assertThat(hint.getCharacterWhitelist()).isNull();
    }

    @Test
    void pageSegmentationMapsToExpectedTesseractModes() {
        assertThat(OcrReadHint.PageSegmentation.SINGLE_BLOCK.tesseractMode()).isEqualTo(PSM_SINGLE_BLOCK);
        assertThat(OcrReadHint.PageSegmentation.SINGLE_LINE.tesseractMode()).isEqualTo(PSM_SINGLE_LINE);
        assertThat(OcrReadHint.PageSegmentation.RAW_LINE.tesseractMode()).isEqualTo(PSM_RAW_LINE);
    }
}
