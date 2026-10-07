package com.znsio.teswiz.visual;

import java.time.Duration;

/**
 * Implemented by web drivers that can dispatch input at viewport coordinates through the browser's real input
 * pipeline, rather than by synthesising DOM events.
 *
 * <p>
 * This exists for visually-located elements. {@code VisualElement} knows only a bounding box from OCR or template
 * matching - there is no DOM node to act on - so it has to act on a point. Its fallback path dispatches synthetic
 * events at {@code document.elementFromPoint(x, y)}, which is enough for ordinary DOM pages but not for content
 * rendered into a {@code <canvas>}: a canvas application does its own hit-testing from pointer listeners bound to the
 * canvas, and synthesised events are additionally {@code isTrusted: false}. The action appears to succeed and the
 * application never reac * application never reac * application never reac * application never reac * application never reacywright, through CDP), so it is
 * indistinguishable from a user and reaches canvas content.
 *
 * <p>
 * All coordinates are viewport-relative, matching the bounds {@code VisualElement} resolves.
 */
public interface NativeCoordinateInput {

    /**
     * Clicks at a point.
     *
     * @param x viewport x coordinate
     * @param y viewport y coordinate
     */
    void clickAtViewportPoint(int x, int y);

    /**
     * Double-clicks at a point.
     *
     * @param x viewport x coordinate
     * @param y viewport y coordinate
     */
    void doubleClickAtViewportPoint(int x, int y);

    /**
     * Moves the pointer to a point, which is how a hover is expressed for canvas content.
     *
     * @param x viewport x coordinate
     * @param y viewport y coordinate
     */
    void hoverAtViewportPoint(int x, int y);

    /**
     * Presses at a point, holds for the given duration, then releases.
     *
     * @param x        viewport x coordinate
     * @param y        viewport y coordinate
     * @param duration how long to hold the press
     */
    void longPressAtViewportPoint(int x, int y, Duration duration);

    /**
     * Presses at a start point, drags to an end point, then releases. Used for swipe and drag gestures.
     *
     * @param startX viewport x coordinate to press at
     * @param startY viewport y coordinate to press at
     * @param endX   viewport x coordinate to release at
     * @param endY   viewport y coordinate to release at
     */
    void dragFromViewportPoint(int startX, int startY, int endX, int endY);
}
