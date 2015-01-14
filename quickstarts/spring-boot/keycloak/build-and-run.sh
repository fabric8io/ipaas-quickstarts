#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/quickstart/springboot-keycloak:2.2-SNAPSHOT
mvn fabric8:run
