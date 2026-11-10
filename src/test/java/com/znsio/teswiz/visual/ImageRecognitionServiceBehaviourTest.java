package com.znsio.teswiz.visual;

import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.VisualElement;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the observable behaviour of {@link ImageRecognitionService}'s template matching against a
 * generated scene. Guard cases run everywhere; matching cases need the OpenCV native library and
 * are skipped (not failed) where it cannot be loaded.
 */
class ImageRecognitionServiceBehaviourTest {
    private static final int SCENE_WIDTH = 640;
    private static final int SCENE_HEIGHT = 320;
    private static final int PATTERN_SIZE = 60;
    private static final double THRESHOLD = 0.9;
    private static final byte[] NOT_AN_IMAGE = "not an image".getBytes();
    private static final VisualRegion RIGHT_HALF = new VisualRegion(SCENE_WIDTH / 2, 0, SCENE_WIDTH / 2,
            SCENE_HEIGHT);

    @TempDir
    static Path templates;
    private static String patternTemplate;
    private static String otherTemplate;

    @BeforeAll
    static void writeTemplates() throws IOException {
        patternTemplate = write("pattern.png", pattern(Color.RED, Color.BLUE));
        otherTemplate = write("other.png", pattern(Color.GREEN, Color.MAGENTA));
    }

    // --- guards: no OpenCV needed ---

    @Test
    void unusableInputFindsNothing() {
        byte[] scene = scene(400, 150);
        assertThat(ImageRecognitionService.findTemplateMatch(null, List.of(patternTemplate), THRESHOLD, null))
                .isNull();
        assertThat(ImageRecognitionService.findTemplateMatch(scene, null, THRESHOLD, null)).isNull();
        assertThat(ImageRecognitionService.findTemplateMatch(scene, List.of(), THRESHOLD, null)).isNull();
        assertThat(ImageRecognitionService.findAllTemplateMatches(null, List.of(patternTemplate), THRESHOLD, null))
                .isEmpty();
        assertThat(ImageRecognitionService.findAllTemplateMatches(scene, List.of(), THRESHOLD, null)).isEmpty();
    }

    // --- real OpenCV ---

    @Test
    void bytesThatAreNotAnImageFindNothing() {
        assumeOpenCvLoaded();
        assertThat(ImageRecognitionService.findTemplateMatch(NOT_AN_IMAGE, List.of(patternTemplate), THRESHOLD,
                null)).isNull();
        assertThat(ImageRecognitionService.findAllTemplateMatches(NOT_AN_IMAGE, List.of(patternTemplate), THRESHOLD,
                null)).isEmpty();
    }

    @Test
    void findsTheTemplateAtItsLocationWithTheTemplateNameAsLabel() {
        assumeOpenCvLoaded();

        VisualElement match = ImageRecognitionService.findTemplateMatch(scene(400, 150), List.of(patternTemplate),
                THRESHOLD, null);

        assertThat(match).isNotNull();
        assertThat(match.getX()).isEqualTo(400);
        assertThat(match.getY()).isEqualTo(150);
        assertThat(match.getWidth()).isEqualTo(PATTERN_SIZE);
        assertThat(match.getHeight()).isEqualTo(PATTERN_SIZE);
        assertThat(match.getLabel()).isEqualTo("pattern.png");
    }

    @Test
    void aTemplateThatIsNotInTheSceneIsNotFound() {
        assumeOpenCvLoaded();

        assertThat(ImageRecognitionService.findTemplateMatch(scene(400, 150), List.of(otherTemplate), THRESHOLD,
                null)).isNull();
    }

    @Test
    void missingTemplateFilesAreSkippedAndTheNextTemplateIsTried() {
        assumeOpenCvLoaded();
        String missing = templates.resolve("missing.png").toString();

        VisualElement match = ImageRecognitionService.findTemplateMatch(scene(400, 150),
                List.of(missing, otherTemplate, patternTemplate), THRESHOLD, null);

        assertThat(match).isNotNull();
        assertThat(match.getLabel()).isEqualTo("pattern.png");
    }

    @Test
    void regionSearchReportsAbsoluteCoordinates() {
        assumeOpenCvLoaded();

        VisualElement match = ImageRecognitionService.findTemplateMatch(scene(40, 40, 400, 150),
                List.of(patternTemplate), THRESHOLD, RIGHT_HALF, null);

        assertThat(match).isNotNull();
        assertThat(match.getX()).isEqualTo(400);
        assertThat(match.getY()).isEqualTo(150);
    }

    @Test
    void findAllReturnsEachOccurrenceOnce() {
        assumeOpenCvLoaded();

        List<VisualElement> matches = ImageRecognitionService.findAllTemplateMatches(scene(40, 40, 400, 150),
                List.of(patternTemplate), THRESHOLD, null);

        // find-all reports the first pixel that clears the threshold, which can be a neighbour of the exact spot
        List<VisualElement> byX = matches.stream().sorted(Comparator.comparingInt(VisualElement::getX)).toList();
        assertThat(byX).hasSize(2);
        assertThat(byX.get(0).getX()).isCloseTo(40, within(2));
        assertThat(byX.get(0).getY()).isCloseTo(40, within(2));
        assertThat(byX.get(1).getX()).isCloseTo(400, within(2));
        assertThat(byX.get(1).getY()).isCloseTo(150, within(2));
        assertThat(matches).allSatisfy(match -> assertThat(match.getLabel()).isEqualTo("pattern.png"));
    }

    @Test
    void findAllHonoursTheRegion() {
        assumeOpenCvLoaded();

        List<VisualElement> matches = ImageRecognitionService.findAllTemplateMatches(scene(40, 40, 400, 150),
                List.of(patternTemplate), THRESHOLD, RIGHT_HALF, null);

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getX()).isCloseTo(400, within(2));
    }

    @Test
    void findAllCombinesTemplatesAndSkipsMissingFiles() {
        assumeOpenCvLoaded();
        String missing = templates.resolve("missing.png").toString();

        List<VisualElement> matches = ImageRecognitionService.findAllTemplateMatches(scene(400, 150),
                List.of(missing, otherTemplate, patternTemplate), THRESHOLD, null);

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getLabel()).isEqualTo("pattern.png");
    }

    private static void assumeOpenCvLoaded() {
        Assumptions.assumeTrue(ImageRecognitionService.isOpenCvAvailable(),
                "OpenCV native library could not be loaded; skipping template matching behaviour tests");
    }

    /** A 60x60 block of four colour quadrants with a white centre: distinctive for template matching. */
    private static BufferedImage pattern(Color first, Color second) {
        BufferedImage image = new BufferedImage(PATTERN_SIZE, PATTERN_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        int half = PATTERN_SIZE / 2;
        graphics.setColor(first);
        graphics.fillRect(0, 0, half, half);
        graphics.fillRect(half, half, half, half);
        graphics.setColor(second);
        graphics.fillRect(half, 0, half, half);
        graphics.fillRect(0, half, half, half);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(20, 20, 20, 20);
        graphics.dispose();
        return image;
    }

    private static byte[] scene(int... patternPositions) {
        BufferedImage image = new BufferedImage(SCENE_WIDTH, SCENE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.LIGHT_GRAY);
        graphics.fillRect(0, 0, SCENE_WIDTH, SCENE_HEIGHT);
        BufferedImage pattern = pattern(Color.RED, Color.BLUE);
        for (int i = 0; i < patternPositions.length; i += 2) {
            graphics.drawImage(pattern, patternPositions[i], patternPositions[i + 1], null);
        }
        graphics.dispose();
        return toPng(image);
    }

    private static String write(String fileName, BufferedImage image) throws IOException {
        Path file = templates.resolve(fileName);
        ImageIO.write(image, "png", file.toFile());
        return file.toString();
    }

    private static byte[] toPng(BufferedImage image) {
        try (ByteArrayOutputStream png = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", png);
            return png.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to encode scene image", e);
        }
    }
}
