# War Wildlfy QuickStart

This quickstart exposes a simple application with a simple REST service deployed on top of Wildfly 9.0.2.Final. 
This service can be called from the home page of the application.

The example is based on Markus Eisele blog post:
http://blog.eisele.net/2015/07/running-wildfly-on-openshift-3-with-kubernetes-fabric8-on-windows.html

### Building

The example can be built with

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

### Access services using a web browser

When the application is running, you can use a web browser to access the HTTP service. Assuming that you
have a [Vagrant setup](http://fabric8.io/guide/getStarted/vagrant.html) you can access the application with
`http://war-wildfly-default.vagrant.f8/`.

Notice: As it depends on your OpenShift setup, the hostname (route) might vary. Verify with `oc get routes` which
hostname is valid for you.

### More details

You can find more details about running this [quickstart](http://fabric8.io/guide/quickstarts/running.html) on the website. This also includes instructions how to change the Docker image user and registry.
