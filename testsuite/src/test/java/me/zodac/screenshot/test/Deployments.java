package me.zodac.screenshot.test;

import java.io.File;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.MavenFormatStage;
import org.jboss.shrinkwrap.resolver.api.maven.PomEquippedResolveStage;

/**
 * Deployments utility class to deploy integration test artifacts.
 */
final class Deployments {

    private static final PomEquippedResolveStage BOM_LOCAL = Maven.configureResolver().workOffline().loadPomFromFile("pom.xml");

    private Deployments() {

    }

    /**
     * Creates a test EAR containing the integration test and any required JARs.
     *
     * @return the test EAR
     */
    static EnterpriseArchive getTestEar() {
        final WebArchive ejbJar = ShrinkWrap.create(WebArchive.class, "ScreenshotServiceTestWar.war")
                .addPackages(true, ScreenshotServiceIT.class.getPackage())
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsResource("log4j.properties");

        return ShrinkWrap.create(EnterpriseArchive.class, "ScreenshotServiceTestEar.ear")
                .addAsLibraries(resolveLocalJars("me.zodac", "screenshot-service-api"))
                .addAsLibraries(resolveLocalJars("com.google.code.gson", "gson"))
                .addAsModule(ejbJar);
    }

    private static File[] resolveLocalJars(final String groupId, final String artifactId) {
        final String gav = String.format("%s:%s", groupId, artifactId);
        final MavenFormatStage mavenFormatStage = BOM_LOCAL.resolve(gav).withTransitivity();
        return mavenFormatStage.asFile();
    }
}
