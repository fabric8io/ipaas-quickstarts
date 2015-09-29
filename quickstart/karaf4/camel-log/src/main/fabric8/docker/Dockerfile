FROM ${docker.from}
MAINTAINER fabric8@googlegroups.com

# environment variables
ENV JAVA_HOME=/usr/lib/jvm/java
ENV KARAF_VERSION=4.0.0
ENV KARAF_HOME=/opt/jboss/karaf
ENV KARAF_OPTS=-javaagent:/opt/jboss/karaf/jolokia-agent.jar=host=0.0.0.0,port=8778,authMode=jaas,realm=karaf,user=admin,password=admin

# karaf ports
EXPOSE 8181 8778

# copy all content from maven directory used by docker plugin to /opt/jboss
COPY maven /opt/jboss/
USER root
RUN ["chown", "-R", "jboss:jboss","/opt/jboss/"]
USER jboss

# extract karaf distribution
RUN tar xzf /opt/jboss/quickstart-karaf4-camellog-2.3-SNAPSHOT.tar.gz -C /opt/jboss
RUN rm /opt/jboss/quickstart-karaf4-camellog-2.3-SNAPSHOT.tar.gz
RUN ln -s /opt/jboss/quickstart-karaf4-camellog-2.3-SNAPSHOT /opt/jboss/karaf

# override with custom users.properties
RUN mv /opt/jboss/users.properties /opt/jboss/karaf/etc/

# install jolokia agent
RUN curl http://central.maven.org/maven2/org/jolokia/jolokia-jvm/1.3.1/jolokia-jvm-1.3.1-agent.jar -o /opt/jboss/karaf/jolokia-agent.jar

# send log output to stdout
RUN sed -i 's/^\(.*rootLogger.*\)out/\1stdout/' /opt/jboss/karaf/etc/org.ops4j.pax.logging.cfg

# launch karaf
CMD /opt/jboss/karaf/bin/karaf server
