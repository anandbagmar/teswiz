package com.znsio.teswiz.visual;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.filters.apitraffic.ScenarioDirectoryResolver;
import com.znsio.teswiz.filters.apitraffic.TeswizScenarioDirectoryResolver;

/**
 * Writes the region located by the LLM vision engine as a cropped PNG into a dedicated
 * {@code llm-vision-captures} sub-folder of the current scenario's report directory.
 *
 * <p>The crop is taken from the raw screenshot at the raw pixel bounding box, so the saved file is a
 * ready-to-use OpenCV template: a user can review it and, if good, promote it into their own image
 * template library and switch that locator to {@code VisionStrategy.OCR_THEN_LLM} to avoid paying
 * for the LLM call on future runs. Promotion is always a manual, human-reviewed step - this writer
 * only harvests and reports; it never auto-reuses captures.
 */
public final class LlmVisionCaptureWriter {

    private static final Logger LOGGER = LogManager.getLogger(LlmVisionCaptureWriter.class.getName());
    private static final String CAPTURE_DIR = "llm-vision-captures";
    private static final String LOG_DIR_PROPERTY = "LOG_DIR";
    private static final String FALLBACK_NAME = "llm-capture.png";

    private final ScenarioDirectoryResolver scenarioDirectoryResolver;

    public LlmVisionCaptureWriter() {
        this(new TeswizScenarioDirectoryResolver());
    }

    public LlmVisionCaptureWriter(ScenarioDirectoryResolver scenarioDirectoryResolver) {
        this.scenarioDirectoryResolver = scenarioDirectoryResolver;
    }

    /**
     * Derives the suggested image-template file name from a natural-language description. This is a
     * suggestion only - the user chooses the final name and location when promoting a capture.
     */
    public static String suggestedFileName(String description) {
        if (description == null) {
            return FALLBACK_NAME;
        }
        String sanitized = description.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return sanitized.isEmpty() ? FALLBACK_NAME : sanitized + ".png";
    }

    /**
     * Crops {@code source} to the given raw pixel box (clamped to image bounds) and writes it as an
     * indexed PNG under the scenario's {@code llm-vision-captures} folder.
     *
     * @return a {@link CaptureResult} with the saved file path and the suggested library file name
     */
    public CaptureResult saveCapture(BufferedImage source, int x, int y, int width, int height,
                                     String description, int index) {
        int safeX = Math.max(0, Math.min(x, source.getWidth() - 1));
        int safeY = Math.max(0, Math.min(y, source.getHeight() - 1));
        int safeW = Math.max(1, Math.min(width, source.getWidth() - safeX));
        int safeH = Math.max(1, Math.min(height, source.getHeight() - safeY));

        BufferedImage crop = source.getSubimage(safeX, safeY, safeW, safeH);

        String suggestedName = suggestedFileName(description);
        String fileName = String.format("%02d-%s", index, suggestedName);
        Path dir = scenarioDirectoryResolver.resolve().resolve(CAPTURE_DIR);
        Path file = dir.resolve(fileName);

        writePng(crop, file);
        return new CaptureResult(relativeToLogDir(file), suggestedName);
    }

    private void writePng(BufferedImage image, Path file) {
        try {
            Files.createDirectories(file.getParent());
            ImageIO.write(image, "png", file.toFile());
        } catch (IOException e) {
            LOGGER.warn("Failed to write llm-vision-capture file {}: {}", file, e.getMessage());
            throw new UncheckedIOException("Unable to write llm-vision-capture file: " + file, e);
        }
    }

    private String relativeToLogDir(Path file) {
        String logDir = System.getProperty(LOG_DIR_PROPERTY);
        if (logDir == null || logDir.isBlank()) {
            return file.toString();
        }
        Path logDirPath = Path.of(logDir).toAbsolutePath().normalize();
        Path absoluteFile = file.toAbsolutePath().normalize();
        return absoluteFile.startsWith(logDirPath)
                ? logDirPath.relativize(absoluteFile).toString()
                : absoluteFile.toString();
    }

    /**
     * Outcome of a saved capture: the saved file path (relative to {@code LOG_DIR} when possible) and
     * the suggested image-template file name for manual promotion.
     */
    public record CaptureResult(String savedPath, String suggestedFileName) {
    }
}
