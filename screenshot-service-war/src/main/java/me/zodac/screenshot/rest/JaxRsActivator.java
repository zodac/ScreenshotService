package me.zodac.screenshot.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * A class extending {@link Application} and annotated with @ApplicationPath is the Java EE 6 "no XML" approach to activating JAX-RS.
 * <p>
 * Resources are served relative to the servlet path specified in the {@link ApplicationPath} annotation.
 */
@ApplicationPath("/")
public class JaxRsActivator extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<>();
        classes.add(ScreenshotEndpoint.class);
        return classes;
    }
}