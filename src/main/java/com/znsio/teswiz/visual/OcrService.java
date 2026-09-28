package com.znsio.teswiz.visual;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Setup;
import com.znsio.teswiz.runner.VisualElement;

import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

public class OcrService {
    private static final Logger LOGGER = LogManager.getLogger(OcrService.class.getName());
    private static boolean tess4jAvailable = false;

    static {
        try {
            Class.forName("net.sourceforge.tess4j.Tesseract");
            tess4jAvailable = true;
            LOGGER.info("Tess4J Tesseract 5 engine available on classpath.");
        } catch (Throwable t) {
            LOGGER.warn("Tess4J Tesseract engine is not available on classpath: " + t.getMessage());
        }
    }

    public static boolean isTess4jAvailable() {
        return tess4jAvailable;
    }

    public static VisualElement findTextMatch(byte[] screenshotBytes, String searchText, Driver driverFacade) {
        if (!isTess4jAvailable() || screenshotBytes == null || searchText == null || searchText.isBlank()) {
            return null;
        }

        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
        } catch (IOException e) {
            LOGGER.warn("Failed to parse screenshot byte stream for OCR: " + e.getMessage());
            return null;
        }

        if (bufferedImage == null) {
            return null;
        }

        Tesseract tesseract = new Tesseract();
        String dataPath = Setup.getFromConfigs("TESWIZ_TESSERACT_DATA_PATH");
        if (dataPath != null && !dataPath.isBlank() && new File(dataPath).exists()) {
            tesseract.setDatapath(dataPath);
        }

        try {
            List<Word> words = tesseract.getWords(bufferedImage, TessPageIteratorLevel.RIL_WORD);
            String normalizedSearch = searchText.trim().toLowerCase();

            for (Word word : words) {
                String wordText = word.getText();
                if (wordText != null && wordText.trim().toLowerCase().contains(normalizedSearch)) {
                    Rectangle rect = word.getBoundingBox();
                    LOGGER.info(String.format("Found OCR text match '%s' for search '%s' at bounds [x=%d, y=%d, w=%d, h=%d]",
                            wordText, searchText, rect.x, rect.y, rect.width, rect.height));
                    return new VisualElement(rect.x, rect.y, rect.width, rect.height, "OCR: " + wordText, driverFacade);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Tesseract OCR text extraction failed: " + e.getMessage());
        }

        return null;
    }
}
