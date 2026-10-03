package com.znsio.teswiz.visual;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
    private static final double[] PRIMARY_ROTATION_ANGLES = {90.0, 270.0, 45.0, -45.0, 30.0, -30.0, 180.0};
    private static final double[] FINE_GRAINED_ROTATION_ANGLES = {15.0, -15.0, 60.0, -60.0, 75.0, -75.0, 105.0, 120.0, 135.0, 150.0, 210.0, 225.0, 240.0, 300.0, 315.0, 330.0};

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
        return findTextMatch(screenshotBytes, searchText, null, driverFacade);
    }

    public static VisualElement findTextMatch(byte[] screenshotBytes, String searchText, com.znsio.teswiz.entities.VisualRegion region, Driver driverFacade) {
        if (!isTess4jAvailable() || screenshotBytes == null || searchText == null || searchText.isBlank()) {
            return null;
        }

        BufferedImage bufferedImage = parseScreenshot(screenshotBytes);
        if (bufferedImage == null) {
            return null;
        }

        double scaleFactor = (driverFacade != null) ? driverFacade.getViewportScaleFactor(bufferedImage.getWidth()) : 1.0;
        CroppedRegion cropped = cropRegion(bufferedImage, region);

        applyCustomJnaLibraryPath();
        String tessDataPath = resolveTessDataPath();
        if (tessDataPath == null) {
            throwTessDataNotFoundException();
        }

        try {
            Tesseract tesseract = createTesseractInstance(tessDataPath);
            String normalizedSearch = searchText.trim().toLowerCase();
            List<Word> words = tesseract.getWords(cropped.image, RIL_WORD);

            VisualElement wordMatch = matchTextInWords(words, normalizedSearch, searchText, scaleFactor, driverFacade);
            if (wordMatch != null) {
                return wordMatch.withOffset(cropped.offsetX, cropped.offsetY);
            }

            BufferedImage preprocessed = preprocessForOcr(cropped.image);
            List<Word> prepWords = tesseract.getWords(preprocessed, RIL_WORD);
            VisualElement prepMatch = matchTextInWords(prepWords, normalizedSearch, searchText, scaleFactor, driverFacade);
            if (prepMatch != null) {
                return prepMatch.withOffset(cropped.offsetX, cropped.offsetY);
            }

            VisualElement lineMatch = searchLineMatch(tesseract, cropped.image, normalizedSearch, searchText, scaleFactor, driverFacade);
            if (lineMatch != null) {
                return lineMatch.withOffset(cropped.offsetX, cropped.offsetY);
            }

            VisualElement rotatedMatch = searchRotatedTextMatch(tesseract, cropped.image, normalizedSearch, searchText, scaleFactor, driverFacade);
            return rotatedMatch != null ? rotatedMatch.withOffset(cropped.offsetX, cropped.offsetY) : null;
        } catch (UnsatisfiedLinkError e) {
            LOGGER.error("Native Tesseract shared library (libtesseract) could not be loaded: " + e.getMessage());
            throwNativeLibraryNotFoundException(e);
            return null;
        } catch (Throwable e) {
            LOGGER.warn("Tesseract OCR text extraction failed: " + e.getMessage());
            return null;
        }
    }

    public static List<VisualElement> findAllTextMatches(byte[] screenshotBytes, String searchText, Driver driverFacade) {
        return findAllTextMatches(screenshotBytes, searchText, null, driverFacade);
    }

    public static List<VisualElement> findAllTextMatches(byte[] screenshotBytes, String searchText, com.znsio.teswiz.entities.VisualRegion region, Driver driverFacade) {
        if (!isTess4jAvailable() || screenshotBytes == null || searchText == null || searchText.isBlank()) {
            return Collections.emptyList();
        }

        BufferedImage bufferedImage = parseScreenshot(screenshotBytes);
        if (bufferedImage == null) {
            return Collections.emptyList();
        }

        double scaleFactor = (driverFacade != null) ? driverFacade.getViewportScaleFactor(bufferedImage.getWidth()) : 1.0;
        CroppedRegion cropped = cropRegion(bufferedImage, region);

        applyCustomJnaLibraryPath();
        String tessDataPath = resolveTessDataPath();
        if (tessDataPath == null) {
            throwTessDataNotFoundException();
        }

        List<VisualElement> matches = new ArrayList<>();
        try {
            Tesseract tesseract = createTesseractInstance(tessDataPath);
            String normalizedSearch = searchText.trim().toLowerCase();
            List<Word> words = tesseract.getWords(cropped.image, RIL_WORD);

            for (Word word : words) {
                String wordText = word.getText();
                if (wordText != null && isWordMatchingSearch(wordText, normalizedSearch)) {
                    Rectangle rect = word.getBoundingBox();
                    VisualElement element = buildScaledVisualElement(rect.x, rect.y, rect.width, rect.height, scaleFactor, wordText.trim(), driverFacade);
                    matches.add(element.withOffset(cropped.offsetX, cropped.offsetY));
                }
            }

            if (matches.isEmpty()) {
                BufferedImage preprocessed = preprocessForOcr(cropped.image);
                List<Word> prepWords = tesseract.getWords(preprocessed, RIL_WORD);
                for (Word word : prepWords) {
                    String wordText = word.getText();
                    if (wordText != null && isWordMatchingSearch(wordText, normalizedSearch)) {
                        Rectangle rect = word.getBoundingBox();
                        VisualElement element = buildScaledVisualElement(rect.x, rect.y, rect.width, rect.height, scaleFactor, wordText.trim(), driverFacade);
                        matches.add(element.withOffset(cropped.offsetX, cropped.offsetY));
                    }
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Tesseract OCR findAllTextMatches failed: " + e.getMessage());
        }

        return applyNonMaximumSuppression(matches);
    }

    private static BufferedImage preprocessForOcr(BufferedImage image) {
        BufferedImage processed = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = processed.createGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        int width = processed.getWidth();
        int height = processed.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gray = processed.getRaster().getSample(x, y, 0);
                int newGray = (gray < 220) ? Math.max(0, gray - 50) : 255;
                processed.getRaster().setSample(x, y, 0, newGray);
            }
        }
        return processed;
    }

    private static Tesseract createTesseractInstance(String tessDataPath) {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        return tesseract;
    }

    private static boolean isWordMatchingSearch(String wordText, String normalizedSearch) {
        String trimmed = wordText.trim();
        String cleaned = trimmed.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$", "");
        return trimmed.equalsIgnoreCase(normalizedSearch) || cleaned.equalsIgnoreCase(normalizedSearch) || trimmed.toLowerCase().contains(normalizedSearch);
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

    private static VisualElement searchRotatedTextMatch(Tesseract tesseract, BufferedImage originalImage, String normalizedSearch, String searchText, double scaleFactor, Driver driverFacade) {
        VisualElement match = searchAngles(tesseract, originalImage, PRIMARY_ROTATION_ANGLES, normalizedSearch, searchText, scaleFactor, driverFacade, "Primary");
        if (match != null) {
            return match;
        }
        return searchAngles(tesseract, originalImage, FINE_GRAINED_ROTATION_ANGLES, normalizedSearch, searchText, scaleFactor, driverFacade, "Fine-grained intermediate");
    }

    private static VisualElement searchAngles(Tesseract tesseract, BufferedImage originalImage, double[] angles, String normalizedSearch, String searchText, double scaleFactor, Driver driverFacade, String passName) {
        int origW = originalImage.getWidth();
        int origH = originalImage.getHeight();

        for (double angle : angles) {
            BufferedImage rotatedImage = rotateImage(originalImage, angle);
            if (rotatedImage == null) {
                continue;
            }

            List<Word> rotatedWords = tesseract.getWords(rotatedImage, RIL_WORD);
            VisualElement rawMatch = matchTextInWords(rotatedWords, normalizedSearch, searchText, 1.0, null);
            if (rawMatch != null) {
                Rectangle rotRect = new Rectangle(rawMatch.getX(), rawMatch.getY(), rawMatch.getWidth(), rawMatch.getHeight());
                Rectangle mappedRect = mapRotatedRectToOriginal(rotRect, angle, origW, origH, rotatedImage.getWidth(), rotatedImage.getHeight());

                String matchedLabel = rawMatch.getLabel().startsWith("OCR: ") ? rawMatch.getLabel().substring(5) : rawMatch.getLabel();
                VisualElement element = buildScaledVisualElement(mappedRect.x, mappedRect.y, mappedRect.width, mappedRect.height, scaleFactor, matchedLabel, driverFacade);
                LOGGER.info(String.format("Found rotated OCR match '%s' for search '%s' during %s pass at angle %.1f° -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                        matchedLabel, searchText, passName, angle, element.getX(), element.getY(), element.getWidth(), element.getHeight(), scaleFactor));
                return element;
            }
        }
        return null;
    }

    private static Rectangle mapRotatedRectToOriginal(Rectangle rotRect, double angleDegrees, int origW, int origH, int rotW, int rotH) {
        int x1 = rotRect.x;
        int y1 = rotRect.y;
        int x2 = rotRect.x + rotRect.width;
        int y2 = rotRect.y + rotRect.height;

        Point p1 = mapRotatedPointToOriginal(x1, y1, angleDegrees, origW, origH, rotW, rotH);
        Point p2 = mapRotatedPointToOriginal(x2, y1, angleDegrees, origW, origH, rotW, rotH);
        Point p3 = mapRotatedPointToOriginal(x1, y2, angleDegrees, origW, origH, rotW, rotH);
        Point p4 = mapRotatedPointToOriginal(x2, y2, angleDegrees, origW, origH, rotW, rotH);

        int minX = Math.min(Math.min(p1.x, p2.x), Math.min(p3.x, p4.x));
        int minY = Math.min(Math.min(p1.y, p2.y), Math.min(p3.y, p4.y));
        int maxX = Math.max(Math.max(p1.x, p2.x), Math.max(p3.x, p4.x));
        int maxY = Math.max(Math.max(p1.y, p2.y), Math.max(p3.y, p4.y));

        minX = Math.max(0, Math.min(minX, origW - 1));
        minY = Math.max(0, Math.min(minY, origH - 1));
        int rectW = Math.max(1, Math.min(maxX - minX, origW - minX));
        int rectH = Math.max(1, Math.min(maxY - minY, origH - minY));

        return new Rectangle(minX, minY, rectW, rectH);
    }

    private static Point mapRotatedPointToOriginal(int rotX, int rotY, double angleDegrees, int origW, int origH, int rotW, int rotH) {
        double radians = Math.toRadians(-angleDegrees);
        double rotCenterX = rotW / 2.0;
        double rotCenterY = rotH / 2.0;
        double origCenterX = origW / 2.0;
        double origCenterY = origH / 2.0;

        double xRel = rotX - rotCenterX;
        double yRel = rotY - rotCenterY;

        double origXRel = xRel * Math.cos(radians) - yRel * Math.sin(radians);
        double origYRel = xRel * Math.sin(radians) + yRel * Math.cos(radians);

        int origX = (int) Math.round(origXRel + origCenterX);
        int origY = (int) Math.round(origYRel + origCenterY);

        return new Point(origX, origY);
    }

    private static BufferedImage rotateImage(BufferedImage src, double angleDegrees) {
        try {
            double radians = Math.toRadians(angleDegrees);
            double sin = Math.abs(Math.sin(radians));
            double cos = Math.abs(Math.cos(radians));
            int srcW = src.getWidth();
            int srcH = src.getHeight();
            int newW = (int) Math.floor(srcW * cos + srcH * sin);
            int newH = (int) Math.floor(srcW * sin + srcH * cos);

            BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = result.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            AffineTransform at = new AffineTransform();
            at.translate((newW - srcW) / 2.0, (newH - srcH) / 2.0);
            at.rotate(radians, srcW / 2.0, srcH / 2.0);
            g2d.drawRenderedImage(src, at);
            g2d.dispose();

            return result;
        } catch (Exception e) {
            LOGGER.warn("Failed to rotate image by " + angleDegrees + "°: " + e.getMessage());
            return null;
        }
    }

    private static VisualElement buildScaledVisualElement(int rawX, int rawY, int rawWidth, int rawHeight, double scaleFactor, String labelText, Driver driverFacade) {
        int logicalX = (int) Math.round(rawX / scaleFactor);
        int logicalY = (int) Math.round(rawY / scaleFactor);
        int logicalW = (int) Math.round(rawWidth / scaleFactor);
        int logicalH = (int) Math.round(rawHeight / scaleFactor);
        return new VisualElement(logicalX, logicalY, logicalW, logicalH, "OCR: " + labelText, driverFacade);
    }

    private static List<VisualElement> applyNonMaximumSuppression(List<VisualElement> rawMatches) {
        if (rawMatches == null || rawMatches.isEmpty()) {
            return Collections.emptyList();
        }

        List<VisualElement> filtered = new ArrayList<>();
        for (VisualElement candidate : rawMatches) {
            boolean isDuplicate = false;
            for (VisualElement existing : filtered) {
                int dx = Math.abs(candidate.getX() - existing.getX());
                int dy = Math.abs(candidate.getY() - existing.getY());
                if (dx < 10 && dy < 10) {
                    isDuplicate = true;
                    break;
                }
            }
            if (!isDuplicate) {
                filtered.add(candidate);
            }
        }
        return filtered;
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

    private static class CroppedRegion {
        final BufferedImage image;
        final int offsetX;
        final int offsetY;

        CroppedRegion(BufferedImage image, int offsetX, int offsetY) {
            this.image = image;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    private static CroppedRegion cropRegion(BufferedImage bufferedImage, com.znsio.teswiz.entities.VisualRegion region) {
        if (region == null) {
            return new CroppedRegion(bufferedImage, 0, 0);
        }
        int offsetX = Math.max(0, Math.min(region.getX(), bufferedImage.getWidth() - 1));
        int offsetY = Math.max(0, Math.min(region.getY(), bufferedImage.getHeight() - 1));
        int w = Math.min(region.getWidth(), bufferedImage.getWidth() - offsetX);
        int h = Math.min(region.getHeight(), bufferedImage.getHeight() - offsetY);
        BufferedImage subimage = (w > 0 && h > 0) ? bufferedImage.getSubimage(offsetX, offsetY, w, h) : bufferedImage;
        return new CroppedRegion(subimage, offsetX, offsetY);
    }
}
