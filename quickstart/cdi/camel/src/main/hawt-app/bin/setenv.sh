# Enable the jolokia agent configured using etc/jolokia.properties
JAVA_OPTIONS="${JAVA_OPTIONS} -javaagent:agents/jolokia.jar=config=etc/jolokia.properties"
export JAVA_OPTIONS