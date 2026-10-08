package com.znsio.teswiz.runner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.openqa.selenium.WebElement;

import com.znsio.teswiz.entities.Direction;

/**
 * Guards the self-clearing contract of {@link VisualElement} gestures: a gesture draws a highlight and MUST remove it
 * before returning, on every path. The regression this protects against is a leftover highlight overlay being captured
 * in the next locate screenshot and masking the template, breaking the subsequent match (GAMC-19077).
 * <p>
 * The {@link Driver} is mocked and its {@code getInnerDriver()} returns {@code null}, so each gesture takes its
 * "no input mechanism" early-return path. That is deliberate: it needs no live browser, and it proves the cleanup runs
 * even when the gesture itself does nothing — i.e. that {@code clearHighlight()} is in a {@code finally}, not merely at
 * the end of the happy path.
 */
class VisualElementHighlightTest {

    private Driver driver;
    private VisualElement element;

    @BeforeEach
    void setUp() {
        // getInnerDriver() and getType() default to null on the mock, so every gesture falls through to its
        // warnNotPerformed early return — the cleanup must still fire.
        driver = mock(Driver.class);
        element = new VisualElement(10, 20, 100, 40, "SPIN", driver);
    }

    @Test
    void clickHighlightsThenClearsEvenWhenNoInputMechanismIsAvailable() {
        element.click();

        InOrder inOrder = Mockito.inOrder(driver);
        inOrder.verify(driver).highlightVisualElement(element);
        inOrder.verify(driver).clearHighlight();
    }

    @Test
    void doubleClickClearsItsHighlight() {
        element.doubleClick();
        verify(driver).clearHighlight();
    }

    @Test
    void hoverClearsItsHighlight() {
        element.hover();
        verify(driver).clearHighlight();
    }

    @Test
    void longPressClearsItsHighlight() {
        element.longPress();
        verify(driver).clearHighlight();
    }

    @Test
    void sendKeysClearsItsHighlight() {
        element.sendKeys("hello");
        verify(driver).clearHighlight();
    }

    @Test
    void swipeClearsAnyActiveHighlight() {
        element.swipe(Direction.UP);
        verify(driver).clearHighlight();
    }

    @Test
    void dragAndDropToClearsAnyActiveHighlight() {
        element.dragAndDropTo(mock(WebElement.class));
        verify(driver).clearHighlight();
    }
}
