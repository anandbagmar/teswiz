package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LlmVisionCaptureWriterTest {

    @Test
    void sanitizeToFileName_lowercasesAndHyphenatesAndAppendsPngExtension() {
        assertThat(LlmVisionCaptureWriter.suggestedFileName("Submit Button")).isEqualTo("submit-button.png");
    }

    @Test
    void sanitizeToFileName_collapsesNonAlphanumericRunsAndTrimsEdges() {
        assertThat(LlmVisionCaptureWriter.suggestedFileName("  the blue 'Login' button!!  "))
                .isEqualTo("the-blue-login-button.png");
    }

    @Test
    void sanitizeToFileName_fallsBackForEmptyOrSymbolOnlyDescription() {
        assertThat(LlmVisionCaptureWriter.suggestedFileName("***")).isEqualTo("llm-capture.png");
        assertThat(LlmVisionCaptureWriter.suggestedFileName("")).isEqualTo("llm-capture.png");
    }

    @Test
    void saveCapture_cropsToTheBoxAndWritesAnIndexedPngInTheCaptureFolder(@TempDir Path scenarioDir) throws Exception {
        BufferedImage source = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = source.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, 100, 100);
        g.setColor(Color.GREEN);
        g.fillRect(20, 30, 40, 25); // the "element" region we expect cropped out
        g.dispose();

        LlmVisionCaptureWriter writer = new LlmVisionCaptureWriter(() -> scenarioDir);

        LlmVisionCaptureWriter.CaptureResult result =
                writer.saveCapture(source, 20, 30, 40, 25, "Login Button", 1);

        Path saved = scenarioDir.resolve("llm-vision-captures").resolve("01-login-button.png");
        assertThat(Files.exists(saved)).isTrue();
        assertThat(result.suggestedFileName()).isEqualTo("login-button.png");
        assertThat(result.savedPath()).contains("llm-vision-captures");

        BufferedImage crop = ImageIO.read(saved.toFile());
        assertThat(crop.getWidth()).isEqualTo(40);
        assertThat(crop.getHeight()).isEqualTo(25);
        // the crop is the green element region
        assertThat(new Color(crop.getRGB(1, 1)).getGreen()).isGreaterThan(200);
    }

    @Test
    void saveCapture_clampsBoxToImageBoundsWithoutThrowing(@TempDir Path scenarioDir) throws Exception {
        BufferedImage source = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        LlmVisionCaptureWriter writer = new LlmVisionCaptureWriter(() -> scenarioDir);

        // box extends beyond the image; must be clamped, not throw
        LlmVisionCaptureWriter.CaptureResult result =
                writer.saveCapture(source, 40, 40, 999, 999, "Edge Case", 2);

        assertThat(result).isNotNull();
        assertThat(Files.exists(scenarioDir.resolve("llm-vision-captures").resolve("02-edge-case.png"))).isTrue();
    }
}
