#!/bin/bash
mvn clean install
mvn fabric8:zip fabric8:deploy
