# Karaf Camel Log QuickStart

This quickstart demonstrates how to use SQL via JDBC along with Camel's REST DSL to expose a RESTful API.


### Building

The example can be built with

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



### Accessing the rest service

When the example is running, a reset service can be accessed to list available books that can be ordered, and as well order status.

If you run the example on a local fabric8 installation using vagrant, then the REST service is exposes as

    http://qs-camel-rest-sql.vagrant.f8

The actual endpoint is using context-path `camel-rest-sql/books` and the REST service provides two services

- books = to list all the available books that can be ordered
- order/{id} = to output order status for the given order id. The example will automatic create new orders with a running order id starting from 1.

You can from a web browser then access these services such as:

    http://qs-camel-rest-sql.vagrant.f8/camel-rest-sql/books
    http://qs-camel-rest-sql.vagrant.f8/camel-rest-sql/books/order/1


### More details

You can find more details about running the quickstart [examples](http://fabric8.io/guide/getStarted/example.html) on the website.

