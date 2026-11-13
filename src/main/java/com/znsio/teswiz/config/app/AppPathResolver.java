package com.znsio.teswiz.config.app;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.Duration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.znsio.teswiz.exceptions.InvalidTestDataException;
import com.znsio.teswiz.config.TeswizRuntimeConfiguration;
import com.znsio.teswiz.network.FallbackHttpClient;
import com.znsio.teswiz.network.HttpRequest;
import com.znsio.teswiz.network.HttpResponse;
import com.znsio.teswiz.tools.SensitiveDataMasker;

public final class AppPathResolver {
    private static final Logger LOGGER = LogManager.getLogger(AppPathResolver.class.getName());
    private static final int MILLIS_PER_SECOND = 1_000;
    private static final int HTTP_OK = 200;
    private static final String NETWORK_PURPOSE = "app-download";
    private static final String LAMBDATEST_APP_PREFIX = "lt://";
    private static final String BROWSERSTACK_APP_PREFIX = "bs://";

    private AppPathResolver() {
    }

    public static String resolveAppPath(String appPath, String saveToLocalDirectory) {
        if (isCloudHostedAppReference(appPath)) {
            LOGGER.info("Cloud hosted app reference '{}' provided. Skipping local file validation.", SensitiveDataMasker.mask(appPath));
            return appPath;
        }
        String fileName = new File(appPath).getName();
        String localFilePath = saveToLocalDirectory + File.separator + fileName;
        if (new File(localFilePath).exists()) {
            LOGGER.info("App is already available at path: '{}'. Skipping network URL validation.", SensitiveDataMasker.mask(localFilePath));
            return localFilePath;
        }
        if (isAppPathUrl(appPath)) {
            LOGGER.info("App url '{}' is provided in capabilities. Download it, if not already available at '{}'", SensitiveDataMasker.mask(appPath), SensitiveDataMasker.mask(localFilePath));
            downloadFileIfDoesNotExist(appPath, localFilePath, saveToLocalDirectory);
            LOGGER.info("Changing value of appPath from URL to file path");
            LOGGER.info("Before change, appPath value: {}", SensitiveDataMasker.mask(appPath));
            appPath = localFilePath;
            LOGGER.info("After change, appPath value: {}", SensitiveDataMasker.mask(localFilePath));
        } else {
            LOGGER.info("App file path '{}' is provided in capabilities.", SensitiveDataMasker.mask(appPath));
            if (!(new File(appPath).exists())) {
                throw new InvalidTestDataException(
                        String.format("App file path '%s' provided in capabilities is incorrect", appPath));
            }
        }
        LOGGER.info("App file path '{}' is provided in capabilities.", SensitiveDataMasker.mask(appPath));
        LOGGER.info("File available at App file path '{}'", SensitiveDataMasker.mask(appPath));
        return appPath;
    }

    public static boolean isCloudHostedAppReference(String appPath) {
        if (null == appPath) {
            return false;
        }
        return appPath.startsWith(LAMBDATEST_APP_PREFIX) || appPath.startsWith(BROWSERSTACK_APP_PREFIX);
    }

    public static boolean isAppPathUrl(String appPathUrl) {
        try {
            new URL(appPathUrl);
            LOGGER.info("'{}' is a URL.", appPathUrl);
            validateAppUrl(appPathUrl);
            return true;
        } catch (MalformedURLException e) {
            LOGGER.info("'{}' is not a URL.", appPathUrl);
            return false;
        }
    }

    private static void downloadFile(String url, String filePath, String saveToDirectory) {
        LOGGER.info("Downloading App from url: '{}'", url);
        try {
            createDirectoryIfNotExists(saveToDirectory);
            HttpRequest request = HttpRequest.get(url, appDownloadTimeout()).followingRedirects();
            HttpResponse response = new FallbackHttpClient(NETWORK_PURPOSE).download(request, Path.of(filePath));
            failUnlessOk(response.status(), url);
            String formattedSize = getDownloadedAppSize(Path.of(filePath));
            LOGGER.info("App downloaded at path: '{}', having size: '{} MB'", filePath, formattedSize);
        } catch (IOException e) {
            throw new InvalidTestDataException(String.format("Unable to download file '%s'", url), e);
        }
    }

    private static void failUnlessOk(int statusCode, String url) {
        if (statusCode != HTTP_OK) {
            throw new InvalidTestDataException(
                    String.format("Unable to connect to url: '%s'. Got connection error '%d'", url, statusCode));
        }
    }

    private static String getDownloadedAppSize(Path filePath) {
        try {
            long fileSizeBytes = Files.size(filePath);
            double fileSizeMB = (double) fileSizeBytes / (1024 * 1024);
            return new DecimalFormat("#.##").format(fileSizeMB);
        } catch (IOException e) {
            throw new InvalidTestDataException("Unable to get downloaded app file size. Download " +
                    "may be corrupt. Check and fix before rerunning the test.", e);
        }
    }

    private static void createDirectoryIfNotExists(String directory) throws IOException {
        Path directoryPath = Path.of(directory);
        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
        }
    }

    private static void downloadFileIfDoesNotExist(String appPath, String filePath, String saveToDirectory) {
        if (!(new File(filePath).exists())) {
            LOGGER.info("App is not available at path: '{}'. Download it.", appPath);
            downloadFile(appPath, filePath, saveToDirectory);
        } else {
            LOGGER.info("App is already available at path: '{}'. No need to download it.", appPath);
        }
    }

    private static void validateAppUrl(String appPathUrl) {
        try {
            HttpRequest request = HttpRequest.head(appPathUrl, appDownloadTimeout()).followingRedirects();
            int responseCode = new FallbackHttpClient(NETWORK_PURPOSE).send(request).status();
            if (responseCode != HTTP_OK) {
                LOGGER.info("'{}' is an invalid URL.", appPathUrl);
                throw new InvalidTestDataException("URL is not accessible: " + appPathUrl);
            }
            LOGGER.info("'{}' is a valid URL.", appPathUrl);
        } catch (IOException e) {
            throw new InvalidTestDataException(
                    String.format("Failed to make a connection using url: '%s'", appPathUrl) + e);
        }
    }

    private static Duration appDownloadTimeout() {
        return Duration.ofMillis(getAppDownloadTimeoutMillis());
    }

    static int getAppDownloadTimeoutMillis() {
        return Math.multiplyExact(
                TeswizRuntimeConfiguration.getInt(TeswizRuntimeConfiguration.APP_DOWNLOAD_TIMEOUT_SECONDS),
                MILLIS_PER_SECOND);
    }
}
