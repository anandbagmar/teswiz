package com.znsio.teswiz.visual;

import com.znsio.teswiz.entities.VisualRegion;

/**
 * Converts between the two coordinate spaces the vision subsystem straddles, at runtime, so region
 * cropping and located-box handling are correct on any display/OS combination (macOS Retina and
 * non-Retina, external monitors, Windows, Linux, headless):
 *
 * <ul>
 *   <li><b>Logical / CSS / viewport pixels</b> - what Selenium/Appium and the DOM report (element
 *       locations, viewport width). A {@link VisualRegion} is always in logical pixels.</li>
 *   <li><b>Physical / screenshot pixels</b> - what a captured screenshot actually contains. On a
 *       {@code devicePixelRatio > 1} display (e.g. Retina) the screenshot is larger than the
 *       viewport by that ratio.</li>
 * </ul>
 *
 * <p>The scale factor is never assumed or hard-coded per OS; callers pass the value computed at
 * runtime from the real screenshot width and the real viewport width (see
 * {@code Driver.getViewportScaleFactor(int)}), so this logic adapts to whatever display the browser
 * actually launched on.
 */
public final class VisualRegionScaler {

    private VisualRegionScaler() {
    }

    /**
     * An integer pixel rectangle in physical screenshot coordinates.
     */
    public record PixelRegion(int x, int y, int width, int height) {
    }

    /**
     * Scales a logical {@link VisualRegion} into a physical-pixel crop rectangle for the given
     * screenshot, clamped to the image bounds. A {@code null} region yields the whole image. A
     * non-positive {@code scaleFactor} falls back to {@code 1.0} (logical == physical).
     *
     * @param region      the region in logical/viewport pixels, or {@code null} for the whole image
     * @param imageWidth  the physical screenshot width in pixels
     * @param imageHeight the physical screenshot height in pixels
     * @param scaleFactor physical/logical ratio (screenshotWidth / viewportWidth) computed at runtime
     */
    public static PixelRegion scaleRegionToPixels(VisualRegion region, int imageWidth, int imageHeight,
            double scaleFactor) {
        if (region == null) {
            return new PixelRegion(0, 0, imageWidth, imageHeight);
        }
        double scale = scaleFactor > 0 ? scaleFactor : 1.0;

        int x = clamp((int) Math.round(region.getX() * scale), 0, Math.max(0, imageWidth - 1));
        int y = clamp((int) Math.round(region.getY() * scale), 0, Math.max(0, imageHeight - 1));
        int width = (int) Math.round(region.getWidth() * scale);
        int height = (int) Math.round(region.getHeight() * scale);

        // Clamp the extent so the crop never runs past the screenshot edge.
        width = Math.min(width, imageWidth - x);
        height = Math.min(height, imageHeight - y);
        width = Math.max(1, width);
        height = Math.max(1, height);

        return new PixelRegion(x, y, width, height);
    }

    /**
     * Clamps a located bounding box (in physical pixels, relative to the searched image) to the
     * image bounds, tolerating a box that touches or slightly overshoots the right/bottom edge - a
     * vision model routinely draws a box flush to the image edge, which must be accepted, not
     * discarded. Returns {@code null} only when the box is genuinely invalid: a negative origin, an
     * origin at/beyond the image edge, or a non-positive size.
     *
     * @return the clamped box, or {@code null} if the box cannot be made valid within the image
     */
    public static PixelRegion clampBoxToImage(int x, int y, int width, int height, int imageWidth, int imageHeight) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0) {
            return null;
        }
        if (x >= imageWidth || y >= imageHeight) {
            return null;
        }
        int clampedWidth = Math.min(width, imageWidth - x);
        int clampedHeight = Math.min(height, imageHeight - y);
        if (clampedWidth <= 0 || clampedHeight <= 0) {
            return null;
        }
        return new PixelRegion(x, y, clampedWidth, clampedHeight);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
