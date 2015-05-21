#!/bin/bash

# Clean up the build and install it
mvn clean install docker:build

[ x"$DOCKER_REGISTRY" != x ] || ( echo "No \$DOCKER_REGISTRY set" && exit 1 )

extra=""
if [ $# -gt 0 ]; then
    if [ $# = 2 ]; then
        extra="-Ddocker.username=$1 -Ddocker.password=$2"
    else
        echo "No password given"
        echo "Usage: $0 [<user> <password>]"
        exit 1;
    fi;
fi
mvn docker:push -Ddocker.registry=$DOCKER_REGISTRY $extra

mvn fabric8:apply
