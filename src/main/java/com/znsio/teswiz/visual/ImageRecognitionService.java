package com.znsio.teswiz.visual;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

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
        if (!isOpenCvAvailable() || screenshotBytes == null || templatePaths == null || templatePaths.isEmpty()) {
            return null;
        }

        Mat sceneMat = loadSceneMat(screenshotBytes);
        if (sceneMat == null) {
            return null;
        }

        try {
            for (String templatePath : templatePaths) {
                Mat templateMat = loadTemplateMat(templatePath);
                if (templateMat == null) {
                    continue;
                }

                try {
                    String templateName = new File(templatePath).getName();
                    VisualElement match = matchMultiScale(sceneMat, templateMat, templateName, confidenceThreshold, driverFacade);
                    if (match != null) {
                        LOGGER.info(String.format("Successfully matched visual template '%s' at bounds %s", templateName, match));
                        return match;
                    }
                } finally {
                    templateMat.release();
                }
            }
        } finally {
            sceneMat.release();
        }

        return null;
    }

    public static List<VisualElement> findAllTemplateMatches(byte[] screenshotBytes, List<String> templatePaths, double confidenceThreshold, Driver driverFacade) {
        if (!isOpenCvAvailable() || screenshotBytes == null || templatePaths == null || templatePaths.isEmpty()) {
            return Collections.emptyList();
        }

        Mat sceneMat = loadSceneMat(screenshotBytes);
        if (sceneMat == null) {
            return Collections.emptyList();
        }

        List<VisualElement> matches = new ArrayList<>();
        try {
            for (String templatePath : templatePaths) {
                Mat templateMat = loadTemplateMat(templatePath);
                if (templateMat == null) {
                    continue;
                }

                try {
                    String templateName = new File(templatePath).getName();
                    List<VisualElement> templateMatches = matchAllMultiScale(sceneMat, templateMat, templateName, confidenceThreshold, driverFacade);
                    matches.addAll(templateMatches);
                } finally {
                    templateMat.release();
                }
            }
        } finally {
            sceneMat.release();
        }

        return applyNonMaximumSuppression(matches);
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

    private static VisualElement matchMultiScale(Mat scene, Mat template, String templateName, double threshold, Driver driverFacade) {
        double bestVal = -1.0;
        Point bestLoc = null;
        int bestW = 0;
        int bestH = 0;
        int sceneWidth = scene.cols();
        int sceneHeight = scene.rows();

        for (double scale : DEFAULT_SCALE_FACTORS) {
            int scaledW = (int) (template.cols() * scale);
            int scaledH = (int) (template.rows() * scale);

            if (isInvalidScaleDimensions(scaledW, scaledH, sceneWidth, sceneHeight)) {
                continue;
            }

            Mat scaledTemplate = new Mat();
            try {
                Imgproc.resize(template, scaledTemplate, new Size(scaledW, scaledH));
                Mat result = new Mat();
                try {
                    Imgproc.matchTemplate(scene, scaledTemplate, result, Imgproc.TM_CCOEFF_NORMED);
                    Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

                    if (mmr.maxVal > bestVal) {
                        bestVal = mmr.maxVal;
                        bestLoc = mmr.maxLoc;
                        bestW = scaledW;
                        bestH = scaledH;
                    }
                } finally {
                    result.release();
                }
            } finally {
                scaledTemplate.release();
            }
        }

        if (bestVal >= threshold && bestLoc != null) {
            return buildMatchedVisualElement(bestLoc, bestW, bestH, templateName, sceneWidth, bestVal, threshold, driverFacade);
        }

        LOGGER.debug(String.format("Template '%s' best match score was %.4f (below threshold %.2f)", templateName, bestVal, threshold));
        return null;
    }

    private static List<VisualElement> matchAllMultiScale(Mat scene, Mat template, String templateName, double threshold, Driver driverFacade) {
        List<VisualElement> results = new ArrayList<>();
        int sceneWidth = scene.cols();
        int sceneHeight = scene.rows();

        for (double scale : DEFAULT_SCALE_FACTORS) {
            int scaledW = (int) (template.cols() * scale);
            int scaledH = (int) (template.rows() * scale);

            if (isInvalidScaleDimensions(scaledW, scaledH, sceneWidth, sceneHeight)) {
                continue;
            }

            Mat scaledTemplate = new Mat();
            try {
                Imgproc.resize(template, scaledTemplate, new Size(scaledW, scaledH));
                Mat resultMat = new Mat();
                try {
                    Imgproc.matchTemplate(scene, scaledTemplate, resultMat, Imgproc.TM_CCOEFF_NORMED);
                    double scaleFactor = (driverFacade != null) ? driverFacade.getViewportScaleFactor(sceneWidth) : 1.0;

                    for (int y = 0; y < resultMat.rows(); y++) {
                        for (int x = 0; x < resultMat.cols(); x++) {
                            double matchVal = resultMat.get(y, x)[0];
                            if (matchVal >= threshold) {
                                int logicalX = (int) Math.round(x / scaleFactor);
                                int logicalY = (int) Math.round(y / scaleFactor);
                                int logicalW = (int) Math.round(scaledW / scaleFactor);
                                int logicalH = (int) Math.round(scaledH / scaleFactor);
                                results.add(new VisualElement(logicalX, logicalY, logicalW, logicalH, templateName, driverFacade));
                            }
                        }
                    }
                } finally {
                    resultMat.release();
                }
            } finally {
                scaledTemplate.release();
            }
        }
        return results;
    }

    private static boolean isInvalidScaleDimensions(int scaledW, int scaledH, int sceneWidth, int sceneHeight) {
        return scaledW > sceneWidth || scaledH > sceneHeight || scaledW < 5 || scaledH < 5;
    }

    private static VisualElement buildMatchedVisualElement(Point bestLoc, int bestW, int bestH, String templateName, int sceneWidth, double matchVal, double threshold, Driver driverFacade) {
        LOGGER.info(String.format("Template '%s' matched with score %.4f >= threshold %.2f", templateName, matchVal, threshold));
        double scaleFactor = (driverFacade != null) ? driverFacade.getViewportScaleFactor(sceneWidth) : 1.0;
        int logicalX = (int) Math.round(bestLoc.x / scaleFactor);
        int logicalY = (int) Math.round(bestLoc.y / scaleFactor);
        int logicalW = (int) Math.round(bestW / scaleFactor);
        int logicalH = (int) Math.round(bestH / scaleFactor);
        LOGGER.info(String.format("Template '%s' matched at screenshot bounds [x=%d, y=%d, w=%d, h=%d] -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                templateName, (int) bestLoc.x, (int) bestLoc.y, bestW, bestH, logicalX, logicalY, logicalW, logicalH, scaleFactor));
        return new VisualElement(logicalX, logicalY, logicalW, logicalH, templateName, driverFacade);
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
