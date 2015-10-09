#!/bin/bash
echo "Hit ctrl+c to stop"
while :
do
	curl http://qs-cdi-camel-jetty-server.vagrant.f8/camel/hello?name=donald
	echo ""
    sleep 1
done
