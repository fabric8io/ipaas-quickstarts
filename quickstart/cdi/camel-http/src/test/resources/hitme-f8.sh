#!/bin/bash
echo "Hit ctrl+c to stop"
while :
do
	curl http://quickstart-camelservlet.vagrant.f8/camel/hello?name=donald
	echo ""
    sleep 1
done
