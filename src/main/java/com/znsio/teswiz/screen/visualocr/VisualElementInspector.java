package com.znsio.teswiz.screen.visualocr;

import java.util.List;

/**
 * Inspection role of a Visual OCR screen: highlight a located element and capture a single screenshot, without
 * performing any gesture or settle wait. Segregated from the finding and acting roles.
 */
public interface VisualElementInspector {

    VisualOcrScreen inspectVisualElementByText(String elementName, String ocrText);
    VisualOcrScreen inspectVisualElementByImage(String elementName, List<String> imagePaths);
}
