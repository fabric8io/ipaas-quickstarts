FROM jboss/wildfly:8.2.0.Final
MAINTAINER docker-maven-plugin@jolokia.org
ADD maven /opt/jboss/
USER root
RUN chown -R jboss:jboss /opt/jboss
USER jboss
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-c", "standalone-apiman.xml"]
