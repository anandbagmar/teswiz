package com.znsio.teswiz.config.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.znsio.teswiz.config.TeswizRuntimeConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.znsio.teswiz.exceptions.InvalidTestDataException;

class AppPathResolverTest {
    private static final Logger LOGGER = LogManager.getLogger(AppPathResolverTest.class.getName());
    private static final String DIRECTORY_PATH = System.getProperty("user.dir") + File.separator + "temp"
            + File.separator + "unitTests" + File.separator + "sampleApps";
    private static final String FILE_NAME = "VodQA.apk";
    private static final String EXPECTED_APP_PATH = DIRECTORY_PATH + File.separator + FILE_NAME;
    private static final String APP_PATH_AS_CORRECT_FILE_PATH = EXPECTED_APP_PATH;
    private static final String APP_PATH_AS_INCORRECT_FILE_PATH = System.getProperty("user.dir") + File.separator
            + "temp" + File.separator + "unitTests" + File.separator + "smleApps" + File.separator + FILE_NAME;
    private static final String LAMBDATEST_APP_REFERENCE = "lt://APP123";
    private static HttpServer appServer;
    private static String appPathAsCorrectUrl;
    private static String appPathAsIncorrectUrl;

    @BeforeAll
    static void setupBefore() throws IOException {
        LOGGER.info("Running AppPathResolverTest");
        appServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        appServer.createContext("/VodQA.apk", AppPathResolverTest::serveValidApp);
        appServer.createContext("/invalid.apk", exchange -> sendResponse(exchange, 404, new byte[0]));
        appServer.start();
        String serverBaseUrl = "http://127.0.0.1:" + appServer.getAddress().getPort();
        appPathAsCorrectUrl = serverBaseUrl + "/VodQA.apk";
        appPathAsIncorrectUrl = serverBaseUrl + "/invalid.apk";
    }

    @AfterAll
    static void tearDownAfter() {
        if (appServer != null) {
            appServer.stop(0);
        }
    }

    @AfterEach
    void clearDownloadTimeoutProperty() {
        System.clearProperty(TeswizRuntimeConfiguration.APP_DOWNLOAD_TIMEOUT_SECONDS);
    }

    @Test
    void shouldUseThirtySecondDefaultForAppDownloadTimeout() {
        assertEquals(15_000, AppPathResolver.getAppDownloadTimeoutMillis());
    }

    @Test
    void shouldUseConfiguredAppDownloadTimeoutInSeconds() {
        System.setProperty(TeswizRuntimeConfiguration.APP_DOWNLOAD_TIMEOUT_SECONDS, "45");

        assertEquals(45_000, AppPathResolver.getAppDownloadTimeoutMillis());
    }

    @Test
    void shouldUseDefaultForInvalidAppDownloadTimeout() {
        System.setProperty(TeswizRuntimeConfiguration.APP_DOWNLOAD_TIMEOUT_SECONDS, "-1");

        assertThrows(InvalidTestDataException.class, AppPathResolver::getAppDownloadTimeoutMillis);
    }

    private static void serveValidApp(HttpExchange exchange) throws IOException {
        sendResponse(exchange, 200, "sample app content".getBytes(StandardCharsets.UTF_8));
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, byte[] responseBody) throws IOException {
        try (exchange) {
            if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(statusCode, -1);
                return;
            }
            exchange.sendResponseHeaders(statusCode, responseBody.length);
            exchange.getResponseBody().write(responseBody);
        }
    }

    @Test
    void givenIncorrectUrlWhenDirectoryAndFileDoNotExistThenIOExceptionOccurWhileTryingToDownloadFile() {
        deleteDirectoryUsedForUnitTests();
        assertThrows(InvalidTestDataException.class,
                () -> AppPathResolver.resolveAppPath(appPathAsIncorrectUrl, DIRECTORY_PATH));
    }

    @Test
    void givenIncorrectUrlWhenDirectoryExistAndFileDoNotExistThenIOExceptionOccurWhileTryingToDownloadFile() {
        createDirectoryUsedForUnitTests();
        deleteFile(APP_PATH_AS_CORRECT_FILE_PATH);
        assertThrows(InvalidTestDataException.class,
                () -> AppPathResolver.resolveAppPath(appPathAsIncorrectUrl, DIRECTORY_PATH));
    }

    @Test
    void givenIncorrectUrlWhenDirectoryAndFileBothExistThenFileIsReadable() {
        createDirectoryUsedForUnitTests();
        AppPathResolver.resolveAppPath(appPathAsCorrectUrl, DIRECTORY_PATH);
        assertThrows(InvalidTestDataException.class,
                () -> AppPathResolver.resolveAppPath(appPathAsIncorrectUrl, DIRECTORY_PATH));
    }

    @Test
    void givenCorrectUrlWhenDirectoryAndFileDoNotExistThenCreateDirectoryAndDownloadFile() {
        deleteDirectoryUsedForUnitTests();
        String actualAppPath = AppPathResolver.resolveAppPath(appPathAsCorrectUrl, DIRECTORY_PATH);
        assertEquals(EXPECTED_APP_PATH, actualAppPath);
        assertTrue(new File(actualAppPath).canRead());
    }

    @Test
    void givenCorrectUrlWhenDirectoryExistButFileDoNotExistThenDownloadFile() {
        createDirectoryUsedForUnitTests();
        deleteFile(APP_PATH_AS_CORRECT_FILE_PATH);
        String actualAppPath = AppPathResolver.resolveAppPath(appPathAsCorrectUrl, DIRECTORY_PATH);
        assertEquals(EXPECTED_APP_PATH, actualAppPath);
        assertTrue(new File(actualAppPath).canRead());
    }

    @Test
    void givenCorrectUrlWhenDirectoryAndFileAlreadyExistThenDoNotDownloadFile() {
        createDirectoryUsedForUnitTests();
        AppPathResolver.resolveAppPath(appPathAsCorrectUrl, DIRECTORY_PATH);
        assertTrue(new File(EXPECTED_APP_PATH).canRead());
        String actualAppPath = AppPathResolver.resolveAppPath(appPathAsCorrectUrl, DIRECTORY_PATH);
        assertTrue(new File(actualAppPath).canRead());
        assertEquals(EXPECTED_APP_PATH, actualAppPath);
    }

    @Test
    void givenIncorrectFilePathWhenDirectoryAndFileDoNotExistThenFileIsNotReadable() {
        deleteDirectoryUsedForUnitTests();
        assertThrows(InvalidTestDataException.class,
                () -> AppPathResolver.resolveAppPath(APP_PATH_AS_INCORRECT_FILE_PATH, DIRECTORY_PATH));
    }

    @Test
    void givenIncorrectFilePathWhenDirectoryExistButFileDoNotExistThenFileIsNotReadable() {
        createDirectoryUsedForUnitTests();
        deleteFile(APP_PATH_AS_CORRECT_FILE_PATH);
        assertThrows(InvalidTestDataException.class,
                () -> AppPathResolver.resolveAppPath(APP_PATH_AS_INCORRECT_FILE_PATH, DIRECTORY_PATH));
    }

    @Test
    void givenIncorrectFilePathWhenDirectoryAndFileExistThenFileIsNotReadable() {
        createDirectoryUsedForUnitTests();
        assertThrows(InvalidTestDataException.class,
                () -> AppPathResolver.resolveAppPath(APP_PATH_AS_INCORRECT_FILE_PATH, DIRECTORY_PATH));
    }

    @Test
    void givenCorrectFilePathWhenDirectoryAndFileDoNotExistThenFileIsNotReadable() {
        deleteDirectoryUsedForUnitTests();
        assertThrows(RuntimeException.class,
                () -> AppPathResolver.resolveAppPath(APP_PATH_AS_CORRECT_FILE_PATH, DIRECTORY_PATH));
    }

    @Test
    void givenCorrectFilePathWhenDirectoryExistButFileDoNotExistThenFileIsNotReadable() {
        createDirectoryUsedForUnitTests();
        deleteFile(APP_PATH_AS_CORRECT_FILE_PATH);
        assertThrows(RuntimeException.class,
                () -> AppPathResolver.resolveAppPath(APP_PATH_AS_CORRECT_FILE_PATH, DIRECTORY_PATH));
    }

    @Test
    void givenCorrectFilePathWhenDirectoryAndFileAlreadyExistThenFileIsReadable() {
        createDirectoryUsedForUnitTests();
        AppPathResolver.resolveAppPath(appPathAsCorrectUrl, DIRECTORY_PATH);
        String actualAppPath = AppPathResolver.resolveAppPath(APP_PATH_AS_CORRECT_FILE_PATH, DIRECTORY_PATH);
        assertTrue(new File(actualAppPath).canRead());
        assertEquals(EXPECTED_APP_PATH, actualAppPath);
    }

    @Test
    void givenLambdaTestAppReferenceWhenCheckingAppPathThenReturnReferenceWithoutLocalValidation() {
        String actualAppPath = AppPathResolver.resolveAppPath(LAMBDATEST_APP_REFERENCE, DIRECTORY_PATH);
        assertEquals(LAMBDATEST_APP_REFERENCE, actualAppPath);
    }

    @Test
    void shouldIdentifyNonUrlAppPathsWithoutThrowing() {
        assertThat(AppPathResolver.isAppPathUrl("temp/sampleApps/TheApp.apk")).isFalse();
    }

    private static void deleteFile(String filePath) {
        try {
            Files.deleteIfExists(Path.of(filePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file for unit test: " + e.getMessage());
        }
    }

    private static void deleteDirectoryUsedForUnitTests() {
        if (Files.exists(Paths.get(DIRECTORY_PATH))) {
            try {
                Files.walk(Paths.get(DIRECTORY_PATH))
                        .sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            } catch (IOException e) {
                System.err.println("Failed to delete folder for unit test: " + e.getMessage());
            }
        }
    }

    private static void createDirectoryUsedForUnitTests() {
        if (!Files.exists(Paths.get(DIRECTORY_PATH))) {
            try {
                Files.createDirectories(Paths.get(DIRECTORY_PATH));
            } catch (IOException e) {
                System.err.println("Failed to create directory: " + DIRECTORY_PATH
                        + " for unit test, error occurred" + e);
            }
        }
    }
}
