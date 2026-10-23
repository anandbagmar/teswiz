package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import com.znsio.teswiz.entities.VisualRegion;

import org.junit.jupiter.api.Test;

class VisualRegionScalerTest {

    // --- scaleRegionToPixels: logical (CSS/viewport) region -> physical screenshot pixels ---------

    @Test
    void scaleRegion_withUnitScale_returnsSameCoordinates() {
        // scaleFactor 1.0 (non-Retina / external monitor): logical == physical.
        VisualRegion logical = VisualRegion.inRegion(369, 674, 261, 104);

        VisualRegionScaler.PixelRegion pixels = VisualRegionScaler.scaleRegionToPixels(logical, 1920, 1080, 1.0);

        assertThat(pixels.x()).isEqualTo(369);
        assertThat(pixels.y()).isEqualTo(674);
        assertThat(pixels.width()).isEqualTo(261);
        assertThat(pixels.height()).isEqualTo(104);
    }

    @Test
    void scaleRegion_withRetina2xScale_multipliesByScaleFactor() {
        // scaleFactor 2.0 (Retina): a logical region maps to a 2x physical-pixel region.
        VisualRegion logical = VisualRegion.inRegion(309, 653, 261, 104);

        VisualRegionScaler.PixelRegion pixels = VisualRegionScaler.scaleRegionToPixels(logical, 3600, 2086, 2.0);

        assertThat(pixels.x()).isEqualTo(618);
        assertThat(pixels.y()).isEqualTo(1306);
        assertThat(pixels.width()).isEqualTo(522);
        assertThat(pixels.height()).isEqualTo(208);
    }

    @Test
    void scaleRegion_clampsToImageBoundsWhenScaledRegionOverflows() {
        // A region near the bottom-right whose scaled size would exceed the image is clamped to fit.
        VisualRegion logical = VisualRegion.inRegion(950, 530, 100, 100);

        VisualRegionScaler.PixelRegion pixels = VisualRegionScaler.scaleRegionToPixels(logical, 1920, 1080, 2.0);

        assertThat(pixels.x()).isEqualTo(1900);
        assertThat(pixels.y()).isEqualTo(1060);
        assertThat(pixels.width()).isEqualTo(1920 - 1900);
        assertThat(pixels.height()).isEqualTo(1080 - 1060);
    }

    @Test
    void scaleRegion_nullRegionReturnsFullImage() {
        VisualRegionScaler.PixelRegion pixels = VisualRegionScaler.scaleRegionToPixels(null, 1920, 1080, 2.0);

        assertThat(pixels.x()).isZero();
        assertThat(pixels.y()).isZero();
        assertThat(pixels.width()).isEqualTo(1920);
        assertThat(pixels.height()).isEqualTo(1080);
    }

    @Test
    void scaleRegion_nonPositiveScaleFactorFallsBackToUnitScale() {
        VisualRegion logical = VisualRegion.inRegion(10, 20, 30, 40);

        VisualRegionScaler.PixelRegion pixels = VisualRegionScaler.scaleRegionToPixels(logical, 1920, 1080, 0.0);

        assertThat(pixels.x()).isEqualTo(10);
        assertThat(pixels.y()).isEqualTo(20);
        assertThat(pixels.width()).isEqualTo(30);
        assertThat(pixels.height()).isEqualTo(40);
    }

    // --- clampBoxToImage: accept an edge-touching / slightly-oversized located box ----------------

    @Test
    void clampBox_acceptsBoxFlushToRightAndBottomEdge() {
        // The real failure: LLM returned [23,24,239,58] for a 261x104 crop (23+239=262, 1px over).
        VisualRegionScaler.PixelRegion clamped = VisualRegionScaler.clampBoxToImage(23, 24, 239, 58, 261, 104);

        assertThat(clamped).isNotNull();
        assertThat(clamped.x()).isEqualTo(23);
        assertThat(clamped.y()).isEqualTo(24);
        assertThat(clamped.width()).isEqualTo(261 - 23); // clamped to the image edge
        assertThat(clamped.height()).isEqualTo(58);
    }

    @Test
    void clampBox_leavesInBoundsBoxUnchanged() {
        VisualRegionScaler.PixelRegion clamped = VisualRegionScaler.clampBoxToImage(10, 10, 50, 40, 261, 104);

        assertThat(clamped.x()).isEqualTo(10);
        assertThat(clamped.y()).isEqualTo(10);
        assertThat(clamped.width()).isEqualTo(50);
        assertThat(clamped.height()).isEqualTo(40);
    }

    @Test
    void clampBox_rejectsBoxWithOriginOutsideImage() {
        assertThat(VisualRegionScaler.clampBoxToImage(300, 10, 20, 20, 261, 104)).isNull();
        assertThat(VisualRegionScaler.clampBoxToImage(10, 200, 20, 20, 261, 104)).isNull();
    }

    @Test
    void clampBox_rejectsNonPositiveSize() {
        assertThat(VisualRegionScaler.clampBoxToImage(10, 10, 0, 20, 261, 104)).isNull();
        assertThat(VisualRegionScaler.clampBoxToImage(10, 10, 20, -5, 261, 104)).isNull();
    }

    @Test
    void clampBox_rejectsNegativeOrigin() {
        assertThat(VisualRegionScaler.clampBoxToImage(-1, 10, 20, 20, 261, 104)).isNull();
    }

    // --- toLogicalRegion: physical crop-relative match -> absolute logical/viewport coordinates ----

    @Test
    void toLogical_unitScale_noOffset_returnsSameCoordinates() {
        VisualRegionScaler.PixelRegion logical = VisualRegionScaler.toLogicalRegion(100, 200, 50, 40, 0, 0, 1.0);

        assertThat(logical.x()).isEqualTo(100);
        assertThat(logical.y()).isEqualTo(200);
        assertThat(logical.width()).isEqualTo(50);
        assertThat(logical.height()).isEqualTo(40);
    }

    @Test
    void toLogical_addsCropOffsetBeforeScaling() {
        // match at crop-relative (10,20) inside a region cropped at physical offset (300,640), unit scale:
        // absolute physical = (310, 660); logical (scale 1.0) = (310, 660).
        VisualRegionScaler.PixelRegion logical = VisualRegionScaler.toLogicalRegion(10, 20, 30, 25, 300, 640, 1.0);

        assertThat(logical.x()).isEqualTo(310);
        assertThat(logical.y()).isEqualTo(660);
        assertThat(logical.width()).isEqualTo(30);
        assertThat(logical.height()).isEqualTo(25);
    }

    @Test
    void toLogical_retina2x_addsOffsetThenDividesByScale() {
        // Retina: crop offset is in physical px (e.g. 618 = 309 logical * 2). A match at crop-relative
        // (40,60) -> absolute physical (658,760) -> logical (/2) = (329, 380).
        VisualRegionScaler.PixelRegion logical = VisualRegionScaler.toLogicalRegion(40, 60, 100, 80, 618, 700, 2.0);

        assertThat(logical.x()).isEqualTo(329);
        assertThat(logical.y()).isEqualTo(380);
        assertThat(logical.width()).isEqualTo(50);
        assertThat(logical.height()).isEqualTo(40);
    }

    @Test
    void toLogical_nonPositiveScaleFallsBackToUnit() {
        VisualRegionScaler.PixelRegion logical = VisualRegionScaler.toLogicalRegion(10, 10, 20, 20, 0, 0, 0.0);

        assertThat(logical.x()).isEqualTo(10);
        assertThat(logical.y()).isEqualTo(10);
    }
}
