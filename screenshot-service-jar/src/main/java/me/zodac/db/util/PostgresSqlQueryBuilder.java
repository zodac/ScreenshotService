package me.zodac.db.util;

import static java.util.stream.Collectors.joining;

import java.util.List;

/**
 * Utility class used to construct SQL queries for CRUD operations for the PostgreSQL database.
 */
public class PostgresSqlQueryBuilder {

    private PostgresSqlQueryBuilder() {

    }

    /**
     * Creates the <b>INSERT</b> SQL statement to create the screenshot request.
     *
     * @param urls the URLs for the request
     * @return the SQL statement
     */
    public static String insertRequest(final List<String> urls) {
        final String urlsForSqlArray = urls.stream()
                .map(url -> "'" + url + "'")
                .collect(joining(", "));

        return String.format("INSERT INTO screenshot_request (urls) VALUES (ARRAY [%s]) RETURNING request_id", urlsForSqlArray);
    }

    /**
     * Creates the <b>INSERT</b> SQL statement to create the screenshot result.
     *
     * @param jobId     the job ID of the screenshot request
     * @param filePaths the file paths of the screenshots
     * @return the SQL statement
     */
    public static String insertResult(final int jobId, final List<String> filePaths) {
        final String filePathsForSqlArray = filePaths.stream()
                .map(url -> "'" + url + "'")
                .collect(joining(", "));

        return String.format("INSERT INTO screenshot_result (job_id, screenshot_filepaths) VALUES ('%s', ARRAY [%s])", jobId, filePathsForSqlArray);
    }

    /**
     * Creates the <b>SELECT</b> SQL statement to retrieve a screenshot result based on job ID.
     *
     * @param jobId the job ID of the screenshot result
     * @return the SQL statement
     */
    public static String selectResult(final int jobId) {
        return String.format("SELECT screenshot_filepaths FROM screenshot_result WHERE job_id = '%d'", jobId);
    }
}
