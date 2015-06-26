#!/bin/bash

set -exo pipefail

ELASTICSEARCH_URL=${ELASTICSEARCH_URL:-http://localhost:9200}

if [ -n "${ELASTICSEARCH_SERVICE_NAME}" ]; then
    SVC_HOST=${ELASTICSEARCH_SERVICE_NAME}_SERVICE_HOST
    SVC_PORT=${ELASTICSEARCH_SERVICE_NAME}_SERVICE_PORT
    ELASTICSEARCH_URL=http://${!SVC_HOST}:${!SVC_PORT}
fi

# Wait for ES to start up properly
until $(curl -s -f -o /dev/null --connect-timeout 1 -m 1 --head ${ELASTICSEARCH_URL}); do
    sleep 0.1;
done

if ! [ $(curl -s -f -o /dev/null ${ELASTICSEARCH_URL}/.kibana) ]; then
    curl -s -f -XPUT -d@/index-pattern.json "${ELASTICSEARCH_URL}/.kibana/index-pattern/\[logstash-\]YYYY.MM.DD"
    curl -s -f -XPUT -d@/fabric8-search.json "${ELASTICSEARCH_URL}/.kibana/search/Fabric8"
    curl -s -f -XPUT -d@/fabric8-dashboard.json "${ELASTICSEARCH_URL}/.kibana/dashboard/Fabric8"
    curl -s -f -XPUT -d@/kibana-config.json "${ELASTICSEARCH_URL}/.kibana/config/4.1.0"
fi

sleep infinity