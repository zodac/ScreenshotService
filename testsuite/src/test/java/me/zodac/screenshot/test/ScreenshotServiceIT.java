package me.zodac.screenshot.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.Gson;

import me.zodac.screenshot.api.rest.ScreenshotRequest;
import me.zodac.screenshot.api.rest.ScreenshotResult;

/**
 * Integration tests for the REST endpoints for <core>screenshot-service</core>.
 */
@RunWith(Arquillian.class)
public class ScreenshotServiceIT {

    private static final String BASE_SCREENSHOT_SERVICE_URL = "http://192.168.99.100:8080/screenshot/";
    private static final String TEST_URL = "http://www.google.ie";
    private static final int NUMBER_OF_SCREENSHOTS_TO_TAKE = 10;
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Deployment(order = 1)
    public static EnterpriseArchive getTestEar() {
        return Deployments.getTestEar();
    }

    @InSequence(1)
    @RunAsClient
    @Test(timeout = 180_000L)
    public void verifyScreenshotService() throws IOException, InterruptedException {
        // Send request to service
        final List<String> urls = createUrls();

        final String payload = GSON.toJson(new ScreenshotRequest(urls));

        final HttpRequest createRequest = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(BASE_SCREENSHOT_SERVICE_URL))
                .header("Content-Type", "application/json")
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(createRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("Did not receive a 201_CREATED HTTP response")
                .isEqualTo(HttpURLConnection.HTTP_CREATED);

        final String requestUrl = response.headers().firstValue("location").orElse("No 'location' header");
        assertThat(requestUrl)
                .as("Location header did not contain the URL for the created screenshot request")
                .startsWith(BASE_SCREENSHOT_SERVICE_URL);

        // Retrieve result
        final HttpRequest resultRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "application/json")
                .build();

        final HttpResponse<String> resultResponse = HTTP_CLIENT.send(resultRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(resultResponse.statusCode())
                .as("Did not receive a 200_OK HTTP response for: " + requestUrl)
                .isEqualTo(HttpURLConnection.HTTP_OK);


        final ScreenshotResult screenshotResult = GSON.fromJson(resultResponse.body(), ScreenshotResult.class);
        assertThat(screenshotResult.getScreenshotFileNames())
                .as("Screenshot response did not have the correct number of file names")
                .hasSize(NUMBER_OF_SCREENSHOTS_TO_TAKE);

        // Retrieve single screenshot
        final String firstScreenshotFileName = screenshotResult.getScreenshotFileNames().get(0);
        final HttpRequest singleScreenshotRequest = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(requestUrl + "/" + firstScreenshotFileName))
                .build();
        final HttpResponse<String> singleScreenshotResponse = HTTP_CLIENT.send(singleScreenshotRequest, HttpResponse.BodyHandlers.ofString());

        assertThat(singleScreenshotResponse.statusCode())
                .as("Did not receive a 200_OK HTTP response for: " + requestUrl + "/" + firstScreenshotFileName)
                .isEqualTo(HttpURLConnection.HTTP_OK);
    }

    @InSequence(2)
    @RunAsClient
    @Test
    public void whenSendingRequestWithInvalidUrls_then400ResponseIsReturned() throws IOException, InterruptedException {
        final List<String> urls = createInvalidUrls();
        final String payload = GSON.toJson(new ScreenshotRequest(urls));

        final HttpRequest request = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .uri(URI.create(BASE_SCREENSHOT_SERVICE_URL))
                .header("Content-Type", "application/json")
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("Did not receive a 400_BAD_REQUEST HTTP response")
                .isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST);
    }

    @InSequence(3)
    @RunAsClient
    @Test
    public void whenGettingRequestByJobId_givenInvalidJobId_then404ResponseIsReturned() throws IOException, InterruptedException {
        final int invalidJobId = -1;
        final String requestUrl = BASE_SCREENSHOT_SERVICE_URL + invalidJobId;

        final HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "application/json")
                .build();

        final HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("Did not receive a 404_NOT_FOUND HTTP response for: " + requestUrl)
                .isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    @InSequence(4)
    @RunAsClient
    @Test
    public void whenGettingRequestByScreenshotFileName_givenInvalidScreenshotFileName_then404ResponseIsReturned() throws IOException, InterruptedException {
        final String invalidScreenshotFileName = "invalidScreenshot.png";
        final String requestUrl = BASE_SCREENSHOT_SERVICE_URL + "/1/" + invalidScreenshotFileName;

        final HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(requestUrl))
                .header("Content-Type", "image/png")
                .build();

        final HttpResponse<byte[]> singleScreenshotResponse = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());

        assertThat(singleScreenshotResponse.statusCode())
                .as("Did not receive a 404_NOT_FOUND HTTP response for: " + requestUrl)
                .isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
    }

    private static List<String> createUrls() {
        return IntStream.range(0, NUMBER_OF_SCREENSHOTS_TO_TAKE)
                .mapToObj(i -> TEST_URL)
                .collect(Collectors.toList());
    }

    private static List<String> createInvalidUrls() {
        return Collections.singletonList("invalid-");
    }
}
