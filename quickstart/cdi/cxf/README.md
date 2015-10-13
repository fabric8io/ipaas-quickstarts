# CDI CXF QuickStart

This quick start demonstrates how to create a RESTful (JAX-RS) web service using Apache CXF and expose it using CDI running in a Java standalone container.

The REST service provides a customer service that supports the following operations
 
- PUT /customerservice/customers/ - to create or update a customer
- GET /customerservice/customers/{id} - to view a customer with the given id
- DELETE /customerservice/customers/{id} - to delete a customer with the given id
- GET /customerservice/orders/{orderId} - to view an order with the given id
- GET /customerservice/orders/{orderId}/products/{productId} - to view a specific product on an order with the given id

When the application is deployed, you can access the REST service using a web browser.


### Building

The example can be built with

    mvn clean install


### Running the example locally

The example can be run locally using the following Maven goal:

    mvn clean install exec:java


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


### Access services using a web browser

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the RESTful services we defined:

Notice: As it depends on your OpenShift setup, the hostname (route) might vary. Verify with `oc get routes` which 
hostname is valid for you

Use this URL to display the root of the REST service, which also allows to access the WADL of the service:

    http://quickstart-java-cxf-cdi.vagrant.f8/cxfcdi

Use this URL to display the XML representation for customer 123:

    http://quickstart-java-cxf-cdi.vagrant.f8/cxfcdi/customerservice/customers/123

You can also access the XML representation for order 223 ...

    http://quickstart-java-cxf-cdi.vagrant.f8/rest/cxf/customerservice/customers/123

**Note:** if you use Safari, you will only see the text elements but not the XML tags - you can view the entire document with 'View Source'


### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/test/resources`, so we will use those for testing our services.

1. Open a command prompt and change directory to `cxf-cdi`.
2. Run the following curl commands (curl commands may not be available on all platforms):
    
    * Create a customer
 
            curl -X POST -T src/test/resources/add_customer.xml -H "Content-Type: text/xml" http://quickstart-java-cxf-cdi.vagrant.f8/cxfcdi/customerservice/customers
  
    * Retrieve the customer instance with id 123
    
            curl http://localhost:8080/cxfcdi/cxfcdi/customerservice/customers/123

    * Update the customer instance with id 123
  
            curl -X PUT -T src/test/resources/update_customer.xml -H "Content-Type: text/xml" http://quickstart-java-cxf-cdi.vagrant.f8/cxfcdi/customerservice/customers

    * Delete the customer instance with id 123
  
             curl -X DELETE http://quickstart-java-cxf-cdi.vagrant.f8/cxfcdi/customerservice/customers/123


### More details

You can find more details about running the quickstart [examples](http://fabric8.io/guide/getStarted/example.html) on the website.

