package com.znsio.teswiz.visual;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.exceptions.VisualSubsystemDisabledException;
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

        BufferedImage bufferedImage = parseScreenshot(screenshotBytes);
        if (bufferedImage == null) {
            return null;
        }

        applyCustomJnaLibraryPath();

        String tessDataPath = resolveTessDataPath();
        if (tessDataPath == null) {
            throwTessDataNotFoundException();
        }

        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataPath);

            double scaleFactor = (driverFacade != null) ? driverFacade.getViewportScaleFactor(bufferedImage.getWidth()) : 1.0;
            String normalizedSearch = searchText.trim().toLowerCase();
            List<Word> words = tesseract.getWords(bufferedImage, RIL_WORD);

            VisualElement wordMatch = matchTextInWords(words, normalizedSearch, searchText, scaleFactor, driverFacade);
            if (wordMatch != null) {
                return wordMatch;
            }

            return searchLineMatch(tesseract, bufferedImage, normalizedSearch, searchText, scaleFactor, driverFacade);
        } catch (UnsatisfiedLinkError e) {
            LOGGER.error("Native Tesseract shared library (libtesseract) could not be loaded: " + e.getMessage());
            throwNativeLibraryNotFoundException(e);
            return null;
        } catch (Throwable e) {
            LOGGER.warn("Tesseract OCR text extraction failed: " + e.getMessage());
            return null;
        }
    }

    private static BufferedImage parseScreenshot(byte[] screenshotBytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(screenshotBytes));
        } catch (IOException e) {
            LOGGER.warn("Failed to parse screenshot byte stream for OCR: " + e.getMessage());
            return null;
        }
    }

    private static void applyCustomJnaLibraryPath() {
        String customLibPath = Setup.getFromConfigs("TESWIZ_TESSERACT_LIB_PATH");
        if (customLibPath != null && !customLibPath.isBlank() && new File(customLibPath).exists()) {
            String currentJnaPath = System.getProperty("jna.library.path", "");
            if (!currentJnaPath.contains(customLibPath)) {
                System.setProperty("jna.library.path", customLibPath + (currentJnaPath.isEmpty() ? "" : File.pathSeparator + currentJnaPath));
            }
        }
    }

    private static VisualElement matchTextInWords(List<Word> words, String normalizedSearch, String searchText, double scaleFactor, Driver driverFacade) {
        String[] searchTokens = normalizedSearch.split("\\s+");
        if (searchTokens.length > 1) {
            VisualElement sequenceMatch = searchWordSequence(words, searchTokens, searchText, scaleFactor, driverFacade);
            if (sequenceMatch != null) {
                return sequenceMatch;
            }
        }

        VisualElement exactMatch = searchExactWordMatch(words, normalizedSearch, searchText, scaleFactor, driverFacade);
        if (exactMatch != null) {
            return exactMatch;
        }

        VisualElement boundaryMatch = searchWordBoundaryMatch(words, normalizedSearch, searchText, scaleFactor, driverFacade);
        if (boundaryMatch != null) {
            return boundaryMatch;
        }

        return searchSubstringWordMatch(words, normalizedSearch, searchText, scaleFactor, driverFacade);
    }

    private static VisualElement searchWordSequence(List<Word> words, String[] searchTokens, String searchText, double scaleFactor, Driver driverFacade) {
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

                VisualElement candidate = buildScaledVisualElement(minX, minY, rectW, rectH, scaleFactor, searchText, driverFacade);
                LOGGER.info(String.format("Found candidate OCR word sequence match '%s' at viewport bounds [x=%d, y=%d, w=%d, h=%d]",
                        searchText, candidate.getX(), candidate.getY(), candidate.getWidth(), candidate.getHeight()));
                return candidate;
            }
        }
        return null;
    }

    private static VisualElement searchExactWordMatch(List<Word> words, String normalizedSearch, String searchText, double scaleFactor, Driver driverFacade) {
        for (Word word : words) {
            String wordText = word.getText();
            if (wordText != null) {
                String cleaned = wordText.trim().replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$", "");
                if (wordText.trim().equalsIgnoreCase(normalizedSearch) || cleaned.equalsIgnoreCase(normalizedSearch)) {
                    Rectangle rect = word.getBoundingBox();
                    VisualElement element = buildScaledVisualElement(rect.x, rect.y, rect.width, rect.height, scaleFactor, wordText.trim(), driverFacade);
                    LOGGER.info(String.format("Found exact OCR word match '%s' for search '%s' at screenshot bounds [x=%d, y=%d, w=%d, h=%d] -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                            wordText.trim(), searchText, rect.x, rect.y, rect.width, rect.height, element.getX(), element.getY(), element.getWidth(), element.getHeight(), scaleFactor));
                    return element;
                }
            }
        }
        return null;
    }

    private static VisualElement searchWordBoundaryMatch(List<Word> words, String normalizedSearch, String searchText, double scaleFactor, Driver driverFacade) {
        Pattern wordBoundaryPattern = Pattern.compile("\\b" + Pattern.quote(normalizedSearch) + "\\b", Pattern.CASE_INSENSITIVE);
        for (Word word : words) {
            String wordText = word.getText();
            if (wordText != null && wordBoundaryPattern.matcher(wordText.trim()).find()) {
                Rectangle rect = word.getBoundingBox();
                VisualElement element = buildScaledVisualElement(rect.x, rect.y, rect.width, rect.height, scaleFactor, wordText.trim(), driverFacade);
                LOGGER.info(String.format("Found word-boundary OCR match '%s' for search '%s' at screenshot bounds [x=%d, y=%d, w=%d, h=%d] -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                        wordText.trim(), searchText, rect.x, rect.y, rect.width, rect.height, element.getX(), element.getY(), element.getWidth(), element.getHeight(), scaleFactor));
                return element;
            }
        }
        return null;
    }

    private static VisualElement searchSubstringWordMatch(List<Word> words, String normalizedSearch, String searchText, double scaleFactor, Driver driverFacade) {
        for (Word word : words) {
            String wordText = word.getText();
            if (wordText != null && wordText.trim().toLowerCase().contains(normalizedSearch)) {
                Rectangle rect = word.getBoundingBox();
                VisualElement element = buildScaledVisualElement(rect.x, rect.y, rect.width, rect.height, scaleFactor, wordText.trim(), driverFacade);
                LOGGER.info(String.format("Found OCR word substring match '%s' for search '%s' at screenshot bounds [x=%d, y=%d, w=%d, h=%d] -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                        wordText.trim(), searchText, rect.x, rect.y, rect.width, rect.height, element.getX(), element.getY(), element.getWidth(), element.getHeight(), scaleFactor));
                return element;
            }
        }
        return null;
    }

    private static VisualElement searchLineMatch(Tesseract tesseract, BufferedImage bufferedImage, String normalizedSearch, String searchText, double scaleFactor, Driver driverFacade) {
        List<Word> lines = tesseract.getWords(bufferedImage, RIL_TEXTLINE);
        for (Word line : lines) {
            String lineText = line.getText();
            if (lineText != null && lineText.trim().toLowerCase().contains(normalizedSearch)) {
                Rectangle rect = line.getBoundingBox();
                VisualElement element = buildScaledVisualElement(rect.x, rect.y, rect.width, rect.height, scaleFactor, lineText.trim(), driverFacade);
                LOGGER.info(String.format("Found OCR text line match '%s' for search '%s' at screenshot bounds [x=%d, y=%d, w=%d, h=%d] -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                        lineText.trim(), searchText, rect.x, rect.y, rect.width, rect.height, element.getX(), element.getY(), element.getWidth(), element.getHeight(), scaleFactor));
                return element;
            }
        }
        return null;
    }

    private static VisualElement buildScaledVisualElement(int rawX, int rawY, int rawWidth, int rawHeight, double scaleFactor, String labelText, Driver driverFacade) {
        int logicalX = (int) Math.round(rawX / scaleFactor);
        int logicalY = (int) Math.round(rawY / scaleFactor);
        int logicalW = (int) Math.round(rawWidth / scaleFactor);
        int logicalH = (int) Math.round(rawHeight / scaleFactor);
        return new VisualElement(logicalX, logicalY, logicalW, logicalH, "OCR: " + labelText, driverFacade);
    }

    private static void throwTessDataNotFoundException() {
        LOGGER.error("eng.traineddata language data file not found in system tessdata paths or configuration.");
        throw new VisualSubsystemDisabledException(
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

    private static void throwNativeLibraryNotFoundException(UnsatisfiedLinkError e) {
        throw new VisualSubsystemDisabledException(
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
    }

    private static String resolveTessDataPath() {
        String path = checkConfiguredTessDataPath();
        if (path != null) return path;

        path = checkEnvironmentTessDataPath();
        if (path != null) return path;

        path = checkSystemTessDataPaths();
        if (path != null) return path;

        return extractTessDataFromClasspath();
    }

    private static String checkConfiguredTessDataPath() {
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
        return null;
    }

    private static String checkEnvironmentTessDataPath() {
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
        return null;
    }

    private static String checkSystemTessDataPaths() {
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
        return null;
    }

    private static String extractTessDataFromClasspath() {
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
