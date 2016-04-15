# Java Camel Spring Infinispan QuickStart

This quickstart run in a Java standalone container, using Spring with Apache Camel (Infinispan component)

This example uses a timer to trigger a message every 5th secondi, generating a random number and put this value in an Infinispan cache and then get this value.

This example is related to `infinispan-server` quickstart which must be up and running.

You should be able to see the operations looking at the logs of `infinispan-client`.

### Building

Navigate to the $IPAAS_QUICKSTART/quickstart/infinispan/infinispan-client/ folder and the example can be built with

    mvn clean install

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

