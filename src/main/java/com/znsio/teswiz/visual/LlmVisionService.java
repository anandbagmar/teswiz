package com.znsio.teswiz.visual;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.imageio.ImageIO;

import org.json.JSONObject;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.context.SessionContext;
import com.znsio.teswiz.context.TestExecutionContext;
import com.znsio.teswiz.entities.TEST_CONTEXT;
import com.znsio.teswiz.entities.VisualRegion;
import com.znsio.teswiz.runner.Driver;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.runner.VisualElement;
import com.znsio.teswiz.services.ApiService;

/**
 * Default {@link LlmVisionEngine}: a last-resort element locator that asks a vision-capable LLM (OpenAI-compatible chat
 * completions wire format) for the on-screen bounding box of an element described in natural language and/or shown in
 * one or more reference/example images.
 *
 * <p>
 * HTTP calls reuse the framework's configured {@code API_ENGINE} via {@link ApiService}, so no new HTTP client is
 * introduced. The call is retried up to {@code LLM_VISION_MAX_RETRIES} times on transient failures; the per-call
 * network deadline ({@code LLM_VISION_TIMEOUT_SECONDS}) is applied via the Playwright engine's timeout property for the
 * duration of the call. Returned pixel coordinates are bounds-checked and scaled to logical viewport coordinates
 * (matching {@link OcrService} / {@link ImageRecognitionService}). Token usage is logged per request, accumulated per
 * test, and summarised per run.
 *
 * <p>
 * This class holds no secrets in logs: the {@code Authorization} header is redacted before any traffic file is written.
 */
public class LlmVisionService implements LlmVisionEngine {

    private static final Logger LOGGER = LogManager.getLogger(LlmVisionService.class.getName());
    private static final String DEFAULT_OPENAI_MODEL = "gpt-4o";
    private static final String REDACTED = "***REDACTED***";
    private static final String TIMEOUT_PROPERTY = "TESWIZ_API_TIMEOUT_SECONDS";

    private static final String SYSTEM_PROMPT = "You are a precise UI element locator. The FIRST image is the SCREENSHOT to search. Any "
            + "additional images are REFERENCE EXAMPLES showing what the target element looks like - "
            + "they are hints only and are NOT part of the screenshot. Find the element in the "
            + "SCREENSHOT (guided by the description and/or the reference examples) and return ONLY a "
            + "compact JSON object with its bounding box in pixel coordinates of the SCREENSHOT, using "
            + "integer pixels with the origin at the top-left corner. The box must refer to the "
            + "SCREENSHOT, never to a reference image. Schema: {\"found\": boolean, \"x\": int, "
            + "\"y\": int, \"width\": int, \"height\": int}. If you cannot confidently find the element, "
            + "return {\"found\": false}. Do not include any explanation, markdown, or extra keys.";

    private static final String READ_SYSTEM_PROMPT = "You are a precise UI value reader. The image is a SCREENSHOT. Read and return ONLY the exact text or "
            + "value shown for the target the user describes - transcribe it verbatim as it appears on screen. "
            + "Return just the value as plain text, with no label, no quotes, no explanation, and no markdown. If "
            + "the target is not visible or you cannot read it, return an empty string.";

    /**
     * Returns {@code true} when the fallback is enabled and the minimum configuration (endpoint + API key env var
     * present) is satisfied, mirroring {@code OcrService.isTess4jAvailable()}.
     */
    public static boolean isAvailable() {
        if (!Runner.isLlmVisionFallbackEnabled()) {
            return false;
        }
        return !Runner.getLlmVisionEndpoint().isBlank() && resolveApiKey() != null;
    }

    @Override
    public VisualElement findByDescription(byte[] screenshotBytes, String description, VisualRegion region,
            Driver driverFacade) {
        return findByDescription(screenshotBytes, description, null, region, driverFacade);
    }

    @Override
    public VisualElement findByDescription(byte[] screenshotBytes, String description, List<String> referenceImagePaths,
            VisualRegion region, Driver driverFacade) {
        boolean hasDescription = description != null && !description.isBlank();
        boolean hasReferences = referenceImagePaths != null && !referenceImagePaths.isEmpty();
        if (!isAvailable() || screenshotBytes == null || (!hasDescription && !hasReferences)) {
            return null;
        }

        BufferedImage image = parseScreenshot(screenshotBytes);
        if (image == null) {
            return null;
        }

        int offsetX = 0;
        int offsetY = 0;
        BufferedImage searchImage = image;
        if (region != null) {
            offsetX = Math.max(0, Math.min(region.getX(), image.getWidth() - 1));
            offsetY = Math.max(0, Math.min(region.getY(), image.getHeight() - 1));
            int w = Math.min(region.getWidth(), image.getWidth() - offsetX);
            int h = Math.min(region.getHeight(), image.getHeight() - offsetY);
            if (w > 0 && h > 0) {
                searchImage = image.getSubimage(offsetX, offsetY, w, h);
            }
        }

        byte[] searchImageBytes = encodePng(searchImage);
        if (searchImageBytes == null) {
            return null;
        }

        List<byte[]> referenceImages = loadReferenceImages(referenceImagePaths);
        String effectiveDescription = hasDescription ? description : "the element shown in the reference image(s)";

        LlmLocation location = requestLocationWithRetries(searchImageBytes, referenceImages, effectiveDescription);
        if (location == null || !location.found) {
            return null;
        }

        VisualElement element = buildVisualElement(location, searchImage.getWidth(), searchImage.getHeight(), offsetX,
                offsetY, effectiveDescription, driverFacade);
        if (element != null) {
            saveCaptureIfEnabled(searchImage, location, effectiveDescription);
        }
        return element;
    }

    @Override
    public String readTextByDescription(byte[] screenshotBytes, String description, VisualRegion region,
            Driver driverFacade) {
        boolean hasDescription = description != null && !description.isBlank();
        if (!isAvailable() || screenshotBytes == null || !hasDescription) {
            return "";
        }

        BufferedImage image = parseScreenshot(screenshotBytes);
        if (image == null) {
            return "";
        }

        BufferedImage searchImage = cropToRegion(image, region);
        byte[] searchImageBytes = encodePng(searchImage);
        if (searchImageBytes == null) {
            return "";
        }

        String value = requestTextWithRetries(searchImageBytes, description);
        LOGGER.info("LLM vision read for '{}' within region {} returned: '{}'", description,
                region != null ? region : "[entire screen]", value);
        return value;
    }

    /**
     * Crops the screenshot to {@code region} (bounds-checked); returns the whole image when {@code region} is
     * {@code null} or degenerate.
     */
    private BufferedImage cropToRegion(BufferedImage image, VisualRegion region) {
        if (region == null) {
            return image;
        }
        int offsetX = Math.max(0, Math.min(region.getX(), image.getWidth() - 1));
        int offsetY = Math.max(0, Math.min(region.getY(), image.getHeight() - 1));
        int w = Math.min(region.getWidth(), image.getWidth() - offsetX);
        int h = Math.min(region.getHeight(), image.getHeight() - offsetY);
        return (w > 0 && h > 0) ? image.getSubimage(offsetX, offsetY, w, h) : image;
    }

    /**
     * Asks the vision model to READ the value of the described target, retrying transient failures up to
     * {@code LLM_VISION_MAX_RETRIES}. Surfaces the model's trimmed textual content; returns an empty string when every
     * attempt fails or the model returns nothing.
     */
    private String requestTextWithRetries(byte[] screenshotBytes, String description) {
        int maxRetries = Runner.getLlmVisionMaxRetries();
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return requestText(screenshotBytes, description, attempt, maxRetries);
            } catch (RuntimeException e) {
                lastError = e;
                LOGGER.warn("LLM vision read attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
            }
        }
        if (lastError != null) {
            LOGGER.error("LLM vision read failed after {} attempt(s): {}", maxRetries, lastError.getMessage());
        }
        return "";
    }

    private String requestText(byte[] screenshotBytes, String description, int attempt, int maxRetries) {
        LlmVisionProvider provider = resolveProvider();
        String model = resolveModel();
        String apiKeyOrToken = resolveApiKeyOrToken(provider);

        String endpoint = provider.resolveEndpoint(Runner.getLlmVisionEndpoint(), model);
        String userText = "Read and return the text/value of this target in the SCREENSHOT: " + description;
        String requestBody = provider.buildRequestBody(READ_SYSTEM_PROMPT, userText, screenshotBytes,
                Collections.emptyList(), model);
        Map<String, String> headers = provider.buildHeaders(apiKeyOrToken);

        long startedAt = System.currentTimeMillis();
        TeswizApiResponse response = postWithTimeout(endpoint, requestBody, headers);
        long durationMs = System.currentTimeMillis() - startedAt;

        int status = response.getStatusCode();
        String responseBody = response.getResponseBody();

        recordTrafficIfEnabled(endpoint, requestBody, headers, status, response.getHeaders(), responseBody, attempt,
                maxRetries);

        if (status < 200 || status >= 300) {
            throw new IllegalStateException("LLM vision endpoint returned HTTP " + status);
        }

        ParsedLlmResponse parsed = provider.parseResponse(responseBody);
        String value = (parsed == null || parsed.content() == null) ? "" : parsed.content().trim();
        recordAndLogReadUsage(parsed, description, status, durationMs, value);
        return value;
    }

    private void recordAndLogReadUsage(ParsedLlmResponse parsed, String description, int status, long durationMs,
            String value) {
        long promptTokens = parsed == null ? 0 : parsed.promptTokens();
        long completionTokens = parsed == null ? 0 : parsed.completionTokens();
        long totalTokens = parsed == null ? 0 : parsed.totalTokens();

        accumulateUsage(promptTokens, completionTokens, totalTokens, durationMs);

        String model = Runner.getLlmVisionModel();
        LOGGER.info(
                "LLM vision read request: desc='{}', model={}, status={}, tokens[prompt={}, completion={}, total={}], durationMs={}, read='{}'",
                description, model.isBlank() ? DEFAULT_OPENAI_MODEL : model, status, promptTokens, completionTokens,
                totalTokens, durationMs, value);
    }

    /**
     * When {@code LLM_VISION_SAVE_CAPTURES=true}, crops the located region from the (region-adjusted) screenshot at the
     * raw pixel box and saves it as a reusable OpenCV template under the scenario's {@code llm-vision-captures} folder,
     * logging the saved path plus a suggested library name/path. This is a harvest-and-report step for manual
     * promotion; it never auto-reuses captures.
     */
    private void saveCaptureIfEnabled(BufferedImage searchImage, LlmLocation location, String description) {
        if (!Runner.isLlmVisionSaveCapturesEnabled()) {
            return;
        }
        try {
            int index = nextCaptureIndex();
            LlmVisionCaptureWriter.CaptureResult result = new LlmVisionCaptureWriter().saveCapture(searchImage,
                    location.x, location.y, location.width, location.height, description, index);
            recordCapture(description, result.savedPath(), result.suggestedFileName());
            LOGGER.info(System.lineSeparator() + "LLM vision capture: desc='{}'" + System.lineSeparator()
                    + "  saved to          : {}" + System.lineSeparator()
                    + "  suggested name    : {}  (suggestion only - rename as you prefer)" + System.lineSeparator()
                    + "  suggested location: <your-image-template-dir>/{}" + System.lineSeparator()
                    + "                      (e.g. src/test/resources/images/{})" + System.lineSeparator()
                    + "  to reuse & skip the LLM next time: review this crop, copy it into your image"
                    + System.lineSeparator() + "  template directory, then locate via" + System.lineSeparator()
                    + "  driver.vision(VisionStrategy.OCR_THEN_LLM).findByImage(List.of(\"<your-image-template-dir>/{}\"))",
                    description, result.savedPath(), result.suggestedFileName(), result.suggestedFileName(),
                    result.suggestedFileName(), result.suggestedFileName());
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to save LLM vision capture for '{}': {}", description, e.getMessage());
        }
    }

    private int nextCaptureIndex() {
        try {
            TestExecutionContext context = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
            if (context == null) {
                return 1;
            }
            Object existing = context.getTestState(TEST_CONTEXT.LLM_VISION_USAGE);
            LlmVisionUsage usage = (existing instanceof LlmVisionUsage) ? (LlmVisionUsage) existing : null;
            return usage != null ? usage.getCaptureCount() + 1 : 1;
        } catch (RuntimeException e) {
            return 1;
        }
    }

    private void recordCapture(String description, String savedPath, String suggestedName) {
        try {
            TestExecutionContext context = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
            if (context == null) {
                return;
            }
            Object existing = context.getTestState(TEST_CONTEXT.LLM_VISION_USAGE);
            LlmVisionUsage usage = (existing instanceof LlmVisionUsage) ? (LlmVisionUsage) existing
                    : new LlmVisionUsage();
            usage.recordCapture(description, savedPath, suggestedName);
            context.addTestState(TEST_CONTEXT.LLM_VISION_USAGE, usage);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not record LLM vision capture: {}", e.getMessage());
        }
    }

    private List<byte[]> loadReferenceImages(List<String> referenceImagePaths) {
        if (referenceImagePaths == null || referenceImagePaths.isEmpty()) {
            return Collections.emptyList();
        }
        List<byte[]> images = new ArrayList<>();
        for (String path : referenceImagePaths) {
            if (path == null || path.isBlank()) {
                continue;
            }
            File file = new File(path);
            if (!file.exists()) {
                LOGGER.warn("LLM vision reference image does not exist, skipping: {}", path);
                continue;
            }
            try {
                images.add(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                LOGGER.warn("Failed to read LLM vision reference image '{}': {}", path, e.getMessage());
            }
        }
        return images;
    }

    // --- Request / retry ---------------------------------------------------

    private LlmLocation requestLocationWithRetries(byte[] screenshotBytes, List<byte[]> referenceImages,
            String description) {
        int maxRetries = Runner.getLlmVisionMaxRetries();
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return requestLocation(screenshotBytes, referenceImages, description, attempt, maxRetries);
            } catch (RuntimeException e) {
                lastError = e;
                LOGGER.warn("LLM vision request attempt {}/{} failed: {}", attempt, maxRetries, e.getMessage());
            }
        }
        if (lastError != null) {
            LOGGER.error("LLM vision request failed after {} attempt(s): {}", maxRetries, lastError.getMessage());
        }
        return null;
    }

    private LlmLocation requestLocation(byte[] screenshotBytes, List<byte[]> referenceImages, String description,
            int attempt, int maxRetries) {
        LlmVisionProvider provider = resolveProvider();
        String model = resolveModel();
        String apiKeyOrToken = resolveApiKeyOrToken(provider);

        String endpoint = provider.resolveEndpoint(Runner.getLlmVisionEndpoint(), model);
        String userText = "Locate this element and return its bounding box in the SCREENSHOT: " + description;
        String requestBody = provider.buildRequestBody(SYSTEM_PROMPT, userText, screenshotBytes, referenceImages,
                model);
        Map<String, String> headers = provider.buildHeaders(apiKeyOrToken);

        long startedAt = System.currentTimeMillis();
        TeswizApiResponse response = postWithTimeout(endpoint, requestBody, headers);
        long durationMs = System.currentTimeMillis() - startedAt;

        int status = response.getStatusCode();
        String responseBody = response.getResponseBody();

        recordTrafficIfEnabled(endpoint, requestBody, headers, status, response.getHeaders(), responseBody, attempt,
                maxRetries);

        if (status < 200 || status >= 300) {
            throw new IllegalStateException("LLM vision endpoint returned HTTP " + status);
        }

        ParsedLlmResponse parsed = provider.parseResponse(responseBody);
        LlmLocation location = parseLocation(parsed);
        recordAndLogUsage(parsed, description, referenceImages.size(), status, durationMs, location);
        return location;
    }

    private LlmVisionProvider resolveProvider() {
        return LlmVisionProviderFactory.create(Runner.getLlmVisionProvider(), Runner.getLlmVisionApiVersion(),
                resolveApiKey(), new GoogleAdcAccessTokenProvider());
    }

    private String resolveModel() {
        String model = Runner.getLlmVisionModel();
        return model.isBlank() ? DEFAULT_OPENAI_MODEL : model;
    }

    /**
     * Resolves the credential handed to the provider's headers: Vertex AI uses a short-lived GCP OAuth token, every
     * other provider uses the API key from the configured env var.
     */
    private String resolveApiKeyOrToken(LlmVisionProvider provider) {
        if (provider instanceof VertexGeminiLlmVisionProvider vertexProvider) {
            return vertexProvider.resolveAccessToken();
        }
        return resolveApiKey();
    }

    private TeswizApiResponse postWithTimeout(String endpoint, String requestBody, Map<String, String> headers) {
        int timeoutSeconds = Runner.getLlmVisionTimeoutSeconds();
        String previous = System.getProperty(TIMEOUT_PROPERTY);
        System.setProperty(TIMEOUT_PROPERTY, String.valueOf(timeoutSeconds));
        try {
            return ApiService.post(endpoint, requestBody, headers);
        } finally {
            if (previous == null) {
                System.clearProperty(TIMEOUT_PROPERTY);
            } else {
                System.setProperty(TIMEOUT_PROPERTY, previous);
            }
        }
    }

    // --- Response parsing --------------------------------------------------

    private LlmLocation parseLocation(ParsedLlmResponse parsed) {
        if (parsed == null) {
            return null;
        }
        String content = parsed.content();
        if (content == null || content.isBlank()) {
            return null;
        }
        JSONObject box = extractJsonObject(content);
        if (box == null) {
            return null;
        }
        LlmLocation location = new LlmLocation();
        location.found = box.optBoolean("found", false);
        location.x = box.optInt("x", -1);
        location.y = box.optInt("y", -1);
        location.width = box.optInt("width", -1);
        location.height = box.optInt("height", -1);
        return location;
    }

    // Package-private + static for unit testing: parses the first {...} JSON object out of raw LLM
    // content (which may include surrounding prose or markdown fences). Returns null when absent/invalid.
    static JSONObject extractJsonObject(String content) {
        String trimmed = content.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return new JSONObject(trimmed.substring(start, end + 1));
        } catch (RuntimeException e) {
            LOGGER.warn("LLM vision content was not valid JSON: {}", e.getMessage());
            return null;
        }
    }

    // --- Result building ---------------------------------------------------

    private VisualElement buildVisualElement(LlmLocation location, int imageWidth, int imageHeight, int offsetX,
            int offsetY, String description, Driver driverFacade) {
        if (!isWithinBounds(location, imageWidth, imageHeight)) {
            LOGGER.warn("LLM vision returned out-of-bounds box [x={}, y={}, w={}, h={}] for image {}x{}; ignoring.",
                    location.x, location.y, location.width, location.height, imageWidth, imageHeight);
            return null;
        }

        double scaleFactor = (driverFacade != null) ? driverFacade.getViewportScaleFactor(imageWidth) : 1.0;
        int logicalX = (int) Math.round((location.x + offsetX) / scaleFactor);
        int logicalY = (int) Math.round((location.y + offsetY) / scaleFactor);
        int logicalW = (int) Math.round(location.width / scaleFactor);
        int logicalH = (int) Math.round(location.height / scaleFactor);

        VisualElement element = new VisualElement(logicalX, logicalY, logicalW, logicalH, "LLM: " + description,
                driverFacade);
        LOGGER.info("LLM vision located '{}' at viewport bounds [x={}, y={}, w={}, h={}] (scaleFactor: {})",
                description, logicalX, logicalY, logicalW, logicalH, scaleFactor);
        return element;
    }

    private boolean isWithinBounds(LlmLocation location, int imageWidth, int imageHeight) {
        if (location.x < 0 || location.y < 0 || location.width <= 0 || location.height <= 0) {
            return false;
        }
        return location.x + location.width <= imageWidth && location.y + location.height <= imageHeight;
    }

    // --- Usage + traffic logging -------------------------------------------

    private void recordAndLogUsage(ParsedLlmResponse parsed, String description, int referenceImageCount, int status,
            long durationMs, LlmLocation location) {
        long promptTokens = parsed == null ? 0 : parsed.promptTokens();
        long completionTokens = parsed == null ? 0 : parsed.completionTokens();
        long totalTokens = parsed == null ? 0 : parsed.totalTokens();

        accumulateUsage(promptTokens, completionTokens, totalTokens, durationMs);

        String result = (location != null && location.found) ? "FOUND" : "NOT_FOUND";
        String model = Runner.getLlmVisionModel();
        LOGGER.info(
                "LLM vision request: desc='{}', referenceImages={}, model={}, status={}, tokens[prompt={}, completion={}, total={}], durationMs={}, result={}",
                description, referenceImageCount, model.isBlank() ? DEFAULT_OPENAI_MODEL : model, status, promptTokens,
                completionTokens, totalTokens, durationMs, result);
    }

    private void accumulateUsage(long promptTokens, long completionTokens, long totalTokens, long durationMs) {
        try {
            TestExecutionContext context = SessionContext.getTestExecutionContext(Thread.currentThread().getId());
            if (context == null) {
                return;
            }
            Object existing = context.getTestState(TEST_CONTEXT.LLM_VISION_USAGE);
            LlmVisionUsage usage = (existing instanceof LlmVisionUsage) ? (LlmVisionUsage) existing
                    : new LlmVisionUsage();
            usage.record(promptTokens, completionTokens, totalTokens, durationMs);
            context.addTestState(TEST_CONTEXT.LLM_VISION_USAGE, usage);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not accumulate LLM vision usage: {}", e.getMessage());
        }
    }

    private void recordTrafficIfEnabled(String endpoint, String requestBody, Map<String, String> headers, int status,
            Map<String, String> responseHeaders, String responseBody, int attempt, int maxRetries) {
        if (!Runner.isLlmVisionApiTrafficLoggingEnabled()) {
            return;
        }
        try {
            String maskedHeaders = redactAuthorization(headers);
            String elidedBody = redactUrlApiKeys(elideImageData(requestBody));
            String redactedEndpoint = redactUrlApiKeys(endpoint);
            String attemptLabel = maxRetries > 1 ? String.format(" [Attempt %d/%d]", attempt, maxRetries) : "";
            String relativePath = new LlmVisionTrafficRecorder().record("POST", redactedEndpoint + attemptLabel,
                    maskedHeaders, elidedBody, status, responseHeaders == null ? "{}" : responseHeaders.toString(),
                    responseBody);
            LOGGER.info("LLM vision call POST {} -> {} | detail: {}", redactedEndpoint, status, relativePath);
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to record llm-vision-traffic for {}: {}", endpoint, e.getMessage());
        }
    }

    private String redactAuthorization(Map<String, String> headers) {
        Map<String, String> copy = new HashMap<>(headers);
        copy.replaceAll((key, value) -> isSensitiveHeader(key) ? REDACTED : value);
        return copy.toString();
    }

    private boolean isSensitiveHeader(String key) {
        return key.equalsIgnoreCase("Authorization") || key.equalsIgnoreCase("api-key")
                || key.equalsIgnoreCase("x-api-key");
    }

    /**
     * Redacts a {@code key=<value>} API key carried in a URL query string (e.g. Gemini AI Studio), so recorded
     * endpoints and bodies never leak the key.
     */
    private String redactUrlApiKeys(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("key=[^&\\s]+", "key=" + REDACTED);
    }

    private String elideImageData(String requestBody) {
        // Each base64 image blob (screenshot + any reference images) is huge and noisy; replace each
        // with a short placeholder carrying its decoded byte size, so the record stays readable while
        // still showing how many images were sent.
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("data:image/png;base64,([A-Za-z0-9+/=]+)")
                .matcher(requestBody);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            int approxBytes = (int) (matcher.group(1).length() * 0.75);
            matcher.appendReplacement(result, java.util.regex.Matcher
                    .quoteReplacement("data:image/png;base64,<~" + approxBytes + " bytes elided>"));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    // --- Helpers -----------------------------------------------------------

    private static String resolveApiKey() {
        String envName = Runner.getLlmVisionApiKeyEnvName();
        String value = System.getenv(envName);
        if (value == null || value.isBlank()) {
            // Allow a system-property override for local runs / tests.
            value = System.getProperty(envName);
        }
        return (value == null || value.isBlank()) ? null : value;
    }

    private BufferedImage parseScreenshot(byte[] screenshotBytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(screenshotBytes));
        } catch (IOException e) {
            LOGGER.warn("Failed to parse screenshot for LLM vision: {}", e.getMessage());
            return null;
        }
    }

    private byte[] encodePng(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            LOGGER.warn("Failed to encode image for LLM vision: {}", e.getMessage());
            return null;
        }
    }

    private static final class LlmLocation {
        private boolean found;
        private int x;
        private int y;
        private int width;
        private int height;
    }
}
