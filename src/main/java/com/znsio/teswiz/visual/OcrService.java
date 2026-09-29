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

import static net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE;
import static net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel.RIL_WORD;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;

public class OcrService {
    private static final Logger LOGGER = LogManager.getLogger(OcrService.class.getName());
    private static boolean tess4jAvailable = false;

    static {
        try {
            Class.forName("net.sourceforge.tess4j.Tesseract");
            tess4jAvailable = true;

            String existingJnaPath = System.getProperty("jna.library.path", "");
            String homebrewLib = "/opt/homebrew/lib";
            String usrLocalLib = "/usr/local/lib";
            StringBuilder newJnaPath = new StringBuilder(existingJnaPath);

            if (new File(homebrewLib).exists() && !existingJnaPath.contains(homebrewLib)) {
                if (!newJnaPath.isEmpty()) newJnaPath.append(File.pathSeparator);
                newJnaPath.append(homebrewLib);
            }
            if (new File(usrLocalLib).exists() && !existingJnaPath.contains(usrLocalLib)) {
                if (!newJnaPath.isEmpty()) newJnaPath.append(File.pathSeparator);
                newJnaPath.append(usrLocalLib);
            }
            if (!newJnaPath.isEmpty()) {
                System.setProperty("jna.library.path", newJnaPath.toString());
            }
            LOGGER.info("Tess4J Tesseract 5 engine available on classpath. jna.library.path: " + System.getProperty("jna.library.path"));
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

        String customLibPath = Setup.getFromConfigs("TESWIZ_TESSERACT_LIB_PATH");
        if (customLibPath != null && !customLibPath.isBlank() && new File(customLibPath).exists()) {
            String currentJnaPath = System.getProperty("jna.library.path", "");
            if (!currentJnaPath.contains(customLibPath)) {
                System.setProperty("jna.library.path", customLibPath + (currentJnaPath.isEmpty() ? "" : File.pathSeparator + currentJnaPath));
            }
        }

        String tessDataPath = resolveTessDataPath();
        if (tessDataPath == null) {
            LOGGER.error("eng.traineddata language data file not found in system tessdata paths or configuration.");
            throw new com.znsio.teswiz.exceptions.VisualSubsystemDisabledException(
                    "\n====================================================================================================\n" +
                    " [teswiz] Tesseract Language Data (tessdata/eng.traineddata) Not Found!\n" +
                    "----------------------------------------------------------------------------------------------------\n" +
                    " Tesseract OCR requires 'eng.traineddata' to perform text recognition.\n\n" +
                    " To fix this:\n" +
                    "  - macOS:   run 'brew install tesseract' or 'brew install tesseract-lang'\n" +
                    "  - Linux:   run 'sudo apt-get install tesseract-ocr-eng'\n" +
                    "  - Windows: download eng.traineddata from https://github.com/tesseract-ocr/tessdata\n" +
                    "  - Or set 'TESWIZ_TESSERACT_DATA_PATH=/path/to/tessdata' in your teswiz_config.properties file.\n" +
                    "====================================================================================================\n");
        }

        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);

            String normalizedSearch = searchText.trim().toLowerCase();
            double scaleFactor = 1.0;
            if (driverFacade != null) {
                scaleFactor = driverFacade.getViewportScaleFactor(bufferedImage.getWidth());
            }

            List<Word> words = tesseract.getWords(bufferedImage, RIL_WORD);
            String[] searchTokens = normalizedSearch.split("\\s+");

            if (searchTokens.length > 1) {
                for (int i = 0; i <= words.size() - searchTokens.length; i++) {
                    boolean sequenceMatched = true;
                    for (int j = 0; j < searchTokens.length; j++) {
                        String wordText = words.get(i + j).getText();
                        if (wordText == null || !wordText.trim().toLowerCase().contains(searchTokens[j])) {
                            sequenceMatched = false;
                            break;
                        }
                    }
                    if (sequenceMatched) {
                        Rectangle firstRect = words.get(i).getBoundingBox();
                        Rectangle lastRect = words.get(i + searchTokens.length - 1).getBoundingBox();
                        int minX = Math.min(firstRect.x, lastRect.x);
                        int minY = Math.min(firstRect.y, lastRect.y);
                        int maxX = Math.max(firstRect.x + firstRect.width, lastRect.x + lastRect.width);
                        int maxY = Math.max(firstRect.y + firstRect.height, lastRect.y + lastRect.height);
                        int rectW = maxX - minX;
                        int rectH = maxY - minY;

                        int logicalX = (int) Math.round(minX / scaleFactor);
                        int logicalY = (int) Math.round(minY / scaleFactor);
                        int logicalW = (int) Math.round(rectW / scaleFactor);
                        int logicalH = (int) Math.round(rectH / scaleFactor);

                        VisualElement candidate = new VisualElement(logicalX, logicalY, logicalW, logicalH, "OCR: " + searchText, driverFacade);
                        LOGGER.info(String.format("Found candidate OCR word sequence match '%s' at viewport bounds [x=%d, y=%d, w=%d, h=%d]",
                                searchText, logicalX, logicalY, logicalW, logicalH));
                        return candidate;
                    }
                }
            }

            for (Word word : words) {
                String wordText = word.getText();
                if (wordText != null && wordText.trim().toLowerCase().contains(normalizedSearch)) {
                    Rectangle rect = word.getBoundingBox();
                    int logicalX = (int) Math.round(rect.x / scaleFactor);
                    int logicalY = (int) Math.round(rect.y / scaleFactor);
                    int logicalW = (int) Math.round(rect.width / scaleFactor);
                    int logicalH = (int) Math.round(rect.height / scaleFactor);
                    LOGGER.info(String.format("Found OCR word match '%s' for search '%s' at screenshot bounds [x=%d, y=%d, w=%d, h=%d] -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                            wordText, searchText, rect.x, rect.y, rect.width, rect.height, logicalX, logicalY, logicalW, logicalH, scaleFactor));
                    return new VisualElement(logicalX, logicalY, logicalW, logicalH, "OCR: " + wordText, driverFacade);
                }
            }

            List<Word> lines = tesseract.getWords(bufferedImage, RIL_TEXTLINE);
            for (Word line : lines) {
                String lineText = line.getText();
                if (lineText != null && lineText.trim().toLowerCase().contains(normalizedSearch)) {
                    Rectangle rect = line.getBoundingBox();
                    int logicalX = (int) Math.round(rect.x / scaleFactor);
                    int logicalY = (int) Math.round(rect.y / scaleFactor);
                    int logicalW = (int) Math.round(rect.width / scaleFactor);
                    int logicalH = (int) Math.round(rect.height / scaleFactor);
                    LOGGER.info(String.format("Found OCR text line match '%s' for search '%s' at screenshot bounds [x=%d, y=%d, w=%d, h=%d] -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                            lineText, searchText, rect.x, rect.y, rect.width, rect.height, logicalX, logicalY, logicalW, logicalH, scaleFactor));
                    return new VisualElement(logicalX, logicalY, logicalW, logicalH, "OCR: " + lineText, driverFacade);
                }
            }
        } catch (UnsatisfiedLinkError e) {
            LOGGER.error("Native Tesseract shared library (libtesseract) could not be loaded: " + e.getMessage());
            throw new com.znsio.teswiz.exceptions.VisualSubsystemDisabledException(
                    "\n====================================================================================================\n" +
                    " [teswiz] Native Tesseract OCR Engine Not Found on Host Operating System!\n" +
                    "----------------------------------------------------------------------------------------------------\n" +
                    " Tesseract native shared library (libtesseract.dylib / libtesseract.so / tesseract.dll) could not be loaded.\n\n" +
                    " To install Tesseract for your operating system:\n" +
                    "  - macOS:   run 'brew install tesseract'\n" +
                    "  - Linux:   run 'sudo apt-get install tesseract-ocr libtesseract-dev'\n" +
                    "  - Windows: run 'winget install UB-Mannheim.TesseractOCR' or download from\n" +
                    "             https://github.com/UB-Mannheim/tesseract/wiki and add install dir to PATH.\n" +
                    "  - Alternative: set 'TESWIZ_TESSERACT_LIB_PATH=/path/to/lib' in your teswiz_config.properties.\n" +
                    "====================================================================================================\n", e);
        } catch (Throwable e) {
            LOGGER.warn("Tesseract OCR text extraction failed: " + e.getMessage());
        }

        return null;
    }

    private static String resolveTessDataPath() {
        String configuredPath = Setup.getFromConfigs("TESWIZ_TESSERACT_DATA_PATH");
        if (configuredPath != null && !configuredPath.isBlank()) {
            File confFile = new File(configuredPath);
            if (confFile.isDirectory() && new File(confFile, "eng.traineddata").exists()) {
                return confFile.getAbsolutePath();
            }
            if (confFile.isFile() && confFile.getName().endsWith("eng.traineddata") && confFile.exists()) {
                return confFile.getParentFile().getAbsolutePath();
            }
        }

        String envPath = System.getenv("TESSDATA_PREFIX");
        if (envPath != null && !envPath.isBlank()) {
            File envDir = new File(envPath);
            if (envDir.isDirectory() && new File(envDir, "eng.traineddata").exists()) {
                return envDir.getAbsolutePath();
            }
            if (envDir.isFile() && envDir.getName().endsWith("eng.traineddata") && envDir.exists()) {
                return envDir.getParentFile().getAbsolutePath();
            }
        }

        List<String> candidatePaths = List.of(
                "/opt/homebrew/share/tessdata",
                "/opt/homebrew/Cellar/tesseract/5.5.3/share/tessdata",
                "/usr/local/share/tessdata",
                "/usr/share/tessdata",
                "/usr/share/tesseract-ocr/4.00/tessdata",
                "/usr/share/tesseract-ocr/5/tessdata",
                "C:\\Program Files\\Tesseract-OCR\\tessdata"
        );

        for (String candidate : candidatePaths) {
            File dataFile = new File(candidate, "eng.traineddata");
            if (dataFile.exists()) {
                return candidate;
            }
        }

        try {
            File extracted = net.sourceforge.tess4j.util.LoadLibs.extractTessResources("tessdata");
            if (extracted != null && extracted.exists() && new File(extracted, "eng.traineddata").exists()) {
                return extracted.getAbsolutePath();
            }
        } catch (Throwable t) {
            LOGGER.debug("Could not extract tessdata via LoadLibs: " + t.getMessage());
        }

        return null;
    }
}
