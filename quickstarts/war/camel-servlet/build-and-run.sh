#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/quickstart/war-camel-servlet:2.0-SNAPSHOT
mvn fabric8:run
