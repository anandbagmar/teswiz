package com.znsio.teswiz.visual;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.image.BufferedImage;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.VisualElement;

public class ImageRecognitionService {
    private static final Logger LOGGER = LogManager.getLogger(ImageRecognitionService.class.getName());
    private static boolean openCvNativeLoaded = false;
    private static final double[] DEFAULT_SCALE_FACTORS = {1.0, 0.75, 0.5, 1.25, 1.5, 2.0};

    static {
        try {
            nu.pattern.OpenCV.loadLocally();
            openCvNativeLoaded = true;
            LOGGER.info("OpenCV native library loaded successfully via OpenPnP loadLocally.");
        } catch (Throwable t) {
            try {
                nu.pattern.OpenCV.loadShared();
                openCvNativeLoaded = true;
                LOGGER.info("OpenCV native library loaded successfully via OpenPnP loadShared.");
            } catch (Throwable t2) {
                try {
                    System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                    openCvNativeLoaded = true;
                    LOGGER.info("OpenCV native library loaded via System.loadLibrary.");
                } catch (Throwable e) {
                    LOGGER.warn("Failed to load OpenCV native shared library: " + e.getMessage());
                }
            }
        }
    }

    public static boolean isOpenCvAvailable() {
        return openCvNativeLoaded;
    }

    public static VisualElement findTemplateMatch(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, Driver driverFacade) {
        return findTemplateMatch(screenshotBytes, templatePaths, confidenceThreshold, null, driverFacade);
    }

    public static VisualElement findTemplateMatch(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, com.znsio.teswiz.entities.VisualRegion region, Driver driverFacade) {
        if (!isSearchable(screenshotBytes, templatePaths)) {
            return null;
        }
        Scene scene = prepareScene(screenshotBytes, region, driverFacade);
        if (scene == null) {
            return null;
        }

        try {
            for (String templatePath : templatePaths) {
                String templateName = new File(templatePath).getName();
                VisualElement match = withTemplate(templatePath, templateMat -> matchMultiScale(scene.mat(),
                        templateMat, templateName, confidenceThreshold, scene.offsetX(), scene.offsetY(),
                        scene.scaleFactor(), driverFacade));
                if (match != null) {
                    LOGGER.info("Successfully matched visual template '{}' at bounds {}", templateName, match);
                    return match;
                }
            }
        } finally {
            scene.mat().release();
        }

        return null;
    }

    public static List<VisualElement> findAllTemplateMatches(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, Driver driverFacade) {
        return findAllTemplateMatches(screenshotBytes, templatePaths, confidenceThreshold, null, driverFacade);
    }

    public static List<VisualElement> findAllTemplateMatches(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, com.znsio.teswiz.entities.VisualRegion region, Driver driverFacade) {
        if (!isSearchable(screenshotBytes, templatePaths)) {
            return Collections.emptyList();
        }
        Scene scene = prepareScene(screenshotBytes, region, driverFacade);
        if (scene == null) {
            return Collections.emptyList();
        }

        List<VisualElement> matches = new ArrayList<>();
        try {
            for (String templatePath : templatePaths) {
                String templateName = new File(templatePath).getName();
                List<VisualElement> templateMatches = withTemplate(templatePath, templateMat -> matchAllMultiScale(
                        scene.mat(), templateMat, templateName, confidenceThreshold, scene.offsetX(),
                        scene.offsetY(), scene.scaleFactor(), driverFacade));
                if (templateMatches != null) {
                    matches.addAll(templateMatches);
                }
            }
        } finally {
            scene.mat().release();
        }

        return applyNonMaximumSuppression(matches);
    }

    private static boolean isSearchable(byte[] screenshotBytes, List<String> templatePaths) {
        return isOpenCvAvailable() && screenshotBytes != null && templatePaths != null && !templatePaths.isEmpty();
    }

    /**
     * Decodes the screenshot, crops it to the optional region and converts it to an OpenCV matrix. The scale factor
     * MUST come from the FULL screenshot width (before cropping), so it stays correct on Retina/HiDPI regardless of the
     * region crop size. Returns {@code null} when the bytes are not a usable image; the caller releases the matrix.
     */
    private static Scene prepareScene(byte[] screenshotBytes, com.znsio.teswiz.entities.VisualRegion region,
            Driver driverFacade) {
        BufferedImage bufferedImage = parseScreenshot(screenshotBytes);
        if (bufferedImage == null) {
            return null;
        }
        double scaleFactor = (driverFacade != null) ? driverFacade.getViewportScaleFactor(bufferedImage.getWidth())
                : 1.0;

        int offsetX = 0;
        int offsetY = 0;
        if (region != null) {
            VisualRegionScaler.PixelRegion crop = VisualRegionScaler.scaleRegionToPixels(
                    region, bufferedImage.getWidth(), bufferedImage.getHeight(), scaleFactor);
            offsetX = crop.x();
            offsetY = crop.y();
            bufferedImage = bufferedImage.getSubimage(crop.x(), crop.y(), crop.width(), crop.height());
        }

        Mat sceneMat = bufferedImageToMat(bufferedImage);
        return sceneMat == null ? null : new Scene(sceneMat, offsetX, offsetY, scaleFactor);
    }

    /** Loads a template, applies the matcher and releases the template; {@code null} if the template is unusable. */
    private static <T> T withTemplate(String templatePath, Function<Mat, T> matcher) {
        Mat templateMat = loadTemplateMat(templatePath);
        if (templateMat == null) {
            return null;
        }
        try {
            return matcher.apply(templateMat);
        } finally {
            templateMat.release();
        }
    }

    /** The scene being searched (crop-relative), the crop's offset in the full screenshot, and the scale factor. */
    private record Scene(Mat mat, int offsetX, int offsetY, double scaleFactor) {
    }

    private static BufferedImage parseScreenshot(byte[] screenshotBytes) {
        try {
            return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(screenshotBytes));
        } catch (java.io.IOException e) {
            LOGGER.warn("Failed to parse screenshot bytes to BufferedImage: " + e.getMessage());
            return null;
        }
    }

    private static Mat bufferedImageToMat(BufferedImage image) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return loadSceneMat(bytes);
        } catch (java.io.IOException e) {
            LOGGER.warn("Failed to convert BufferedImage to Mat: " + e.getMessage());
            return null;
        }
    }

    private static Mat loadSceneMat(byte[] screenshotBytes) {
        Mat sceneMat = Imgcodecs.imdecode(new MatOfByte(screenshotBytes), Imgcodecs.IMREAD_COLOR);
        if (sceneMat.empty()) {
            LOGGER.warn("Failed to decode screenshot frame buffer for template matching.");
            return null;
        }
        return sceneMat;
    }

    private static Mat loadTemplateMat(String templatePath) {
        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            LOGGER.warn("Visual template file does not exist: " + templatePath);
            return null;
        }

        Mat templateMat = Imgcodecs.imread(templateFile.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
        if (templateMat.empty()) {
            LOGGER.warn("Failed to read image template file: " + templatePath);
            return null;
        }
        return templateMat;
    }

    private static VisualElement matchMultiScale(Mat scene, Mat template, String templateName, double threshold,
            int offsetX, int offsetY, double scaleFactor, Driver driverFacade) {
        double bestVal = -1.0;
        Point bestLoc = null;
        int bestW = 0;
        int bestH = 0;

        for (double scale : DEFAULT_SCALE_FACTORS) {
            int scaledW = (int) (template.cols() * scale);
            int scaledH = (int) (template.rows() * scale);
            if (isInvalidScaleDimensions(scaledW, scaledH, scene.cols(), scene.rows())) {
                continue;
            }
            Core.MinMaxLocResult mmr = bestMatchAtScale(scene, template, scaledW, scaledH);
            if (mmr.maxVal > bestVal) {
                bestVal = mmr.maxVal;
                bestLoc = mmr.maxLoc;
                bestW = scaledW;
                bestH = scaledH;
            }
        }

        if (bestVal >= threshold && bestLoc != null) {
            return buildMatchedVisualElement(bestLoc, bestW, bestH, templateName, bestVal, threshold,
                    offsetX, offsetY, scaleFactor, driverFacade);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Template '%s' best match score was %.4f (below threshold %.2f)", templateName, bestVal, threshold));
        }
        return null;
    }

    private static Core.MinMaxLocResult bestMatchAtScale(Mat scene, Mat template, int scaledW, int scaledH) {
        return withMatchResult(scene, template, scaledW, scaledH, Core::minMaxLoc);
    }

    private static List<VisualElement> matchAllMultiScale(Mat scene, Mat template, String templateName, double threshold,
            int offsetX, int offsetY, double scaleFactor, Driver driverFacade) {
        List<VisualElement> results = new ArrayList<>();
        for (double scale : DEFAULT_SCALE_FACTORS) {
            int scaledW = (int) (template.cols() * scale);
            int scaledH = (int) (template.rows() * scale);
            if (isInvalidScaleDimensions(scaledW, scaledH, scene.cols(), scene.rows())) {
                continue;
            }
            withMatchResult(scene, template, scaledW, scaledH, resultMat -> {
                collectMatchesAboveThreshold(resultMat, threshold, scaledW, scaledH, templateName, offsetX, offsetY,
                        scaleFactor, driverFacade, results);
                return null;
            });
        }
        return results;
    }

    /**
     * Resizes the template to the given size, matches it against the scene, and hands the score matrix to
     * {@code onResult}; both temporary matrices are always released.
     */
    private static <T> T withMatchResult(Mat scene, Mat template, int scaledW, int scaledH,
            Function<Mat, T> onResult) {
        Mat scaledTemplate = new Mat();
        try {
            Imgproc.resize(template, scaledTemplate, new Size(scaledW, scaledH));
            Mat resultMat = new Mat();
            try {
                Imgproc.matchTemplate(scene, scaledTemplate, resultMat, Imgproc.TM_CCOEFF_NORMED);
                return onResult.apply(resultMat);
            } finally {
                resultMat.release();
            }
        } finally {
            scaledTemplate.release();
        }
    }

    // Every score at or above the threshold becomes a match; duplicates are removed later by non-maximum suppression.
    private static void collectMatchesAboveThreshold(Mat resultMat, double threshold, int scaledW, int scaledH,
            String templateName, int offsetX, int offsetY, double scaleFactor, Driver driverFacade,
            List<VisualElement> results) {
        for (int y = 0; y < resultMat.rows(); y++) {
            for (int x = 0; x < resultMat.cols(); x++) {
                if (resultMat.get(y, x)[0] >= threshold) {
                    // Add the crop offset (physical px) then convert to logical/viewport px.
                    VisualRegionScaler.PixelRegion logical = VisualRegionScaler.toLogicalRegion(
                            x, y, scaledW, scaledH, offsetX, offsetY, scaleFactor);
                    results.add(new VisualElement(logical.x(), logical.y(), logical.width(),
                            logical.height(), templateName, driverFacade));
                }
            }
        }
    }

    private static boolean isInvalidScaleDimensions(int scaledW, int scaledH, int sceneWidth, int sceneHeight) {
        return scaledW > sceneWidth || scaledH > sceneHeight || scaledW < 5 || scaledH < 5;
    }

    private static VisualElement buildMatchedVisualElement(Point bestLoc, int bestW, int bestH, String templateName,
            double matchVal, double threshold, int offsetX, int offsetY, double scaleFactor, Driver driverFacade) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Template '%s' matched with score %.4f >= threshold %.2f", templateName, matchVal, threshold));
        }
        // bestLoc is crop-relative physical px. Add the crop offset, then convert to logical/viewport
        // px using the FULL-screenshot scale factor (passed in, not derived from the crop width).
        VisualRegionScaler.PixelRegion logical = VisualRegionScaler.toLogicalRegion(
                (int) bestLoc.x, (int) bestLoc.y, bestW, bestH, offsetX, offsetY, scaleFactor);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format("Template '%s' matched at crop bounds [x=%d, y=%d, w=%d, h=%d] (crop offset %d,%d) -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                templateName, (int) bestLoc.x, (int) bestLoc.y, bestW, bestH, offsetX, offsetY,
                logical.x(), logical.y(), logical.width(), logical.height(), scaleFactor));
        }
        return new VisualElement(logical.x(), logical.y(), logical.width(), logical.height(), templateName, driverFacade);
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
}
