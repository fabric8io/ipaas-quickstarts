# Enable the jolokia agent configured using etc/jolokia.properties
# TODO: Does not work when deploying to k8s as the base image already have jolokia
# JAVA_OPTIONS="${JAVA_OPTIONS} -javaagent:agents/jolokia.jar=config=etc/jolokia.properties"
# export JAVA_OPTIONS