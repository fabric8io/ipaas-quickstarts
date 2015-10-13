# Java Simple QuickStart

This quickstarts run in a Java standalone container using the jar style.

This example is implemented using very simple Java code.
The source code is provided in the following java file `src/main/java/io/fabric8/quickstarts/java/simple/Main.java`,
which can be viewed from [github](https://github.com/fabric8io/ipaas-quickstarts/blob/master/quickstart/java/simple-mainclass/src/main/java/io/fabric8/quickstarts/java/simple/Main.java).

This example is printing *Hello Fabric8! Here's your random string: lRaNR* to the standard output in the infinite loop.


### Building

The example can be built with

    mvn clean install


### Running the example locally

The example can be run locally using the following Maven goal:

    mvn exec:java


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

