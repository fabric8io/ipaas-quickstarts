#!/bin/bash
echo "Hit ctrl+c to stop"
while :
do
	curl curl http://qs-cdi-camel-jetty.vagrant.f8/camel/hello?name=donald
	echo ""
    sleep 1
done
