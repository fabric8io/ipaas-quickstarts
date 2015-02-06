#!/bin/bash

cp -rf /opt/jboss/upload/modules /opt/jboss/infinispan-server/
cp -rf /opt/jboss/upload/standalone /opt/jboss/infinispan-server/

export JAVA_OPTS="-Djava.net.preferIPv4Stack=true"
export KUBERNETES_TRUST_CERT="true"
export DOCKER_IP=$(ip addr show eth0 | grep -E '^\s*inet' | grep -m1 global | awk '{ print $2 }' | sed 's|/.*||')
export export KUBERNETES_TRUST_CERT="true"

/bin/sh /opt/jboss/infinispan-server/bin/clustered.sh -b $DOCKER_IP -bmanagement $DOCKER_IP -c clustered.xml