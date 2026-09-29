package com.znsio.teswiz.visual;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.VisualElement;

public class ImageRecognitionService {
    private static final Logger LOGGER = LogManager.getLogger(ImageRecognitionService.class.getName());
    private static boolean openCvNativeLoaded = false;

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

        Mat sceneMat = Imgcodecs.imdecode(new MatOfByte(screenshotBytes), Imgcodecs.IMREAD_COLOR);
        if (sceneMat.empty()) {
            LOGGER.warn("Failed to decode screenshot frame buffer for template matching.");
            return null;
        }

        try {
            for (String templatePath : templatePaths) {
                File templateFile = new File(templatePath);
                if (!templateFile.exists()) {
                    LOGGER.warn("Visual template file does not exist: " + templatePath);
                    continue;
                }

                Mat templateMat = Imgcodecs.imread(templateFile.getAbsolutePath(), Imgcodecs.IMREAD_COLOR);
                if (templateMat.empty()) {
                    LOGGER.warn("Failed to read image template file: " + templatePath);
                    continue;
                }

                try {
                    VisualElement match = matchMultiScale(sceneMat, templateMat, templateFile.getName(), confidenceThreshold, driverFacade);
                    if (match != null) {
                        LOGGER.info(String.format("Successfully matched visual template '%s' at bounds %s", templateFile.getName(), match));
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

    private static VisualElement matchMultiScale(Mat scene, Mat template, String templateName, double threshold, Driver driverFacade) {
        double[] scaleFactors = {1.0, 0.75, 0.5, 1.25, 1.5, 2.0};
        double bestVal = -1.0;
        Point bestLoc = null;
        int bestW = 0;
        int bestH = 0;

        int sceneWidth = scene.cols();
        int sceneHeight = scene.rows();

        for (double scale : scaleFactors) {
            int scaledW = (int) (template.cols() * scale);
            int scaledH = (int) (template.rows() * scale);

            if (scaledW > sceneWidth || scaledH > sceneHeight || scaledW < 5 || scaledH < 5) {
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
            LOGGER.info(String.format("Template '%s' matched with score %.4f >= threshold %.2f", templateName, bestVal, threshold));
            double scaleFactor = 1.0;
            if (driverFacade != null) {
                scaleFactor = driverFacade.getViewportScaleFactor(sceneWidth);
            }
            int logicalX = (int) Math.round(bestLoc.x / scaleFactor);
            int logicalY = (int) Math.round(bestLoc.y / scaleFactor);
            int logicalW = (int) Math.round(bestW / scaleFactor);
            int logicalH = (int) Math.round(bestH / scaleFactor);
            LOGGER.info(String.format("Template '%s' matched at screenshot bounds [x=%d, y=%d, w=%d, h=%d] -> viewport bounds [x=%d, y=%d, w=%d, h=%d] (scaleFactor: %.2f)",
                    templateName, (int) bestLoc.x, (int) bestLoc.y, bestW, bestH, logicalX, logicalY, logicalW, logicalH, scaleFactor));
            return new VisualElement(logicalX, logicalY, logicalW, logicalH, templateName, driverFacade);
        }

        LOGGER.debug(String.format("Template '%s' best match score was %.4f (below threshold %.2f)", templateName, bestVal, threshold));
        return null;
    }
}
