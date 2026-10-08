package com.znsio.teswiz.screen.visualocr;

import com.znsio.teswiz.screen.ScreenRegistry;

/**
 * Visual OCR &amp; image-recognition screen contract (Feature -&gt; Step -&gt; BL -&gt; Screen pattern).
 *
 * <p>
 * The operations are segregated into role interfaces - {@link VisualElementFinder} (locate),
 * {@link VisualElementActions} (act), and {@link VisualElementInspector} (inspect) - and this type is the
 * aggregate that implements all three. {@code VisualOcrBL} and the convention-resolved platform screens use the
 * aggregate, while a consumer that needs only one role can depend on the narrower interface.
 *
 * <p>
 * This is deliberately an abstract class with abstract operations (inherited from the role interfaces) and an
 * accessible no-arg constructor, so it can be implemented two ways: by {@link AbstractVisualOcrScreen} subclasses
 * (shared action choreography) and by the dynamically generated playwright-ts bridge (which subclasses this
 * contract and routes the abstract methods to a TypeScript worker). Keep the operations abstract and this
 * constructor accessible so both paths continue to work.
 */
public abstract class VisualOcrScreen
        implements VisualElementFinder, VisualElementActions, VisualElementInspector {

    public static VisualOcrScreen get() {
        return ScreenRegistry.getScreen(VisualOcrScreen.class);
    }
}
