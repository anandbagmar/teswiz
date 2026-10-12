package com.znsio.teswiz.runner;

import java.io.Serializable;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

public abstract class VisualBy extends By implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum VisualByType {
        OCR_TEXT,
        IMAGE_TEMPLATE,
        FALLBACK_TEXT_IMAGE
    }

    private final VisualByType type;
    private final String text;
    private final String imagePath;
    private final double confidenceThreshold;
    private final com.znsio.teswiz.entities.VisualRegion region;

    protected VisualBy(VisualByType type, String text, String imagePath, double confidenceThreshold, com.znsio.teswiz.entities.VisualRegion region) {
        this.type = type;
        this.text = text;
        this.imagePath = imagePath;
        this.confidenceThreshold = confidenceThreshold;
        this.region = region;
    }

    protected VisualBy(VisualByType type, String text, String imagePath, double confidenceThreshold) {
        this(type, text, imagePath, confidenceThreshold, null);
    }

    public VisualByType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public String getImagePath() {
        return imagePath;
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }

    public com.znsio.teswiz.entities.VisualRegion getRegion() {
        return region;
    }

    @Override
    public List<WebElement> findElements(SearchContext context) {
        if (context instanceof Driver) {
            return ((Driver) context).findElements(this);
        }
        throw new UnsupportedOperationException("VisualBy locators require a Driver context.");
    }

    public static VisualBy ocr(String text) {
        return new ByOcrText(text, null);
    }

    public static VisualBy ocr(String text, com.znsio.teswiz.entities.VisualRegion region) {
        return new ByOcrText(text, region);
    }

    public static VisualBy image(String imagePath) {
        return new ByImageTemplate(imagePath, 0.85, null);
    }

    public static VisualBy image(String imagePath, double confidenceThreshold) {
        return new ByImageTemplate(imagePath, confidenceThreshold, null);
    }

    public static VisualBy image(String imagePath, com.znsio.teswiz.entities.VisualRegion region) {
        return new ByImageTemplate(imagePath, 0.85, region);
    }

    public static VisualBy image(String imagePath, double confidenceThreshold, com.znsio.teswiz.entities.VisualRegion region) {
        return new ByImageTemplate(imagePath, confidenceThreshold, region);
    }

    public static VisualBy fallbackTextOrImage(String text, String imagePath) {
        return new ByFallbackTextImage(text, imagePath, 0.85, null);
    }

    public static VisualBy fallbackTextOrImage(String text, String imagePath, com.znsio.teswiz.entities.VisualRegion region) {
        return new ByFallbackTextImage(text, imagePath, 0.85, region);
    }

    public static class ByOcrText extends VisualBy {
        public ByOcrText(String text) {
            this(text, null);
        }

        public ByOcrText(String text, com.znsio.teswiz.entities.VisualRegion region) {
            super(VisualByType.OCR_TEXT, text, null, 0.85, region);
        }

        @Override
        public String toString() {
            return "VisualBy.ocr: " + getText() + (getRegion() != null ? " in " + getRegion() : "");
        }
    }

    public static class ByImageTemplate extends VisualBy {
        public ByImageTemplate(String imagePath, double confidenceThreshold) {
            this(imagePath, confidenceThreshold, null);
        }

        public ByImageTemplate(String imagePath, double confidenceThreshold, com.znsio.teswiz.entities.VisualRegion region) {
            super(VisualByType.IMAGE_TEMPLATE, null, imagePath, confidenceThreshold, region);
        }

        @Override
        public String toString() {
            return "VisualBy.image: " + getImagePath() + (getRegion() != null ? " in " + getRegion() : "");
        }
    }

    public static class ByFallbackTextImage extends VisualBy {
        public ByFallbackTextImage(String text, String imagePath, double confidenceThreshold) {
            this(text, imagePath, confidenceThreshold, null);
        }

        public ByFallbackTextImage(String text, String imagePath, double confidenceThreshold, com.znsio.teswiz.entities.VisualRegion region) {
            super(VisualByType.FALLBACK_TEXT_IMAGE, text, imagePath, confidenceThreshold, region);
        }

        @Override
        public String toString() {
            return "VisualBy.fallbackTextOrImage: " + getText() + " | " + getImagePath() + (getRegion() != null ? " in " + getRegion() : "");
        }
    }
}
