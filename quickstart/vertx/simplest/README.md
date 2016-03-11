# Vert.x Simples QuickStart

This quickstart run an embedded vert.x HTTP server in a standalone Java application.

This example is implemented using very simple Java code.

This example is printing *Hello World! from pod HOSTNAME* as reply message when calling the HTTP server, which runs on port 8080.


### Building

Navigate to the $IPAAS_QUICKSTART/quickstart/vertx/simplest/ folder and the example can be built with

    mvn clean install


### Running the example locally

The example can be run locally using the following Maven goal:

    mvn exec:java

You can then call the service from a webbrowser using `http://localhost:8080`


### Running the example in fabric8

It is assumed a running Kubernetes platform is already running. If not you can find details how to [get started](http://fabric8.io/guide/getStarted/index.html).

The example can be built and deployed using a single goal:

    mvn -Pf8-local-deploy

When the example runs in fabric8, you can use the OpenShift client tool to inspect the status

To list all the running pods:

    oc get pods

### Access services using a web browser

When the application is running, you can use a web browser to access the service. Assuming that you
have a [Vagrant setup](http://fabric8.io/guide/getStarted/vagrant.html) you can access the service with
`http://vertx-simplest-default.vagrant.f8/`.

Notice: As it depends on your OpenShift setup, the hostname (route) might vary. Verify with `oc get routes` which
hostname is valid for you.

You can also use the fabric8 [web console](http://fabric8.io/guide/console.html) to manage the
running pods, and view logs and much more.

### More details

You can find more details about running this [quickstart](http://fabric8.io/guide/quickstarts/running.html) on the website. This also includes instructions how to change the Docker image user and registry.

