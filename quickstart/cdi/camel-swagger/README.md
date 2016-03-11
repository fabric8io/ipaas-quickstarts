# Camel Swagger CDI Example

### Introduction
This is an example that uses the rest-dsl to define a rest services which provides three operations

- GET user/{id}     - Find user by id
- PUT user          - Updates or create a user
- GET user/findAll  - Find all users

### Build
You will need to compile this example first:

	mvn install

### Run
To run the example type

	mvn exec:java

The rest service can be accessed from the following url

	curl http://localhost:8080/user

<http://localhost:8080/user>

For example to get a user with id 123

	curl http://localhost:8080/user/123

<http://localhost:8080/user/123>

The rest services provides Swagger API which can be accessed from the following url

    curl http://localhost:8080/swagger.json

<http://localhost:8080/swagger.json>

To stop the example hit <kbd>ctrl</kbd>+<kbd>c</kbd>

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

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the RESTful services we defined:

Notice: As it depends on your OpenShift setup, the hostname (route) might vary. Verify with `oc get routes` which hostname is valid for you.

Use this URL to display the root of the REST service, which also allows to access the WADL of the service:

Use this URL to display the JSON representation for customer 123:

    http://cdi-camel-swagger-default.vagrant.f8/user/123

where `vagrant.f8` is your Kubernetes domain. 

### More details

You can find more details about running this [quickstart](http://fabric8.io/guide/quickstarts/running.html) on the website. This also includes instructions how to change the Docker image user and registry.


