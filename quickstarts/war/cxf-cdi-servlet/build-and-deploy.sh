#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/quickstart/war-cxf-cdi-servlet:2.0-SNAPSHOT
mvn fabric8:deploy
