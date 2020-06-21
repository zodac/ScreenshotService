package me.zodac.screenshot.api.rest;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * POJO encapsulating the data required for a request to the <code>screenshot-service</code>.
 */
public class ScreenshotRequest implements Serializable {

    private static final long serialVersionUID = 8015037769615499829L;

    private List<String> urls;

    public ScreenshotRequest() {

    }

    public ScreenshotRequest(final List<String> urls) {
        this.urls = urls;
    }

    public List<String> getUrls() {
        return urls;
    }

    public void setUrls(final List<String> urls) {
        this.urls = urls;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ScreenshotRequest that = (ScreenshotRequest) o;
        return Objects.equals(urls, that.urls);
    }

    @Override
    public int hashCode() {
        return Objects.hash(urls);
    }

    @Override
    public String toString() {
        return String.format("%s::{urls: '%s'}", getClass().getSimpleName(), urls);
    }
}
