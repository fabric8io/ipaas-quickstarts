#!/bin/bash
mvn clean install docker:build
docker push $DOCKER_REGISTRY/quickstart/springboot-webmvc:2.2-SNAPSHOT
mvn fabric8:deploy