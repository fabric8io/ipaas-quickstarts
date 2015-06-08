#!/bin/bash

# Wait for ES to start up properly
until $(curl -s -o /dev/null --connect-timeout 1 -m 1 --head --fail http://localhost:9200/); do
    sleep 0.1;
done

if $(curl -s -o /dev/null http://localhost:9200/_template/logstash); then
    curl -s -o /dev/null -XPUT -d@/logstash-template.json http://localhost:9200/_template/logstash
fi

sleep infinity