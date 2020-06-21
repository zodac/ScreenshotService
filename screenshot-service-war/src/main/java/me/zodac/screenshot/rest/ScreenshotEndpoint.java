package me.zodac.screenshot.rest;

import static me.zodac.util.EnvironmentUtils.getEnvironmentValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import me.zodac.db.PostgresManager;
import me.zodac.screenshot.ScreenshotTaker;
import me.zodac.screenshot.api.rest.ScreenshotRequest;
import me.zodac.screenshot.api.rest.ScreenshotResult;

/**
 * REST endpoints for <code>screenshot-service</code>.
 */
@Path("/")
@RequestScoped
public class ScreenshotEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenshotEndpoint.class);
    private static final UrlValidator URL_VALIDATOR = new UrlValidator();
    private static final Gson GSON = new Gson();

    @Context
    private UriInfo uriContext;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRequest(final ScreenshotRequest request) {
        LOGGER.info("POST request received at '{}': {}", uriContext.getAbsolutePath(), request);

        final List<String> invalidUrls = getInvalidUrls(request.getUrls());
        if (!invalidUrls.isEmpty()) {
            final Map<String, List<String>> invalidUrlsWithName = new TreeMap<>();
            invalidUrlsWithName.put("invalidUrls", invalidUrls);

            return Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity(GSON.toJson(invalidUrlsWithName))
                    .build();
        }

        try {
            final ScreenshotTaker screenshotTaker = new ScreenshotTaker();
            final int jobId = screenshotTaker.takeScreenshots(request.getUrls());

            final UriBuilder builder = uriContext.getBaseUriBuilder()
                    .path(String.valueOf(jobId));
            return Response.created(builder.build()).build();
        } catch (final Exception e) {
            LOGGER.error("Unexpected error taking screenshots", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{jobId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResult(@PathParam("jobId") final int jobId) {
        LOGGER.info("GET request for job received at '{}'", uriContext.getAbsolutePath());

        try {
            final ScreenshotResult screenshotResult = PostgresManager.getResult(jobId);
            final String json = new Gson().toJson(screenshotResult);
            return Response.ok(json).build();
        } catch (final NoSuchElementException e) {
            LOGGER.error("Error finding result", e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("Unexpected error retrieving job result", e);
            return Response.serverError().build();
        }
    }

    @GET
    @Path("/{jobId}/{fileName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("image/png")
    public Response getScreenshot(@PathParam("jobId") final int jobId, @PathParam("fileName") final String fileName) {
        LOGGER.info("GET request for screenshot received at '{}'", uriContext.getAbsolutePath());
        final String filePath = getEnvironmentValue("OUTPUT_DIRECTORY") + File.separator + jobId + File.separator + fileName;
        final File screenshot = new File(filePath);

        if (!screenshot.exists()) {
            LOGGER.error("No file '{}' exists", filePath);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            final Date fileDate = new Date(screenshot.lastModified());
            return Response.ok(new FileInputStream(screenshot)).lastModified(fileDate).build();
        } catch (final FileNotFoundException e) {
            LOGGER.error("Error finding file '{}'", filePath, e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (final Exception e) {
            LOGGER.error("Unexpected error retrieving screenshot", e);
            return Response.serverError().build();
        }
    }

    private static List<String> getInvalidUrls(final List<String> urls) {
        return urls.stream()
                .filter(url -> !URL_VALIDATOR.isValid(url))
                .collect(Collectors.toList());
    }
}
