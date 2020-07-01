package me.zodac.util;

/**
 * Utility class used to retrieve environment variables.
 */
public final class EnvironmentUtils {

    private EnvironmentUtils() {

    }

    /**
     * Returns the {@code propertyName} specified if it is set as a {@link System} property or as a {@link System} environment variable. The order of
     * the search is property first then environment variable.
     * <p>
     * If neither is set, then null is returned.
     *
     * @param propertyName the property to search for
     * @return the value of the searched property, otherwise {@code null}
     */
    public static String getEnvironmentValue(final String propertyName) {
        return System.getProperty(propertyName, System.getenv(propertyName));
    }
}
