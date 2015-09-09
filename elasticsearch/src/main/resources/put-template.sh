#!/bin/bash

set -exo pipefail

# Wait for ES to start up properly
until $(curl -s -f -o /dev/null --connect-timeout 1 -m 1 --head http://localhost:9200/); do
    sleep 0.1;
done

# If the logstash template doesn't already exist...
if ! [ $(curl -s -f -o /dev/null http://localhost:9200/_template/logstash) ]; then
    curl -s -f -o /dev/null -XPUT -d@/logstash-template.json http://localhost:9200/_template/logstash
fi

sleep infinity