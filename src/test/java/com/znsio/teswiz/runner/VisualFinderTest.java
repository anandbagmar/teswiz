package com.znsio.teswiz.runner;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.entities.SpatialDirection;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.exceptions.NoSuchVisualElementException;
import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
import com.znsio.teswiz.visual.ImageMatcher;
import com.znsio.teswiz.visual.OcrEngine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link VisualFinder} using fake {@link OcrEngine} / {@link ImageMatcher}
 * implementations. No native OCR/OpenCV dependency is exercised - the fakes return scripted
 * results so the finder's orchestration (retry, fallback, relative selection, OCR gate) can be
 * asserted in isolation. The screenshot source and driver facade are passed as {@code null}; the
 * fakes ignore both, exactly as the finder forwards them unchanged.
 */
class VisualFinderTest {

    private FakeOcrEngine ocrEngine;
    private FakeImageMatcher imageMatcher;
    private VisualFinder finder;

    @BeforeEach
    void setUp() {
        Setup.addBooleanValueToConfigs(Setup.IS_OCR_ENABLED, true);
        Setup.addIntegerValueToConfigs(Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS, 3);
        Setup.addIntegerValueToConfigs(Setup.VISUAL_ELEMENT_RETRY_DELAY_SECONDS, 1);
        ocrEngine = new FakeOcrEngine();
        imageMatcher = new FakeImageMatcher();
        finder = new VisualFinder(null, null, ocrEngine, imageMatcher);
    }

    @AfterEach
    void tearDown() {
        Setup.addBooleanValueToConfigs(Setup.IS_OCR_ENABLED, false);
    }

    @Test
    void findByTextReturnsMatchOnFirstAttempt() {
        VisualElement expected = element(10, 10, 20, 20, "Login");
        ocrEngine.textMatch = expected;

        assertThat(finder.findByText("Login")).isSameAs(expected);
        assertThat(ocrEngine.textMatchCalls).isEqualTo(1);
    }

    @Test
    void findByTextRetriesUpToConfiguredAttemptsThenThrows() {
        Setup.addIntegerValueToConfigs(Setup.VISUAL_ELEMENT_RETRY_ATTEMPTS, 3);
        ocrEngine.textMatch = null;

        assertThatThrownBy(() -> finder.findByText("Missing"))
                .isInstanceOf(NoSuchVisualElementException.class)
                .hasMessageContaining("Missing");
        assertThat(ocrEngine.textMatchCalls).isEqualTo(3);
    }

    @Test
    void findByImageDelegatesToImageMatcher() {
        VisualElement expected = element(5, 5, 30, 30, "logo.png");
        imageMatcher.templateMatch = expected;

        assertThat(finder.findByImage(List.of("logo.png"))).isSameAs(expected);
        assertThat(imageMatcher.templateMatchCalls).isEqualTo(1);
    }

    @Test
    void findByTextOrImageFallsBackToImageWhenTextMissing() {
        ocrEngine.textMatch = null;
        VisualElement imageHit = element(1, 2, 3, 4, "fallback.png");
        imageMatcher.templateMatch = imageHit;

        assertThat(finder.findByTextOrImage("Login", List.of("fallback.png"))).isSameAs(imageHit);
        assertThat(imageMatcher.templateMatchCalls).isEqualTo(1);
    }

    @Test
    void findByImageOrTextFallsBackToTextWhenImageMissing() {
        imageMatcher.templateMatch = null;
        VisualElement textHit = element(7, 8, 9, 10, "Login");
        ocrEngine.textMatch = textHit;

        assertThat(finder.findByImageOrText(List.of("logo.png"), "Login")).isSameAs(textHit);
    }

    @Test
    void findAllByTextOrImageReturnsTextMatchesWhenPresent() {
        ocrEngine.allTextMatches = List.of(element(0, 0, 1, 1, "A"));
        imageMatcher.allTemplateMatches = List.of(element(0, 0, 1, 1, "B"));

        List<VisualElement> result = finder.findAllByTextOrImage("A", List.of("b.png"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLabel()).isEqualTo("A");
        assertThat(imageMatcher.allTemplateMatchCalls).isZero();
    }

    @Test
    void findRelativeByTextPicksClosestCandidateInDirection() {
        VisualElement anchor = element(100, 100, 10, 10, "anchor");
        VisualElement below = element(100, 200, 10, 10, "below");
        VisualElement above = element(100, 20, 10, 10, "above");
        ocrEngine.textMatch = anchor;
        ocrEngine.allTextMatches = new ArrayList<>(List.of(below, above));

        VisualElement result = finder.findRelativeByText("target", SpatialDirection.BELOW, "anchor");

        assertThat(result.getLabel()).isEqualTo("below");
    }

    @Test
    void findRelativeByTextThrowsWhenNoCandidateMatchesDirection() {
        VisualElement anchor = element(100, 100, 10, 10, "anchor");
        VisualElement above = element(100, 20, 10, 10, "above");
        ocrEngine.textMatch = anchor;
        ocrEngine.allTextMatches = new ArrayList<>(List.of(above));

        assertThatThrownBy(() -> finder.findRelativeByText("target", SpatialDirection.BELOW, "anchor"))
                .isInstanceOf(NoSuchVisualElementException.class);
    }

    @Test
    void verifyOcrEnabledGateThrowsWhenDisabled() {
        Setup.addBooleanValueToConfigs(Setup.IS_OCR_ENABLED, false);

        assertThatThrownBy(() -> finder.findByText("Login"))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
    }

    private static VisualElement element(int x, int y, int w, int h, String label) {
        return new VisualElement(x, y, w, h, label, null);
    }

    private static final class FakeOcrEngine implements OcrEngine {
        private VisualElement textMatch;
        private List<VisualElement> allTextMatches = List.of();
        private int textMatchCalls;

        @Override
        public VisualElement findTextMatch(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade) {
            textMatchCalls++;
            return textMatch;
        }

        @Override
        public List<VisualElement> findAllTextMatches(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade) {
            return allTextMatches;
        }
    }

    private static final class FakeImageMatcher implements ImageMatcher {
        private VisualElement templateMatch;
        private List<VisualElement> allTemplateMatches = List.of();
        private int templateMatchCalls;
        private int allTemplateMatchCalls;

        @Override
        public VisualElement findTemplateMatch(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, VisualRegion region, Driver driverFacade) {
            templateMatchCalls++;
            return templateMatch;
        }

        @Override
        public List<VisualElement> findAllTemplateMatches(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, VisualRegion region, Driver driverFacade) {
            allTemplateMatchCalls++;
            return allTemplateMatches;
        }
    }
}
