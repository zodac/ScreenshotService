package me.zodac.screenshot.api.rest;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * POJO encapsulating the data sent as part of the result from the <code>screenshot-service</code>.
 */
public class ScreenshotResult implements Serializable {

    private static final long serialVersionUID = 6181240132162856104L;

    private int jobId;
    private List<String> screenshotFileNames;

    public ScreenshotResult() {

    }

    public ScreenshotResult(final int jobId, final List<String> screenshotFileNames) {
        this.jobId = jobId;
        this.screenshotFileNames = screenshotFileNames;
    }

    public int getJobId() {
        return jobId;
    }

    public List<String> getScreenshotFileNames() {
        return screenshotFileNames;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ScreenshotResult that = (ScreenshotResult) o;
        return jobId == that.jobId &&
                Objects.equals(screenshotFileNames, that.screenshotFileNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, screenshotFileNames);
    }

    @Override
    public String toString() {
        return String.format("%s::{jobId: '%s', screenshotFileNames: '%s'}", getClass().getSimpleName(), jobId, screenshotFileNames);
    }
}
