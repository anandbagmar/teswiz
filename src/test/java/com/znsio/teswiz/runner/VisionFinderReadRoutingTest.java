package com.znsio.teswiz.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.entities.VisionStrategy;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
import com.znsio.teswiz.visual.ImageMatcher;
import com.znsio.teswiz.visual.LlmVisionEngine;
import com.znsio.teswiz.visual.OcrEngine;
import com.znsio.teswiz.visual.OcrReadHint;

/**
 * Unit tests for the read routing in {@link VisionFinder} (readText / readValue / readNumber) using fake
 * {@link OcrEngine} and {@link LlmVisionEngine} implementations. No native OCR or LLM call is exercised - the
 * fakes return scripted results so the per-strategy routing (OCR vs LLM vs OCR-then-LLM fallback) can be asserted
 * in isolation, mirroring {@link VisualFinderTest}.
 */
class VisionFinderReadRoutingTest {

    private static final VisualRegion REGION = VisualRegion.inRegion(10, 20, 100, 40);

    private FakeOcrEngine ocrEngine;
    private FakeLlmVisionEngine llmEngine;
    private VisualFinder visualFinder;

    @BeforeEach
    void setUp() {
        Setup.addBooleanValueToConfigs(Setup.IS_OCR_ENABLED, true);
        Setup.addBooleanValueToConfigs(Setup.LLM_VISION_FALLBACK_ENABLED, true);
        ocrEngine = new FakeOcrEngine();
        llmEngine = new FakeLlmVisionEngine();
        visualFinder = new VisualFinder(null, null, ocrEngine, new FakeImageMatcher(), llmEngine);
    }

    @AfterEach
    void tearDown() {
        Setup.addBooleanValueToConfigs(Setup.IS_OCR_ENABLED, false);
        Setup.addBooleanValueToConfigs(Setup.LLM_VISION_FALLBACK_ENABLED, false);
    }

    private VisionFinder vision(VisionStrategy strategy) {
        return new VisionFinder(strategy, visualFinder);
    }

    // --- OCR strategy -------------------------------------------------------

    @Test
    void ocrReadTextUsesOcrEngineAndIgnoresLlm() {
        ocrEngine.textToRead = "Balance 100";

        String value = vision(VisionStrategy.OCR).readText(REGION);

        assertThat(value).isEqualTo("Balance 100");
        assertThat(ocrEngine.readTextCalls).isEqualTo(1);
        assertThat(llmEngine.readCalls).isZero();
    }

    @Test
    void ocrReadTextForwardsTheHint() {
        ocrEngine.textToRead = "99";
        OcrReadHint hint = OcrReadHint.digits();

        vision(VisionStrategy.OCR).readText(REGION, hint);

        assertThat(ocrEngine.lastHint).isSameAs(hint);
    }

    @Test
    void ocrReadNumberParsesViaOcrEngine() {
        ocrEngine.numberToRead = Optional.of(new BigDecimal("1234.56"));

        Optional<BigDecimal> value = vision(VisionStrategy.OCR).readNumber(REGION);

        assertThat(value).contains(new BigDecimal("1234.56"));
        assertThat(llmEngine.readCalls).isZero();
    }

    // --- LLM strategy -------------------------------------------------------

    @Test
    void llmReadValueUsesLlmEngineWithDescription() {
        llmEngine.textToRead = "42";

        String value = vision(VisionStrategy.LLM).readValue(REGION, "the balance amount");

        assertThat(value).isEqualTo("42");
        assertThat(llmEngine.readCalls).isEqualTo(1);
        assertThat(llmEngine.lastDescription).isEqualTo("the balance amount");
        assertThat(ocrEngine.readTextCalls).isZero();
    }

    @Test
    void llmReadNumberReadsViaLlmThenParses() {
        llmEngine.textToRead = "$1,000.00";

        Optional<BigDecimal> value = vision(VisionStrategy.LLM).readNumber(REGION, "the jackpot total");

        assertThat(value).contains(new BigDecimal("1000.00"));
        assertThat(llmEngine.readCalls).isEqualTo(1);
    }

    // --- OCR_THEN_LLM strategy ---------------------------------------------

    @Test
    void ocrThenLlmReturnsOcrReadWhenPresentAndSkipsLlm() {
        ocrEngine.textToRead = "read-by-ocr";
        llmEngine.textToRead = "read-by-llm";

        String value = vision(VisionStrategy.OCR_THEN_LLM).readValue(REGION, "a value");

        assertThat(value).isEqualTo("read-by-ocr");
        assertThat(llmEngine.readCalls).isZero();
    }

    @Test
    void ocrThenLlmFallsBackToLlmWhenOcrReadBlank() {
        ocrEngine.textToRead = "   ";
        llmEngine.textToRead = "read-by-llm";

        String value = vision(VisionStrategy.OCR_THEN_LLM).readValue(REGION, "a value");

        assertThat(value).isEqualTo("read-by-llm");
        assertThat(ocrEngine.readTextCalls).isEqualTo(1);
        assertThat(llmEngine.readCalls).isEqualTo(1);
    }

    @Test
    void ocrThenLlmNumberReturnsOcrWhenParseableAndSkipsLlm() {
        ocrEngine.numberToRead = Optional.of(new BigDecimal("7"));
        llmEngine.textToRead = "999";

        Optional<BigDecimal> value = vision(VisionStrategy.OCR_THEN_LLM).readNumber(REGION, "a number");

        assertThat(value).contains(new BigDecimal("7"));
        assertThat(llmEngine.readCalls).isZero();
    }

    @Test
    void ocrThenLlmNumberFallsBackToLlmWhenOcrEmptyOrUnparseable() {
        ocrEngine.numberToRead = Optional.empty();
        llmEngine.textToRead = "250.00";

        Optional<BigDecimal> value = vision(VisionStrategy.OCR_THEN_LLM).readNumber(REGION, "a number");

        assertThat(value).contains(new BigDecimal("250.00"));
        assertThat(llmEngine.readCalls).isEqualTo(1);
    }

    @Test
    void ocrThenLlmNumberReturnsEmptyWhenBothMiss() {
        ocrEngine.numberToRead = Optional.empty();
        llmEngine.textToRead = "not-a-number";

        Optional<BigDecimal> value = vision(VisionStrategy.OCR_THEN_LLM).readNumber(REGION, "a number");

        assertThat(value).isEmpty();
    }

    // --- Subsystem gate -----------------------------------------------------

    @Test
    void ocrReadThrowsWhenOcrDisabled() {
        Setup.addBooleanValueToConfigs(Setup.IS_OCR_ENABLED, false);

        assertThatThrownBy(() -> vision(VisionStrategy.OCR).readText(REGION))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] Visual OCR & Image Recognition Subsystem is Disabled!");
    }

    @Test
    void llmReadThrowsWhenLlmFallbackDisabled() {
        Setup.addBooleanValueToConfigs(Setup.LLM_VISION_FALLBACK_ENABLED, false);

        assertThatThrownBy(() -> vision(VisionStrategy.LLM).readValue(REGION, "anything"))
                .isInstanceOf(VisualSubsystemDisabledException.class)
                .hasMessageContaining("[teswiz] LLM Vision Fallback is Disabled!");
    }

    // --- Fakes --------------------------------------------------------------

    private static final class FakeOcrEngine implements OcrEngine {
        private String textToRead = "";
        private Optional<BigDecimal> numberToRead = Optional.empty();
        private int readTextCalls;
        private OcrReadHint lastHint;

        @Override
        public VisualElement findTextMatch(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade) {
            return null;
        }

        @Override
        public List<VisualElement> findAllTextMatches(byte[] screenshotBytes, String searchText, VisualRegion region, Driver driverFacade) {
            return List.of();
        }

        @Override
        public String readText(byte[] screenshotBytes, VisualRegion region, Driver driverFacade, OcrReadHint hint) {
            readTextCalls++;
            lastHint = hint;
            return textToRead;
        }

        @Override
        public Optional<BigDecimal> readNumber(byte[] screenshotBytes, VisualRegion region, Driver driverFacade) {
            return numberToRead;
        }
    }

    private static final class FakeLlmVisionEngine implements LlmVisionEngine {
        private String textToRead = "";
        private int readCalls;
        private String lastDescription;

        @Override
        public VisualElement findByDescription(byte[] screenshotBytes, String description, VisualRegion region, Driver driverFacade) {
            return null;
        }

        @Override
        public VisualElement findByDescription(byte[] screenshotBytes, String description, List<String> referenceImagePaths,
                                               VisualRegion region, Driver driverFacade) {
            return null;
        }

        @Override
        public String readTextByDescription(byte[] screenshotBytes, String description, VisualRegion region, Driver driverFacade) {
            readCalls++;
            lastDescription = description;
            return textToRead;
        }
    }

    private static final class FakeImageMatcher implements ImageMatcher {
        @Override
        public VisualElement findTemplateMatch(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, VisualRegion region, Driver driverFacade) {
            return null;
        }

        @Override
        public List<VisualElement> findAllTemplateMatches(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, VisualRegion region, Driver driverFacade) {
            return List.of();
        }
    }
}
