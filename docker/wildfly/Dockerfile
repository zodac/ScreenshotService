FROM jboss/wildfly:19.1.0.Final
RUN /opt/jboss/wildfly/bin/add-user.sh root shroot --silent;
COPY ./ear/* /opt/jboss/wildfly/standalone/deployments/

USER root
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]