#!/bin/bash

cp -rf /opt/jboss/upload/modules /opt/jboss/infinispan-server/
cp -rf /opt/jboss/upload/standalone /opt/jboss/infinispan-server/

export JAVA_OPTS="-Djava.net.preferIPv4Stack=true"
export KUBERNETES_TRUST_CERT="true"
export IP=$(ip addr show eth0 | grep -E '^\s*inet' | grep -m1 global | awk '{ print $2 }' | sed 's|/.*||')
export export KUBERNETES_TRUST_CERT="true"

echo "Starting Clustered Infinispan Server using Docker Address: $IP"
sh /opt/jboss/infinispan-server/bin/clustered.sh -b $IP -bmanagement $IP -c clustered.xml