package com.znsio.teswiz.runner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.Point;

import com.znsio.teswiz.entities.Direction;

/**
 * Unit tests that {@link VisualElement} is a thin value object which forwards gestures to the {@link Driver}
 * facade using its own centre/geometry, and surfaces a missing facade as a programming error. No real driver is
 * needed - the facade is mocked.
 */
class VisualElementForwardingTest {

    // element at (10,20) size 100x40 -> centre (60,40)
    private static final int X = 10;
    private static final int Y = 20;
    private static final int WIDTH = 100;
    private static final int HEIGHT = 40;
    private static final Point EXPECTED_CENTER = new Point(60, 40);

    private VisualElement elementWith(Driver facade) {
        return new VisualElement(X, Y, WIDTH, HEIGHT, "SPIN", facade);
    }

    @Test
    void clickForwardsToFacadeAtCenter() {
        Driver facade = mock(Driver.class);
        elementWith(facade).click();
        verify(facade).visualClickAt(EXPECTED_CENTER, "SPIN");
    }

    @Test
    void doubleClickForwardsToFacadeAtCenter() {
        Driver facade = mock(Driver.class);
        elementWith(facade).doubleClick();
        verify(facade).visualDoubleClickAt(EXPECTED_CENTER, "SPIN");
    }

    @Test
    void hoverForwardsToFacadeAtCenter() {
        Driver facade = mock(Driver.class);
        elementWith(facade).hover();
        verify(facade).visualHoverAt(EXPECTED_CENTER, "SPIN");
    }

    @Test
    void swipeForwardsGeometryAndDirection() {
        Driver facade = mock(Driver.class);
        elementWith(facade).swipe(Direction.DOWN);
        verify(facade).visualSwipe(EXPECTED_CENTER, WIDTH, HEIGHT, Direction.DOWN, "SPIN");
    }

    @Test
    void longPressForwardsDefaultDuration() {
        Driver facade = mock(Driver.class);
        elementWith(facade).longPress();
        verify(facade).visualLongPressAt(eq(EXPECTED_CENTER), eq(Duration.ofSeconds(2)), eq("SPIN"));
    }

    @Test
    void sendKeysForwardsKeys() {
        Driver facade = mock(Driver.class);
        elementWith(facade).sendKeys("hello");
        verify(facade).visualEnterTextAt(eq(EXPECTED_CENTER), eq("SPIN"), any(CharSequence[].class));
    }

    @Test
    void everyGestureHighlightsAndClearsHighlight() {
        Driver facade = mock(Driver.class);
        elementWith(facade).click();
        verify(facade).highlightVisualElement(any(VisualElement.class));
        verify(facade).clearHighlight();
    }

    @Test
    void clearHighlightRunsEvenWhenGestureThrows() {
        Driver facade = mock(Driver.class);
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(facade).visualClickAt(any(Point.class), any());
        VisualElement element = elementWith(facade);

        assertThatThrownBy(element::click).isInstanceOf(RuntimeException.class);
        verify(facade).clearHighlight();
    }

    @Test
    void gestureWithoutFacadeThrowsIllegalState() {
        VisualElement element = elementWith(null);
        assertThatThrownBy(element::click)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPIN");
    }

    @Test
    void geometryHelpersAreUnaffected() {
        VisualElement element = elementWith(null);
        assertThat(element.getCenter()).isEqualTo(EXPECTED_CENTER);
        assertThat(element.getSize().getWidth()).isEqualTo(WIDTH);
        assertThat(element.withOffset(5, 5).getX()).isEqualTo(X + 5);
    }
}
