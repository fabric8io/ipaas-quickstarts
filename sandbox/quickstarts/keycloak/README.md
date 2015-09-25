# Web MVC with Spring-Boot & Keycloak QuickStart

This example assumes that you have a running openshift v3, and fabric8 v2 using docker.
For more info see http://fabric8.io/v2/getStarted.html.

You will also need a running & configured [Keycloak](http://keycloak.jboss.org) server.

This example demonstrates how you can use Spring Boot and web mvc with a [Java Container](http://fabric8.io/gitbook/javaContainer.html), using Keycloak for authentication.

The quickstart uses Spring Boot to configure a little Spring MVC application that offers a REST service.

When the application is running, you can use a web browser to access the REST service. but you will need to authenticate first via Keycloak.

### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build and run from the source code:

1. Change your working directory to `quickstarts/spring-boot/keycloak` directory.
1. Run `mvn clean install docker:build fabric8:apply -Dfabric8.recreate` to build and run the quickstart.


### Using the web console

You can deploy and run this example from the web console, as follows

1. It is assumed that you have already created a fabric and are logged into a container called `root`.
1. Login the web console
1. Click the Wiki button in the navigation bar
1. Select `quickstarts` --> `spring-boot` --> `keycloak`
1. Click the `Run` button in the top right corner