package com.znsio.teswiz.visual;

import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
import com.znsio.teswiz.runner.VisualElement;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the observable behaviour of {@link OcrService}'s public finders and readers against rendered
 * text. Guard cases run everywhere; the OCR cases need the native Tesseract library and English
 * language data, and are skipped (not failed) on a machine without them.
 */
class OcrServiceBehaviourTest {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 200;
    private static final byte[] NOT_AN_IMAGE = "not an image".getBytes();
    private static final VisualRegion RIGHT_HALF = new VisualRegion(WIDTH / 2, 0, WIDTH / 2, HEIGHT);

    // --- guards: no OCR needed ---

    @Test
    void findTextMatchReturnsNullForUnusableInput() {
        byte[] image = render("LOGIN", 40);
        assertThat(OcrService.findTextMatch(null, "LOGIN", null)).isNull();
        assertThat(OcrService.findTextMatch(image, null, null)).isNull();
        assertThat(OcrService.findTextMatch(image, "  ", null)).isNull();
        assertThat(OcrService.findTextMatch(NOT_AN_IMAGE, "LOGIN", null)).isNull();
    }

    @Test
    void findAllTextMatchesReturnsEmptyForUnusableInput() {
        byte[] image = render("LOGIN", 40);
        assertThat(OcrService.findAllTextMatches(null, "LOGIN", null)).isEmpty();
        assertThat(OcrService.findAllTextMatches(image, "", null)).isEmpty();
        assertThat(OcrService.findAllTextMatches(NOT_AN_IMAGE, "LOGIN", null)).isEmpty();
    }

    @Test
    void readersReturnEmptyForUnusableInput() {
        assertThat(OcrService.readText(null, null, null)).isEmpty();
        assertThat(OcrService.readText(NOT_AN_IMAGE, null, null)).isEmpty();
        assertThat(OcrService.readNumber(null, null, null)).isEmpty();
    }

    // --- real OCR ---

    @Test
    void findsAnExactWordAndReportsItsLocation() {
        assumeTesseractInstalled();

        VisualElement match = OcrService.findTextMatch(render("LOGIN", 40, "SPIN", 560), "LOGIN", null);

        assertThat(match).isNotNull();
        assertThat(match.getLabel()).isEqualTo("OCR: LOGIN");
        assertThat(match.getX()).isBetween(20, 140);
        assertThat(match.getY()).isBetween(30, 130);
        assertThat(match.getWidth()).isPositive();
    }

    @Test
    void matchingIsCaseInsensitive() {
        assumeTesseractInstalled();

        VisualElement match = OcrService.findTextMatch(render("LOGIN", 40, "SPIN", 560), "spin", null);

        assertThat(match).isNotNull();
        assertThat(match.getLabel()).isEqualTo("OCR: SPIN");
        assertThat(match.getX()).isGreaterThanOrEqualTo(500);
    }

    @Test
    void multiWordSearchMatchesTheWordSequenceAndUsesTheSearchTextAsLabel() {
        assumeTesseractInstalled();

        VisualElement match = OcrService.findTextMatch(render("LOGIN NOW", 40), "login now", null);

        assertThat(match).isNotNull();
        assertThat(match.getLabel()).isEqualTo("OCR: login now");
        assertThat(match.getX()).isBetween(20, 140);
    }

    @Test
    void regionSearchOnlyLooksInsideTheRegionAndReportsAbsoluteCoordinates() {
        assumeTesseractInstalled();

        VisualElement match = OcrService.findTextMatch(render("SPIN", 40, "SPIN", 560), "SPIN", RIGHT_HALF, null);

        assertThat(match).isNotNull();
        assertThat(match.getX()).isGreaterThanOrEqualTo(500);
    }

    @Test
    void absentTextIsNotFound() {
        assumeTesseractInstalled();

        assertThat(OcrService.findTextMatch(render("LOGIN", 40), "ZEBRA", null)).isNull();
    }

    @Test
    void findAllTextMatchesReturnsEveryOccurrenceOnce() {
        assumeTesseractInstalled();

        List<VisualElement> matches = OcrService.findAllTextMatches(render("SPIN", 40, "SPIN", 560), "SPIN", null);

        assertThat(matches).hasSize(2);
        List<VisualElement> byX = matches.stream().sorted(Comparator.comparingInt(VisualElement::getX)).toList();
        assertThat(byX.get(0).getX()).isLessThan(200);
        assertThat(byX.get(1).getX()).isGreaterThanOrEqualTo(500);
        assertThat(matches).allSatisfy(match -> assertThat(match.getLabel()).isEqualTo("OCR: SPIN"));
    }

    @Test
    void findAllTextMatchesHonoursTheRegion() {
        assumeTesseractInstalled();

        List<VisualElement> matches = OcrService.findAllTextMatches(render("SPIN", 40, "SPIN", 560), "SPIN",
                RIGHT_HALF, null);

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getX()).isGreaterThanOrEqualTo(500);
    }

    @Test
    void readTextReturnsTheRecognisedText() {
        assumeTesseractInstalled();

        assertThat(OcrService.readText(render("LOGIN", 40), null, null)).containsIgnoringCase("LOGIN");
    }

    @Test
    void readTextWithinARegionReadsOnlyThatRegion() {
        assumeTesseractInstalled();

        String text = OcrService.readText(render("LOGIN", 40, "SPIN", 560), RIGHT_HALF, null);

        assertThat(text).containsIgnoringCase("SPIN").doesNotContainIgnoringCase("LOGIN");
    }

    @Test
    void readNumberParsesARenderedAmount() {
        assumeTesseractInstalled();

        assertThat(OcrService.readNumber(render("1,234.56", 40), null, null)).contains(new BigDecimal("1234.56"));
    }

    private static void assumeTesseractInstalled() {
        String probe;
        try {
            probe = OcrService.readText(render("LOGIN", 40), null, null);
        } catch (VisualSubsystemDisabledException e) {
            probe = "";
        }
        Assumptions.assumeTrue(probe.toUpperCase().contains("LOGIN"),
                "Native Tesseract with English language data is not installed; skipping OCR behaviour tests");
    }

    private static byte[] render(String text, int x) {
        return render(text, x, null, 0);
    }

    /** Renders up to two black words on a white canvas, large and anti-aliased so OCR is deterministic. */
    private static byte[] render(String firstText, int firstX, String secondText, int secondX) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setColor(Color.BLACK);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
        graphics.drawString(firstText, firstX, 120);
        if (null != secondText) {
            graphics.drawString(secondText, secondX, 120);
        }
        graphics.dispose();
        try (ByteArrayOutputStream png = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", png);
            return png.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to render OCR test image", e);
        }
    }
}
