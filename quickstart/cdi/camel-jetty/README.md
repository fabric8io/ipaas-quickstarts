# CDI Camel Jetty QuickStart

This example shows how to work with Camel in the Java Container using CDI to configure components,
endpoints and beans.

This quickstart is the server side which embeds a Jetty HTTP server in a Camel route that
exposes a HTTP service.

The `quickstart-cdi-camel-http` is the client to this quickstart that can be started which will call this
Jetty HTTP server every 5 second.


### Building

The example can be built with

    mvn clean install


### Running the example locally

The example can be run locally using the following Maven goal:

    mvn clean install exec:java

And you can access the HTTP service using a web browser on url:

    http://localhost:8080/camel/hello


### Running the example in fabric8

It is assumed a running Kubernetes platform is already running. If not you can find details how to [get started](http://fabric8.io/guide/getStarted/index.html).

The example can be built and deployed using a single goal:

    mvn -Pf8-local-deploy

When the example runs in fabric8, you can use the OpenShift client tool to inspect the status

To list all the running pods:

    oc get pods

Then find the name of the pod that runs this quickstart, and output the logs from the running pods with:

    oc logs <name of pod>

The example exposes a service over HTTP which you can find using

    oc get routes

This lists all the routes to the services, where you can find the actual HTTP url, which you can use from a web browser.

The Camel route is listening on context-path `camel/hello`, so the actual HTTP url should be prefixed with that.
For example if the route to the service is listed at `http://cdi-camel-jetty.vagrant.f8` then the actual URL to use should be `http://cdi-camel-jetty.vagrant.f8/camel/hello`

You can also use the fabric8 [web console](http://fabric8.io/guide/console.html) to manage the
running pods, and view logs and much more.


## Calling the HTTP service from a shell script

You can also call the remote HTTP service from a shell script. We have provided a script named `src/test/resources/hitme-f8.sh` (no script for windows)
in the source code for the quickstart, not in the docker image, which will call the service once per second.

You may need to add execution permission to the script before you can execute it

    chmod +x src/test/resources/hitme-f8.sh

And then run the script

    src/test/resources/hitme-f8.sh

While the script runs, you can try to scale up or down the number of pods on the Jetty HTTP service using either the fabric8 web console,
or from the command line using the openshift client

    oc scale --replicas=3 rc cdi-camel-jetty


### More details

You can find more details about running this [quickstart](http://fabric8.io/guide/quickstarts/running.html) on the website. This also includes instructions how to change the Docker image user and registry.

