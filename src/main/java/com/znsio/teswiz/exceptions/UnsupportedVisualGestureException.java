package com.znsio.teswiz.exceptions;

/**
 * Thrown when a visual gesture (e.g. swipe, long-press, zoom, pinch) is requested on a driver/engine that
 * provides no mechanism to perform it - for example a swipe on a plain Selenium web driver that supports neither
 * touch nor native coordinate input. Failing loudly is intentional: a silently-skipped gesture would let a test
 * pass against an application that never actually reacted.
 */
public class UnsupportedVisualGestureException extends RuntimeException {
    public UnsupportedVisualGestureException(String message) {
        super(message);
    }

    public UnsupportedVisualGestureException(String message, Throwable cause) {
        super(message, cause);
    }
}
