package me.zodac.db;

import static java.util.stream.Collectors.toList;
import static me.zodac.util.EnvironmentUtils.getEnvironmentValue;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;

import me.zodac.db.util.PostgresSqlQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.zodac.screenshot.api.rest.ScreenshotResult;

/**
 * Class responsible for managing the CRUD operations for any screenshot requests for the PostgreSQL database.
 * <p>
 * Requires the following environment variables to be set in order to connect to the database:
 * <ul>
 * <li>JDBC_CONNECTION_URL</li>
 * <li>JDBC_CONNECTION_USER</li>
 * <li>JDBC_CONNECTION_PASSWORD</li>
 * <li>JDBC_CONNECTION_DRIVER</li>
 * </ul>
 */
public final class PostgresManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresManager.class);

    private static final String JDBC_CONNECTION_URL = getEnvironmentValue("JDBC_CONNECTION_URL");
    private static final Properties JDBC_CONNECTION_PROPERTIES = new Properties();

    static {
        JDBC_CONNECTION_PROPERTIES.setProperty("user", getEnvironmentValue("JDBC_CONNECTION_USER"));
        JDBC_CONNECTION_PROPERTIES.setProperty("password", getEnvironmentValue("JDBC_CONNECTION_PASSWORD"));
        JDBC_CONNECTION_PROPERTIES.setProperty("driver", getEnvironmentValue("JDBC_CONNECTION_DRIVER"));
    }

    private PostgresManager() {

    }

    /**
     * Persists a screenshot request to the DB. No ID is required as it is auto-generated by the PostgreSQL DB.
     *
     * @param urls the URLs for the request
     * @return the generated ID for the request
     * @throws SQLException thrown if any error occurs executing the SQL statement
     * @see PostgresSqlQueryBuilder#insertRequest(List)
     */
    public static int createRequest(final List<String> urls) throws SQLException {
        final String insertSqlStatement = PostgresSqlQueryBuilder.insertRequest(urls);
        return executeInsertSqlWithReturnId(insertSqlStatement);
    }

    /**
     * Persists the results for a screenshot request to the DB.
     *
     * @param jobId     the job ID of the screenshot request
     * @param filePaths the file paths of the screenshots
     * @throws SQLException thrown if any error occurs executing the SQL statement
     * @see PostgresSqlQueryBuilder#insertResult(int, List)
     */
    public static void createResult(final int jobId, final List<String> filePaths) throws SQLException {
        final String insertSqlStatement = PostgresSqlQueryBuilder.insertResult(jobId, filePaths);
        executeInsertSql(insertSqlStatement);
    }

    /**
     * Retrieves a previous result based on job ID.
     *
     * @param jobId the job ID of the screenshot request
     * @return the {@link ScreenshotResult}
     * @throws SQLException thrown if any error occurs executing the SQL statement
     * @see PostgresSqlQueryBuilder#selectResult(int)
     */
    public static ScreenshotResult getResult(final int jobId) throws SQLException {
        final String selectSqlStatement = PostgresSqlQueryBuilder.selectResult(jobId);
        LOGGER.debug("Executing SQL statement '{}'", selectSqlStatement);

        try (final Connection connection = DriverManager.getConnection(JDBC_CONNECTION_URL, JDBC_CONNECTION_PROPERTIES);
             final Statement statement = connection.createStatement();
             final ResultSet resultSet = statement.executeQuery(selectSqlStatement)) {
            if (resultSet.next()) {
                final String[] screenshotFilePaths = (String[]) resultSet.getArray("screenshot_filepaths").getArray();

                final List<String> screenshotFileNames = Arrays.stream(screenshotFilePaths)
                        .map(File::new)
                        .map(File::getName)
                        .collect(toList());

                return new ScreenshotResult(jobId, screenshotFileNames);
            } else {
                throw new NoSuchElementException();
            }
        }
    }

    private static void executeInsertSql(final String insertSql) throws SQLException {
        LOGGER.debug("Executing SQL statement '{}'", insertSql);

        try (final Connection connection = DriverManager.getConnection(JDBC_CONNECTION_URL, JDBC_CONNECTION_PROPERTIES);
             final Statement statement = connection.createStatement()) { // TODO: Use a PreparedStatement instead
            statement.execute(insertSql);
        }
    }

    private static int executeInsertSqlWithReturnId(final String insertSqlWithReturnId) throws SQLException {
        LOGGER.debug("Executing SQL statement '{}'", insertSqlWithReturnId);

        try (final Connection connection = DriverManager.getConnection(JDBC_CONNECTION_URL, JDBC_CONNECTION_PROPERTIES);
             final Statement statement = connection.createStatement();
             final ResultSet resultSet = statement.executeQuery(insertSqlWithReturnId)) {

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
        }

        throw new IllegalStateException("No ID was returned from the DB, but no exception was raised");
    }
}
