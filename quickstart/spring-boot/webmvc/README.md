# Spring-Boot Web MVC QuickStart

This example demonstrates how you can use Spring Boot and Spring MVC with as standalone Docker
Container based on [fabric8's base images](https://github.com/fabric8io/base-images#java-base-images)
The quickstart uses Spring Boot to configure a little Spring MVC application that offers a REST service.


### Building

The example can be built with

    mvn clean install


### Running the example locally

The example can be run locally using the following Maven goal:

    mvn spring-boot:run

Then you can access the service using the following url from a web browser:

    http://localhost:8080/ip


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

When the application is running, you can use a web browser to access the REST service. Assuming that you
have a [Vagrant setup](http://fabric8.io/guide/getStarted/vagrant.html) you can access the REST service with
`http://springboot-webmvc-default.vagrant.f8`.

Notice: As it depends on your OpenShift setup, the hostname (route) might vary. Verify with `oc get routes` which
hostname is valid for you.

The URL `http://springboot-webmvc-default.vagrant.f8/ip` can be used to obtain the IP address to show service load-balancing
when running with multiple pods.


### More details

You can find more details about running this [quickstart](http://fabric8.io/guide/quickstarts/running.html) on the website. This also includes instructions how to change the Docker image user and registry.

