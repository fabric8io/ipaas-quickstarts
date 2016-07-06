#set( $H = '##' )
# Swarm Camel REST Quickstart

${H} Introduction

This quickstart uses WildFly Swarm as Java lightweight container and the Apache Camel Integration Framework to expose a RESTfull endpoint registered within the Undertow server.
This example uses the REST fluent DSL to define a service which provides one operation

- GET api/say/{id}       - Say Hello to the user name

To package the Camel module within the Swarm container, we use a [fraction]() which is customized with the RouteBuilder class containing the Route.
The MainApp class is bootstrapped by the Swarm container when we launch it.

```
public static void main(String[] args) throws Exception {
	Swarm swarm = new Swarm();

	// Camel Fraction
	swarm.fraction(new CamelCoreFraction()
	        .addRouteBuilder(new RestService()));
```

${H} Build

You will need to compile this example first:

    mvn install

${H} Run

To run the example type

    mvn wildfly-swarm:run

The rest service can be accessed from the following url

    curl http://localhost:8080/service/say/{name}
<http://localhost:8080/service/say/{name}>

For example to say Hello for the name `charles`

    curl http://localhost:8080/service/say/charles
<http://localhost:8080/service/say/charles>

The rest services provides Swagger API which can be accessed from the following url

    curl http://localhost:8080/swagger.json
<http://localhost:8080/swagger.json>

To stop the example hit <kbd>ctrl</kbd>+<kbd>c</kbd>

${H} Jolokia & JMX

We have registered the Jolokia fraction in order to access the JMX operations or attributes using the JSon HTTP Servlet Bridge offered by the
[jolokia](https://jolokia.org/reference/html/protocol.html) project.

Here are some curl request that we can use to grab JVM data

```
curl -X GET http://localhost:8080/jmx
curl -d "{\"type\":\"read\",\"mbean\":\"java.lang:type=Memory\",\"attribute\":\"HeapMemoryUsage\",\"path\":\"used\"}" http://localhost:8080/jmx/ && echo ""
```

${H} Running the example in fabric8

It is assumed a Kubernetes platform is already running with or without OpenShift. If not, you can find details how to [get started](http://fabric8.io/guide/getStarted/index.html).

The example can be built and deployed using a single goal:

    mvn -Pf8-local-deploy

When the example runs in fabric8, you can use the OpenShift client tool to inspect the status

To list all the running pods:

    oc get pods

Then find the name of the pod that runs this quickstart, and output the logs from the running pods with:

    oc logs <name of pod>

You can also use the fabric8 web console to manage the running pods, and view logs and much more.

${H} Access services using a web browser

You can use any browser to perform a HTTP GET. This allows you to very easily test a few of the RESTful services we defined:

Notice: As it depends on your OpenShift setup, the hostname (route) might vary. Verify with oc get routes which hostname is valid for you.

Use this URL to display response message from the REST service:

    http://swarm-camel-rest-default.vagrant.f8/service/say/charles

where `vagrant.f8` is your Kubernetes domain and `default`, the namespace of the project

${H} More details

You can find more details about running this quickstart on the website. This also includes instructions how to change the Docker image user and registry.
