# War Camel Servlet QuickStart

This example demonstrates how you can use Servlet to expose a http
service in a Camel route, and run that in a servlet container such as
Apache Tomcat. 


### Building

The example can be built with

    mvn clean install


### Running the example locally

The example can be run locally using the following Maven goal:

    mvn clean install jetty:run

Then you can access the service from a web browser using the following url:

    http://localhost:8080/


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


### Access services using a web browser

When the application is running, you can use a web browser to access the HTTP service. Assuming that you
have a [Vagrant setup](http://fabric8.io/guide/getStarted/vagrant.html) you can access the REST service with
`http://war-camel-servlet-default.vagrant.f8/`.

Notice: As it depends on your OpenShift setup, the hostname (route) might vary. Verify with `oc get routes` which
hostname is valid for you.


### More details

You can find more details about running this [quickstart](http://fabric8.io/guide/quickstarts/running.html) on the website. This also includes instructions how to change the Docker image user and registry.

