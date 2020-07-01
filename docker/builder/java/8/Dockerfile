FROM maven:3.6.3-openjdk-8

RUN mkdir --parents /screenshot-service/
WORKDIR /screenshot-service/

# Copying all the pom.xml files so we can run a 'mvn verify clean', which will download all dependencies.
# We do this before copying the source code to cache the docker layer.
# If we change the source code, we won't need to re-download the dependencies.
# However, any changes to the pom.xmls will invalidate the docker layer and require a rebuild.
#
# Failures in the mvn command can be ignored, we are only interested in downloading the dependencies.
#
# In a real scenario, probably would build the artifacts locally and copy into
# some test container in a CICD flow, to leverage the local .m2 on the Jenkins server.
COPY ./pom.xml ./pom.xml
COPY ./screenshot-service-api/pom.xml ./screenshot-service-api/pom.xml
COPY ./screenshot-service-ear/pom.xml ./screenshot-service-ear/pom.xml
COPY ./screenshot-service-jar/pom.xml ./screenshot-service-jar/pom.xml
COPY ./screenshot-service-war/pom.xml ./screenshot-service-war/pom.xml
COPY ./testsuite/pom.xml ./testsuite/pom.xml

RUN mvn verify clean --fail-never


# Copy source code
COPY ./screenshot-service-api ./screenshot-service-api
COPY ./screenshot-service-ear ./screenshot-service-ear
COPY ./screenshot-service-jar ./screenshot-service-jar
COPY ./screenshot-service-war ./screenshot-service-war
# Not used to build the output, but listed in the root pom.xml, so needs to be added
COPY ./testsuite ./testsuite


# Build artifacts
RUN mvn clean install


# Output
RUN mkdir --parents /opt/jboss/wildfly/standalone/deployments/
RUN mv ./screenshot-service-ear/target/*.ear /opt/jboss/wildfly/standalone/deployments/screenshot-service-ear.ear

# Override default command (which tries to execute a maven command)
ENTRYPOINT ["bash"]