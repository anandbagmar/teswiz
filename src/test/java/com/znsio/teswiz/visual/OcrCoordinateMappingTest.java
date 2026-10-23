package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.znsio.teswiz.runner.VisualElement;

/**
 * Unit tests for {@link OcrService#buildVisualElementFromMatch} - the single conversion that maps a
 * raw OCR match (physical, crop-relative screenshot pixels) back to absolute logical/viewport
 * coordinates via {@link VisualRegionScaler#toLogicalRegion}. This is the OCR counterpart to the
 * conversion already used by image matching and the LLM locate, so clicks land correctly on
 * Retina/HiDPI displays and when a region crop offset is in play.
 */
class OcrCoordinateMappingTest {

    @Test
    void unitScale_noCropOffset_returnsSameCoordinates() {
        VisualElement element = OcrService.buildVisualElementFromMatch(100, 200, 50, 40, 0, 0, 1.0, "SPIN", null);

        assertThat(element.getX()).isEqualTo(100);
        assertThat(element.getY()).isEqualTo(200);
        assertThat(element.getWidth()).isEqualTo(50);
        assertThat(element.getHeight()).isEqualTo(40);
        assertThat(element.getLabel()).isEqualTo("OCR: SPIN");
    }

    @Test
    void unitScale_withCropOffset_addsOffsetToMatch() {
        // Match at crop-relative (10,20) inside a region cropped at physical offset (300,640), scale 1.0:
        // absolute = (310, 660).
        VisualElement element = OcrService.buildVisualElementFromMatch(10, 20, 30, 25, 300, 640, 1.0, "Login", null);

        assertThat(element.getX()).isEqualTo(310);
        assertThat(element.getY()).isEqualTo(660);
        assertThat(element.getWidth()).isEqualTo(30);
        assertThat(element.getHeight()).isEqualTo(25);
    }

    @Test
    void retina2x_withCropOffset_addsPhysicalOffsetThenDividesByScale() {
        // The latent bug this guards against: on Retina the crop offset is in PHYSICAL pixels, so it
        // must be added BEFORE dividing by the scale factor - not added to an already-scaled logical
        // coordinate. Match crop-relative (40,60) -> absolute physical (658,760) -> logical /2 = (329,380).
        VisualElement element = OcrService.buildVisualElementFromMatch(40, 60, 100, 80, 618, 700, 2.0, "Route", null);

        assertThat(element.getX()).isEqualTo(329);
        assertThat(element.getY()).isEqualTo(380);
        assertThat(element.getWidth()).isEqualTo(50);
        assertThat(element.getHeight()).isEqualTo(40);
    }

    @Test
    void retina2x_noCropOffset_dividesByScale() {
        VisualElement element = OcrService.buildVisualElementFromMatch(200, 400, 60, 40, 0, 0, 2.0, "Go", null);

        assertThat(element.getX()).isEqualTo(100);
        assertThat(element.getY()).isEqualTo(200);
        assertThat(element.getWidth()).isEqualTo(30);
        assertThat(element.getHeight()).isEqualTo(20);
    }
}
