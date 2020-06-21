package me.zodac.screenshot.api.rest;

/**
 * Application {@link Exception} thrown from the <code>screenshot-service</code> business layer, to be handled in the REST layer.
 */
public class ScreenshotServiceException extends Exception {

    private static final long serialVersionUID = 3566082197594943623L;

    /**
     * Constructor taking in an error message and a cause {@link Throwable}.
     * 
     * @param errorMessage
     *            the error message
     * @param cause
     *            the cause {@link Throwable}
     */
    public ScreenshotServiceException(final String errorMessage, final Throwable cause) {
        super(errorMessage, cause);
    }
}
