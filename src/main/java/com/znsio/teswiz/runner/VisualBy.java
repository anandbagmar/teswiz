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

    protected VisualBy(VisualByType type, String text, String imagePath, double confidenceThreshold) {
        this.type = type;
        this.text = text;
        this.imagePath = imagePath;
        this.confidenceThreshold = confidenceThreshold;
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

    public static VisualBy ocr(String text) {
        return new ByOcrText(text);
    }

    public static VisualBy image(String imagePath) {
        return new ByImageTemplate(imagePath, 0.85);
    }

    public static VisualBy image(String imagePath, double confidenceThreshold) {
        return new ByImageTemplate(imagePath, confidenceThreshold);
    }

    public static VisualBy fallbackTextOrImage(String text, String imagePath) {
        return new ByFallbackTextImage(text, imagePath, 0.85);
    }

    public static class ByOcrText extends VisualBy {
        public ByOcrText(String text) {
            super(VisualByType.OCR_TEXT, text, null, 0.85);
        }

        @Override
        public List<WebElement> findElements(SearchContext context) {
            if (context instanceof Driver) {
                return ((Driver) context).findElements(this);
            }
            throw new UnsupportedOperationException("VisualBy locators require a Driver context.");
        }

        @Override
        public String toString() {
            return "VisualBy.ocr: " + getText();
        }
    }

    public static class ByImageTemplate extends VisualBy {
        public ByImageTemplate(String imagePath, double confidenceThreshold) {
            super(VisualByType.IMAGE_TEMPLATE, null, imagePath, confidenceThreshold);
        }

        @Override
        public List<WebElement> findElements(SearchContext context) {
            if (context instanceof Driver) {
                return ((Driver) context).findElements(this);
            }
            throw new UnsupportedOperationException("VisualBy locators require a Driver context.");
        }

        @Override
        public String toString() {
            return "VisualBy.image: " + getImagePath();
        }
    }

    public static class ByFallbackTextImage extends VisualBy {
        public ByFallbackTextImage(String text, String imagePath, double confidenceThreshold) {
            super(VisualByType.FALLBACK_TEXT_IMAGE, text, imagePath, confidenceThreshold);
        }

        @Override
        public List<WebElement> findElements(SearchContext context) {
            if (context instanceof Driver) {
                return ((Driver) context).findElements(this);
            }
            throw new UnsupportedOperationException("VisualBy locators require a Driver context.");
        }

        @Override
        public String toString() {
            return "VisualBy.fallbackTextOrImage: " + getText() + " | " + getImagePath();
        }
    }
}
