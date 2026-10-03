package com.znsio.teswiz.tools;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.runner.Setup;

public final class ImageCanvasHighlighter {
    private static final Logger LOGGER = LogManager.getLogger(ImageCanvasHighlighter.class.getName());

    private ImageCanvasHighlighter() {
    }

    public static File annotateScreenshotFile(File screenshotFile, int x, int y, int width, int height) {
        if (screenshotFile == null || !screenshotFile.exists()) {
            return screenshotFile;
        }
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return screenshotFile;
        }
        try {
            BufferedImage image = ImageIO.read(screenshotFile);
            if (image == null) return screenshotFile;
            annotateBufferedImage(image, x, y, width, height);
            ImageIO.write(image, "png", screenshotFile);
            LOGGER.info(String.format("Annotated screenshot file '%s' on image canvas at bounds [x=%d, y=%d, w=%d, h=%d]",
                    screenshotFile.getName(), x, y, width, height));
        } catch (Exception e) {
            LOGGER.warn("Failed to annotate screenshot file on image canvas: " + e.getMessage());
        }
        return screenshotFile;
    }

    public static byte[] annotateScreenshotBytes(byte[] imageBytes, int x, int y, int width, int height) {
        if (imageBytes == null || imageBytes.length == 0) {
            return imageBytes;
        }
        if (!Setup.getBooleanValueFromConfigs(Setup.HIGHLIGHT_ELEMENTS)) {
            return imageBytes;
        }
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bais);
            if (image == null) return imageBytes;
            annotateBufferedImage(image, x, y, width, height);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            LOGGER.warn("Failed to annotate screenshot bytes on image canvas: " + e.getMessage());
            return imageBytes;
        }
    }

    public static BufferedImage annotateBufferedImage(BufferedImage image, int x, int y, int width, int height) {
        if (image == null) return null;
        String colorHex = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_COLOR, "#FF4500");
        String borderWidthStr = Setup.getStringValueFromConfigs(Setup.HIGHLIGHT_BORDER_WIDTH, "3px");
        int borderWidth = parseBorderWidth(borderWidthStr);

        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color borderColor = parseColor(colorHex, new Color(255, 69, 0));
            Color fillColor = new Color(borderColor.getRed(), borderColor.getGreen(), borderColor.getBlue(), 64);

            int safeX = Math.max(0, Math.min(x, image.getWidth() - 1));
            int safeY = Math.max(0, Math.min(y, image.getHeight() - 1));
            int safeWidth = Math.min(width, image.getWidth() - safeX);
            int safeHeight = Math.min(height, image.getHeight() - safeY);

            g2d.setColor(fillColor);
            g2d.fillRect(safeX, safeY, safeWidth, safeHeight);

            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(borderWidth));
            g2d.drawRect(safeX, safeY, safeWidth, safeHeight);
        } finally {
            g2d.dispose();
        }
        return image;
    }

    private static Color parseColor(String colorHex, Color defaultColor) {
        if (colorHex == null || colorHex.isBlank()) return defaultColor;
        try {
            if (!colorHex.startsWith("#")) colorHex = "#" + colorHex;
            return Color.decode(colorHex);
        } catch (Exception e) {
            return defaultColor;
        }
    }

    private static int parseBorderWidth(String borderWidthStr) {
        if (borderWidthStr == null || borderWidthStr.isBlank()) return 3;
        try {
            String digits = borderWidthStr.replaceAll("[^0-9]", "");
            if (!digits.isEmpty()) return Integer.parseInt(digits);
        } catch (Exception ignored) {}
        return 3;
    }
}
