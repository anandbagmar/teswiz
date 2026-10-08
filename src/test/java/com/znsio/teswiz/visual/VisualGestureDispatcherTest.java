package com.znsio.teswiz.visual;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;

import com.znsio.teswiz.entities.Direction;
import com.znsio.teswiz.exceptions.UnsupportedVisualGestureException;

/**
 * Unit tests for {@link VisualGestureDispatcher} using a mocked inner driver, so no real browser/device is
 * needed. Covers the "unsupported gesture throws" contract and routing to native coordinate input.
 */
class VisualGestureDispatcherTest {

    private static final Point CENTER = new Point(50, 60);

    /**
     * A plain web driver that only implements WebDriver - no touch, no NativeCoordinateInput, no Interactive,
     * no JavascriptExecutor - so every coordinate gesture is genuinely unsupported.
     */
    private interface PlainWebDriver extends WebDriver {
    }

    /**
     * A web driver that supports native coordinate input (as the Playwright drivers do).
     */
    private interface NativeInputWebDriver extends WebDriver, NativeCoordinateInput {
    }

    @Test
    void swipeThrowsWhenDriverSupportsNoMechanism() {
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(mock(PlainWebDriver.class), false);

        assertThatThrownBy(() -> dispatcher.swipe(CENTER, 100, 40, Direction.UP, "SPIN"))
                .isInstanceOf(UnsupportedVisualGestureException.class)
                .hasMessageContaining("swipe")
                .hasMessageContaining("SPIN");
    }

    @Test
    void longPressThrowsWhenDriverSupportsNoMechanism() {
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(mock(PlainWebDriver.class), false);

        assertThatThrownBy(() -> dispatcher.longPress(CENTER, Duration.ofSeconds(1), "SPIN"))
                .isInstanceOf(UnsupportedVisualGestureException.class)
                .hasMessageContaining("longPress");
    }

    @Test
    void clickThrowsWhenDriverSupportsNoMechanism() {
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(mock(PlainWebDriver.class), false);

        assertThatThrownBy(() -> dispatcher.click(CENTER, "SPIN"))
                .isInstanceOf(UnsupportedVisualGestureException.class)
                .hasMessageContaining("click");
    }

    @Test
    void zoomAndPinchAreUnsupportedOnEveryEngine() {
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(mock(NativeInputWebDriver.class), false);

        assertThatThrownBy(() -> dispatcher.zoom(2.0, "map"))
                .isInstanceOf(UnsupportedVisualGestureException.class)
                .hasMessageContaining("Zoom");
        assertThatThrownBy(() -> dispatcher.pinch(0.5, "map"))
                .isInstanceOf(UnsupportedVisualGestureException.class)
                .hasMessageContaining("Pinch");
    }

    @Test
    void clickRoutesToNativeCoordinateInputAtElementCenter() {
        NativeInputWebDriver nativeDriver = mock(NativeInputWebDriver.class);
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(nativeDriver, false);

        dispatcher.click(CENTER, "SPIN");

        verify(nativeDriver).clickAtViewportPoint(50, 60);
    }

    @Test
    void swipeRoutesToNativeDragWhenSupported() {
        NativeInputWebDriver nativeDriver = mock(NativeInputWebDriver.class);
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(nativeDriver, false);

        dispatcher.swipe(CENTER, 100, 40, Direction.UP, "list");

        // delta is width/height-derived; UP moves the end point above the center by max(height/2, default)
        verify(nativeDriver).dragFromViewportPoint(org.mockito.ArgumentMatchers.eq(50),
                org.mockito.ArgumentMatchers.eq(60), org.mockito.ArgumentMatchers.eq(50),
                org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void longPressRoutesToNativeLongPressWhenSupported() {
        NativeInputWebDriver nativeDriver = mock(NativeInputWebDriver.class);
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(nativeDriver, false);

        Duration hold = Duration.ofSeconds(3);
        dispatcher.longPress(CENTER, hold, "tile");

        verify(nativeDriver).longPressAtViewportPoint(50, 60, hold);
    }

    @Test
    void doubleClickRoutesToNativeDoubleClickWhenSupported() {
        NativeInputWebDriver nativeDriver = mock(NativeInputWebDriver.class);
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(nativeDriver, false);

        dispatcher.doubleClick(CENTER, "tile");

        verify(nativeDriver).doubleClickAtViewportPoint(50, 60);
    }

    @Test
    void swipeEndPointForUpStaysWithinExpectedBand() {
        NativeInputWebDriver nativeDriver = mock(NativeInputWebDriver.class);
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(nativeDriver, false);

        // height 40 -> delta = max(20, DEFAULT 100) = 100; UP end y = 60 - 100 = -40
        dispatcher.swipe(CENTER, 100, 40, Direction.UP, "list");
        verify(nativeDriver).dragFromViewportPoint(50, 60, 50, -40);
    }

    @Test
    void swipeDeltaUsesHalfElementWhenLargeEnough() {
        NativeInputWebDriver nativeDriver = mock(NativeInputWebDriver.class);
        VisualGestureDispatcher dispatcher = new VisualGestureDispatcher(nativeDriver, false);

        // width 300 -> delta = 150; RIGHT end x = 50 + 150 = 200
        dispatcher.swipe(CENTER, 300, 300, Direction.RIGHT, "panel");
        verify(nativeDriver).dragFromViewportPoint(50, 60, 200, 60);
        assertThat(true).isTrue();
    }
}
