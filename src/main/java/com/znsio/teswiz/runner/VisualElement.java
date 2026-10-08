package com.znsio.teswiz.runner;

import java.time.Duration;

import org.openqa.selenium.Dimension;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;

import com.znsio.teswiz.entities.Direction;

/**
 * An immutable geometric handle to something located on screen by OCR text or image-template matching: its
 * bounding box ({@code x, y, width, height}) and {@code label}, plus the {@link Driver} facade used to act on it.
 *
 * <p>
 * A {@code VisualElement} does not know <em>how</em> a gesture is dispatched for the active engine - it forwards
 * each gesture to the {@link Driver} facade with its own centre/geometry, and the facade (via
 * {@code VisualGestureDispatcher}) decides between Appium touch, native coordinate input, Selenium Actions, or
 * synthesised DOM events. This keeps the element a pure value object with no dependency on driver internals.
 *
 * <p>
 * Each gesture auto-highlights the element before acting and clears the highlight afterwards (via a
 * {@code try/finally}), so a leftover overlay never masks the next locate screenshot.
 */
public class VisualElement {
    private static final Duration DEFAULT_LONG_PRESS = Duration.ofSeconds(2);

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final String label;
    private final Driver driverFacade;

    public VisualElement(int x, int y, int width, int height, String label, Driver driverFacade) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.label = label;
        this.driverFacade = driverFacade;
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

    public String getLabel() {
        return label;
    }

    public Point getCenter() {
        return new Point(x + width / 2, y + height / 2);
    }

    public Dimension getSize() {
        return new Dimension(width, height);
    }

    public VisualElement withOffset(int offsetX, int offsetY) {
        if (offsetX == 0 && offsetY == 0) {
            return this;
        }
        return new VisualElement(x + offsetX, y + offsetY, width, height, label, driverFacade);
    }

    public VisualElement highlight() {
        if (driverFacade != null) {
            driverFacade.highlightVisualElement(this);
        }
        return this;
    }

    public void click() {
        gesture(() -> driverFacade.visualClickAt(getCenter(), label));
    }

    public void doubleClick() {
        gesture(() -> driverFacade.visualDoubleClickAt(getCenter(), label));
    }

    public void hover() {
        gesture(() -> driverFacade.visualHoverAt(getCenter(), label));
    }

    public void sendKeys(CharSequence... keysToSend) {
        gesture(() -> driverFacade.visualEnterTextAt(getCenter(), label, keysToSend));
    }

    public void tap() {
        click();
    }

    public void doubleTap() {
        doubleClick();
    }

    public void swipe(Direction direction) {
        gesture(() -> driverFacade.visualSwipe(getCenter(), width, height, direction, label));
    }

    public void longPress(Duration duration) {
        gesture(() -> driverFacade.visualLongPressAt(getCenter(), duration, label));
    }

    public void longPress() {
        longPress(DEFAULT_LONG_PRESS);
    }

    public void dragAndDropTo(WebElement target) {
        gesture(() -> driverFacade.visualDragAndDropTo(getCenter(), target, label));
    }

    public void zoom(double scaleFactor) {
        requireFacade();
        driverFacade.visualZoom(scaleFactor, label);
    }

    public void pinch(double scaleFactor) {
        requireFacade();
        driverFacade.visualPinch(scaleFactor, label);
    }

    /**
     * Runs a gesture bracketed by highlight/clear so the on-screen overlay is always removed afterwards, even if
     * the gesture throws (e.g. an unsupported gesture). A null facade is a programming error for an actionable
     * element and is surfaced immediately.
     *
     * @param action the forwarding call to the Driver facade
     */
    private void gesture(Runnable action) {
        requireFacade();
        highlight();
        try {
            action.run();
        } finally {
            driverFacade.clearHighlight();
        }
    }

    private void requireFacade() {
        if (driverFacade == null) {
            throw new IllegalStateException(
                    "Cannot act on visual element '" + label + "': no Driver facade is attached");
        }
    }

    public WebElement toWebElement() {
        return VisualElementWebElementAdapter.wrap(this);
    }

    @Override
    public String toString() {
        return String.format("VisualElement{label='%s', bounds=[x=%d, y=%d, w=%d, h=%d]}", label, x, y, width, height);
    }
}
