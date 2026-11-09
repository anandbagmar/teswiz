package com.znsio.teswiz.web.playwright;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.exceptions.InvalidTestDataException;

/**
 * Finds the Node worker script that runs the playwright-ts engine.
 *
 * <p>A project's own {@code playwright/worker.mjs} wins, so the teswiz repository and any project
 * that keeps a customised copy behave exactly as before. Otherwise the worker bundled in the teswiz
 * jar is extracted to {@code build/teswiz/playwright/} inside the project. That location matters:
 * Node resolves the worker's {@code playwright} and {@code @applitools/eyes-playwright} imports by
 * walking up from the script, so it finds the project's own {@code node_modules}.
 */
public class PlaywrightWorkerScriptResolver {
    static final String BUNDLED_WORKER_RESOURCE_DIR = "teswiz/playwright/";
    private static final Logger LOGGER = LogManager.getLogger(PlaywrightWorkerScriptResolver.class.getName());
    private static final String WORKER_SCRIPT = "worker.mjs";
    private static final List<String> BUNDLED_WORKER_FILES = List.of(WORKER_SCRIPT, "worker-provider.mjs");
    private static final Path PROJECT_WORKER = Path.of("playwright", WORKER_SCRIPT);
    private static final Path EXTRACTED_WORKER_DIR = Path.of("build", "teswiz", "playwright");

    private final String bundledResourceDir;

    public PlaywrightWorkerScriptResolver() {
        this(BUNDLED_WORKER_RESOURCE_DIR);
    }

    PlaywrightWorkerScriptResolver(String bundledResourceDir) {
        this.bundledResourceDir = bundledResourceDir;
    }

    public Path resolve(Path projectRoot) {
        Path projectWorker = projectRoot.resolve(PROJECT_WORKER);
        if (Files.isRegularFile(projectWorker)) {
            return projectWorker;
        }
        if (!isBundled(WORKER_SCRIPT)) {
            throw missingWorker(projectWorker);
        }
        return extractBundledWorker(projectRoot.resolve(EXTRACTED_WORKER_DIR));
    }

    private Path extractBundledWorker(Path targetDir) {
        try {
            Files.createDirectories(targetDir);
            for (String fileName : BUNDLED_WORKER_FILES) {
                writeIfChanged(targetDir.resolve(fileName), readBundled(fileName));
            }
        } catch (IOException e) {
            throw new InvalidTestDataException(
                    String.format("Unable to extract the bundled Playwright worker to '%s'", targetDir), e);
        }
        Path worker = targetDir.resolve(WORKER_SCRIPT);
        LOGGER.info("Using the Playwright worker bundled in the teswiz jar: '{}'", worker);
        return worker;
    }

    private static void writeIfChanged(Path target, byte[] content) throws IOException {
        if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), content)) {
            return;
        }
        Files.write(target, content);
    }

    private boolean isBundled(String fileName) {
        return null != getClass().getClassLoader().getResource(bundledResourceDir + fileName);
    }

    private byte[] readBundled(String fileName) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(bundledResourceDir + fileName)) {
            if (null == stream) {
                throw new IOException("Missing bundled resource " + bundledResourceDir + fileName);
            }
            return stream.readAllBytes();
        }
    }

    private static InvalidTestDataException missingWorker(Path projectWorker) {
        return new InvalidTestDataException(String.format(
                "Playwright worker script not found: '%s', and the teswiz jar on the classpath does not bundle one. "
                        + "Use a teswiz jar that bundles the worker, or copy playwright/worker.mjs and "
                        + "playwright/worker-provider.mjs from the teswiz repository into the project.",
                projectWorker));
    }
}
