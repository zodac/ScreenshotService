package me.zodac.db.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link PostgresSqlQueryBuilder}.
 */
public class PostgresSqlQueryBuilderTest {

    @Test
    public void whenCreateInsertStatementForRequest_givenUsernameAndTwoUrls_thenSqlStatementIsCreatedWithAnArrayOfTwoUrls() {
        final List<String> urls = List.of("http://websiteOne.com", "http://websiteTwo.com");

        final String result = PostgresSqlQueryBuilder.insertRequest(urls);

        assertThat(result)
                .as("Generated SQL was invalid")
                .isEqualTo(
                        "INSERT INTO screenshot_request (urls) VALUES (ARRAY ['http://websiteOne.com', 'http://websiteTwo.com']) RETURNING request_id");
    }

    @Test
    public void whenCreateInsertStatementForResult_givenJobIdAndTwoFilePaths_thenSqlStatementIsCreatedWithAnArrayOfTwoFilePaths() {
        final int jobId = 1;
        final List<String> filePaths = List.of("/usr/screenshot/1/screenshot1.png", "/usr/screenshot/1/screenshot2.png");

        final String result = PostgresSqlQueryBuilder.insertResult(jobId, filePaths);

        assertThat(result)
                .as("Generated SQL was invalid")
                .isEqualTo(
                        "INSERT INTO screenshot_result (job_id, screenshot_filepaths) VALUES ('1', ARRAY ['/usr/screenshot/1/screenshot1.png', '/usr/screenshot/1/screenshot2.png'])");
    }
}
