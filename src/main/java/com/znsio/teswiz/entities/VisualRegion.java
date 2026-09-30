package com.znsio.teswiz.entities;

import com.znsio.teswiz.runner.VisualElement;
import org.openqa.selenium.WebElement;

import java.util.Objects;

public class VisualRegion {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public VisualRegion(int x, int y, int width, int height) {
        this.x = Math.max(0, x);
        this.y = Math.max(0, y);
        this.width = Math.max(1, width);
        this.height = Math.max(1, height);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static VisualRegion inRegion(int x, int y, int width, int height) {
        return new VisualRegion(x, y, width, height);
    }

    public static VisualRegion from(VisualElement visualElement) {
        if (visualElement == null) {
            throw new IllegalArgumentException("VisualElement cannot be null when creating VisualRegion");
        }
        return new VisualRegion(visualElement.getX(), visualElement.getY(), visualElement.getWidth(), visualElement.getHeight());
    }

    public static VisualRegion from(WebElement webElement) {
        if (webElement == null) {
            throw new IllegalArgumentException("WebElement cannot be null when creating VisualRegion");
        }
        return new VisualRegion(
                webElement.getLocation().getX(),
                webElement.getLocation().getY(),
                webElement.getSize().getWidth(),
                webElement.getSize().getHeight()
        );
    }

    public static VisualRegion inRegion(VisualElement visualElement) {
        return from(visualElement);
    }

    public static VisualRegion inRegion(WebElement webElement) {
        return from(webElement);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VisualRegion that = (VisualRegion) o;
        return x == that.x && y == that.y && width == that.width && height == that.height;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, width, height);
    }

    @Override
    public String toString() {
        return String.format("VisualRegion[x=%d, y=%d, w=%d, h=%d]", x, y, width, height);
    }
}
