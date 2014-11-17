mvn clean install docker:build
docker push $DOCKER_REGISTRY/fabric8/quickstart-karaf-camel-amq:2.0-SNAPSHOT
mvn fabric8:run
