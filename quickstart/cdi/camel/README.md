# CDI Camel QuickStart

This example shows how to work with Camel in the Java Container using CDI to configure components,
endpoints and beans.

This example is implemented using Java code with CDI injected resources such as Camel endpoints and Java beans.


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


### More details

You can find more details about running the quickstart [examples](http://fabric8.io/guide/getStarted/example.html) on the website.

