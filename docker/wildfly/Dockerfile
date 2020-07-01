FROM jboss/wildfly:19.1.0.Final
RUN /opt/jboss/wildfly/bin/add-user.sh root shroot --silent;

USER root
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]