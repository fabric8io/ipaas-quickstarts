# Java Camel Spring QuickStart

This quickstarts run in a Java standalone container, using Spring with Apache Camel.

This example is implemented using solely a Spring XML file (there is no custom Java code).
The source code is provided in the following XML file `src/main/resources/META-INF/spring/camel-context.xml`,
which can be viewed from [github](https://github.com/fabric8io/ipaas-quickstarts/blob/master/quickstart/java/camel-spring/src/main/resources/META-INF/spring/camel-context.xml).

This example uses a timer to trigger a message every 5th second that is routed using a content based router, based on
the message is regarded as high priority or not.


### Building

Navigate to the $IPAAS_QUICKSTART/quickstart/java/camel-spring/ folder and the example can be built with

    mvn clean install


### Running the example locally

The example can be run locally using the following Maven goal:

    mvn camel:run


### Running the example in fabric8

It is assumed a running Kubernetes platform is already running. If not you can find details how to [get started](http://fabric8.io/guide/getStarted/index.html).

The example can be built and deployed using a single goal:

    mvn -Pf8-local-deploy

When the example runs in fabric8, you can use the OpenShift client tool to inspect the status

To list all the running pods:

    oc get pods

Then find the name of the pod that runs this quickstart, and output the logs from the running pods with:

    oc logs <name of pod>

You can also use the fabric8 [web console](http://fabric8.io/guide/console.html) to manage the
running pods, and view logs and much more.


### More details

You can find more details about running this [quickstart](http://fabric8.io/guide/quickstarts/running.html) on the website. This also includes instructions how to change the Docker image user and registry.

