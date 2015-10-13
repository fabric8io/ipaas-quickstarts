# Camel CDI HTTP QuickStart

This example shows how to work with Camel in the Java Container using CDI to configure components,
endpoints and beans.

This quickstart is the client side which embeds a Camel route that triggers every 5th second,
and calls a remote HTTP service and logs the response.

The server side provides the remote HTTP service is the `quickstart-cdi-camel-jetty` quicstart which must be up and running.


### Building

The example can be built with

    mvn clean install


### Running the example locally

The example can be run locally using the following Maven goal:

    mvn clean install exec:java


### Running the example in fabric8

It is assumed a running Kubernetes platform is already running. If not you can find details how to [get started](http://fabric8.io/guide/getStarted/index.html).

The example must be built first using

    mvn clean install docker:build

Then the example can be deployed using:

    mvn fabric8:json fabric8:apply

When the example runs in fabric8, you can use the OpenShift client tool to inspect the status

To list all the running pods:

    oc get pods

Then find the name of the pod that runs this quickstart, and output the logs from the running pods with:

    oc logs <name of pod>

You can also use the fabric8 [web console](http://fabric8.io/guide/console.html) to manage the
running pods, and view logs and much more.


## Calling the remote service from a shell script

You can also call the remote HTTP service from a shell script. We have provided a script named `src/test/resources/hitme-f8.sh` (no script for windows)
in the source code for the quickstart, not in the docker image, which will call the service once per second.

You may need to add execution permission to the script before you can execute it

    chmod +x src/test/resources/hitme-f8.sh

And then run the script

    src/test/resources/hitme-f8.sh

While the script runs, you can try to scale up or down the number of pods on the remote HTTP service using either the fabric8 web console,
or from the command line using the openshift client

    oc scale --replicas=3 replicationcontrollers quickstart-cdi-camel-jetty


### More details

You can find more details about running the quickstart [examples](http://fabric8.io/guide/getStarted/example.html) on the website.



