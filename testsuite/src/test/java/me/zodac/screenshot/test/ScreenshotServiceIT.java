package me.zodac.screenshot.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpStatus;
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

    private static final String BASE_SCREENSHOT_SERVICE_URL = "http://127.0.0.1:8080/screenshot/";
    private static final int NUMBER_OF_SCREENSHOTS_TO_TAKE = 10;
    private static final Gson GSON = new Gson();

    @Deployment(order = 1)
    public static EnterpriseArchive getTestEar() {
        return Deployments.getTestEar();
    }

    @InSequence(1)
    @RunAsClient
    @Test(timeout = 180_000L)
    public void verifyScreenshotService() {
        // Send request to service
        final List<String> urls = createUrls();

        final ScreenshotRequest request = new ScreenshotRequest(urls);
        final String payload = GSON.toJson(request);

        final Response requestResponse = ClientBuilder.newClient()
                .target(BASE_SCREENSHOT_SERVICE_URL)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(payload));

        assertThat(requestResponse.getStatus())
                .as("Did not receive a 201_CREATED HTTP response")
                .isEqualTo(HttpStatus.SC_CREATED);

        final String requestUrl = String.valueOf(requestResponse.getHeaders().getFirst("location"));
        assertThat(requestUrl)
                .as("Location header did not contain the URL for the created screenshot request")
                .startsWith(BASE_SCREENSHOT_SERVICE_URL);

        // Retrieve result
        final Response resultResponse = ClientBuilder.newClient()
                .target(requestUrl)
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertThat(resultResponse.getStatus())
                .as("Did not receive a 200_OK HTTP response for: " + requestUrl)
                .isEqualTo(HttpStatus.SC_OK);

        final ScreenshotResult screenshotResult = GSON.fromJson(resultResponse.readEntity(String.class), ScreenshotResult.class);
        assertThat(screenshotResult.getScreenshotFileNames())
                .as("Screenshot response did not have the correct number of file names")
                .hasSize(NUMBER_OF_SCREENSHOTS_TO_TAKE);

        // Retrieve single screenshot
        final String firstScreenshotFileName = screenshotResult.getScreenshotFileNames().get(0);
        final Response singleScreenshotResponse = ClientBuilder.newClient()
                .target(requestUrl + "/" + firstScreenshotFileName)
                .request("image/png")
                .get();

        assertThat(singleScreenshotResponse.getStatus())
                .as("Did not receive a 200_OK HTTP response for: " + requestUrl + "/" + firstScreenshotFileName)
                .isEqualTo(HttpStatus.SC_OK);
    }

    @InSequence(2)
    @RunAsClient
    @Test
    public void whenSendingRequestWithInvalidUrls_then400ResponseIsReturned() {
        final List<String> urls = createInvalidUrls();

        final ScreenshotRequest request = new ScreenshotRequest(urls);
        final String payload = GSON.toJson(request);

        final Response requestResponse = ClientBuilder.newClient()
                .target(BASE_SCREENSHOT_SERVICE_URL)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(payload));

        assertThat(requestResponse.getStatus())
                .as("Did not receive a 400_BAD_REQUEST HTTP response")
                .isEqualTo(HttpStatus.SC_BAD_REQUEST);
    }

    @InSequence(3)
    @RunAsClient
    @Test
    public void whenGettingRequestByJobId_givenInvalidJobId_then404ResponseIsReturned() {
        final int invalidJobId = -1;
        final String requestUrl = BASE_SCREENSHOT_SERVICE_URL + invalidJobId;

        final Response resultResponse = ClientBuilder.newClient()
                .target(requestUrl)
                .request(MediaType.APPLICATION_JSON)
                .get();

        assertThat(resultResponse.getStatus())
                .as("Did not receive a 404_NOT_FOUND HTTP response for: " + requestUrl)
                .isEqualTo(HttpStatus.SC_NOT_FOUND);
    }

    @InSequence(4)
    @RunAsClient
    @Test
    public void whenGettingRequestByScreenshotFileName_givenInvalidScreenshotFileName_then404ResponseIsReturned() {
        final String invalidScreenshotFileName = "invalidScreenshot.png";
        final String requestUrl = BASE_SCREENSHOT_SERVICE_URL + "/1/" + invalidScreenshotFileName;

        final Response singleScreenshotResponse = ClientBuilder.newClient()
                .target(requestUrl)
                .request("image/png")
                .get();

        assertThat(singleScreenshotResponse.getStatus())
                .as("Did not receive a 404_NOT_FOUND HTTP response for: " + requestUrl)
                .isEqualTo(HttpStatus.SC_NOT_FOUND);
    }

    private static List<String> createUrls() {
        final List<String> urls = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_SCREENSHOTS_TO_TAKE; i++) {
            urls.add("http://www.google.ie");
        }
        return urls;
    }

    private static List<String> createInvalidUrls() {
        return Collections.singletonList("invalid-");
    }
}
