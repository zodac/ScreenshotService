package me.zodac.screenshot;

import static me.zodac.util.EnvironmentUtils.getEnvironmentValue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.zodac.db.PostgresManager;
import me.zodac.screenshot.api.rest.ScreenshotServiceException;

/**
 * Utility class that takes screenshots and saves the result to the database.
 * <p>
 * Requires the following environment variables to be set:
 * <ul>
 * <li>NUMBER_OF_EXECUTOR_THREADS: the number of threads used by the {@link ExecutorService}</li>
 * <li>REMOTE_BROWSER_URL: the URL of the Selenium hub for remote browser access</li>
 * <li>OUTPUT_DIRECTORY: the root directory that all screenshots will be saved to</li>
 * </ul>
 */
public final class ScreenshotTaker {

    private static final ExecutorService EXECUTOR_SERVICE = Executors
            .newFixedThreadPool(Integer.parseInt(getEnvironmentValue("NUMBER_OF_EXECUTOR_THREADS")));
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotTaker.class);
    private static final String REMOTE_BROWSER_URL = getEnvironmentValue("REMOTE_BROWSER_URL");
    private static final String ROOT_DIRECTORY = getEnvironmentValue("OUTPUT_DIRECTORY");
    private static final DateTimeFormatter FILE_NAME_PREFIX_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_hh:mm:ss", Locale.UK);

    /**
     * Executes the business logic for the service:
     * <ol>
     * <li>Saves the input list of URLs as the request to the DB</li>
     * <li>Takes a screenshot for each URL on a remote browser</li>
     * <li>Saves the screenshot to disk</li>
     * <li>Saves the result of the execution to the DB</li>
     * </ol>
     * Saves the input request for screenshots for the given URLs, takes the screenshots for each input URL, then saves the result to the DB.
     * <p>
     * Screenshot attempts are best-effort. If one fails, execution continues and the successful screenshots are returned.
     *
     * @param urls the URLs to be screenshot
     * @return the job ID
     * @throws ScreenshotServiceException thrown if any error occurs taking a screenshot
     * @see PostgresManager#createResult(int, List)
     */
    public int takeScreenshots(final List<String> urls) throws ScreenshotServiceException {
        LOGGER.info("Starting to take {} screenshots", urls.size());
        final int jobId = saveRequest(urls);
        final long startTime = System.nanoTime();

        final String outputDirectory = ROOT_DIRECTORY + File.separator + jobId + File.separator;
        final String fileNamePrefix = FILE_NAME_PREFIX_FORMATTER.format(LocalDateTime.now());

        final List<Future<String>> screenshotFutures = submitScreenshots(fileNamePrefix, outputDirectory, urls);
        final List<String> screenshotFilePaths = retrieveScreenshotFilePaths(screenshotFutures);

        saveResults(jobId, screenshotFilePaths);
        final long endTime = System.nanoTime();
        LOGGER.info("Took {} screenshots in {} seconds", urls.size(), TimeUnit.NANOSECONDS.toSeconds(endTime - startTime));
        return jobId;
    }

    private int saveRequest(final List<String> urls) throws ScreenshotServiceException {
        try {
            return PostgresManager.createRequest(urls);
        } catch (final SQLException e) {
            throw new ScreenshotServiceException("Error saving request", e);
        }
    }

    private List<Future<String>> submitScreenshots(final String fileNamePrefix, final String outputDirectory,
                                                   final List<String> urls) {
        final List<Callable<String>> screenshotCallables = new ArrayList<>(urls.size());
        int fileNameSuffix = 1;
        for (final String url : urls) {
            screenshotCallables.add(takeScreenshot(url, fileNamePrefix, fileNameSuffix++, outputDirectory));
        }

        try {
            return EXECUTOR_SERVICE.invokeAll(screenshotCallables);
        } catch (final InterruptedException e) {
            LOGGER.warn("Error submitting futures", e);
            return Collections.emptyList();
        }
    }

    private static Callable<String> takeScreenshot(final String url, final String fileNamePrefix, final int fileNameSuffix,
                                                   final String outputDirectory) {
        return () -> {
            final WebDriver webDriver = getFullSizeWebDriver();
            webDriver.get(url);
            final File screenshot = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.FILE);
            final String filePath = outputDirectory + fileNamePrefix + "_" + fileNameSuffix + ".png";
            FileUtils.copyFile(screenshot, new File(filePath));
            LOGGER.info("Successfully screenshot '{}' as '{}'", url, filePath);
            webDriver.quit();
            return filePath;
        };
    }

    private static WebDriver getFullSizeWebDriver() throws MalformedURLException {
        final ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        final RemoteWebDriver chrome = new RemoteWebDriver(new URL(REMOTE_BROWSER_URL), options);
        final WebDriver.Window window = chrome.manage().window();
        window.setSize(new Dimension(1920, 1080));
        window.setPosition(new Point(-2000, 0));

        return chrome;
    }

    private List<String> retrieveScreenshotFilePaths(final List<Future<String>> screenshotFutures) {
        final List<String> screenshotFilePaths = new ArrayList<>(screenshotFutures.size());

        for (final Future<String> screenshotFuture : screenshotFutures) {
            try {
                screenshotFilePaths.add(screenshotFuture.get());
            } catch (final InterruptedException | ExecutionException e) {
                LOGGER.warn("Error retrieving future: {}", screenshotFuture, e);
            }
        }

        return screenshotFilePaths;
    }

    private void saveResults(final int jobId, final List<String> screenshotFilePaths) throws ScreenshotServiceException {
        try {
            PostgresManager.createResult(jobId, screenshotFilePaths);
        } catch (final SQLException e) {
            throw new ScreenshotServiceException(String.format("Error saving results for job ID '%d'", jobId), e);
        }
    }
}
