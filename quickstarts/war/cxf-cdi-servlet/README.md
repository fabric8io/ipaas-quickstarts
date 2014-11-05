# REST with CXF and CDI as a WAR QuickStart

This quick start demonstrates how to create a RESTful (JAX-RS) web service using Apache CXF and expose it using CDI running in servlet container as a war

The REST service provides a customer service that supports the following operations
 
- PUT /customerservice/customers/ - to create or update a customer
- GET /customerservice/customers/{id} - to view a customer with the given id
- DELETE /customerservice/customers/{id} - to delete a customer with the given id
- GET /customerservice/orders/{orderId} - to view an order with the given id
- GET /customerservice/orders/{orderId}/products/{productId} - to view a specific product on an order with the given id

When the application is deployed, you can access the REST service using a web browser as shown in the screenshot below:

![Standalone REST diagram](https://raw.githubusercontent.com/fabric8io/fabric8/master/docs/images/cxfcdi-rest.png)


### Building this example

The example comes as source code and pre-built binaries with the fabric8 distribution. 

To try the example you do not need to build from source first. Although building from source allows you to modify the source code, and re-deploy the changes to fabric. See more details on the fabric8 website about the [developer workflow](http://fabric8.io/gitbook/developer.html).

To build from the source code:

1. Change your working directory to `quickstarts/war/cxf-cdi-servlet` directory.
1. Run `mvn clean install docker:build` to build the quickstart.

After building from the source code, you can upload the changes to the fabric container:

1. It is assumed that you have already installed docker,openshift v3 and Fabric8 v2 on your machine as described [here](http://fabric8.io/v2/getStarted.html).
1. Change your working directory to `quickstarts/war/cxf-cdi-servlet` directory.
1. Run `./push.sh` to push this quickstart image to local private docker registry.
1. Run `mvn fabric8:run` to deploy the image to docker container


## How to run this example
1. Run `mvn fabric8:run` to deploy the image to docker container

### How to try this example

To use the application be sure to have deployed the quickstart in fabric8 as described above. 

### Access services using a web browser

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the RESTful services we defined:

Notice: As fabric8 assigns a free dynamic port to the Java container, the port number may vary on your system.

Use this URL to display the root of the REST service, which also allows to access the WADL of the service:

    http://dockerhost:9901/war-cxf-cdi-servlet-2.0.0-SNAPSHOT/cxfcdi/customerservice?_wadl

Use this URL to display the XML representation for customer 123:

    http://dockerhost:9901/war-cxf-cdi-servlet-2.0.0-SNAPSHOT/cxfcdi/customerservice/customers/123

You can also access the XML representation for order 223 ...

    http://dockerhost:9901/war-cxf-cdi-servlet-2.0.0-SNAPSHOT/cxfcdi/customerservice/orders/223
 
**Note:** if you use Safari, you will only see the text elements but not the XML tags - you can view the entire document with 'View Source'


### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/test/resources`, so we will use those for testing our services.

1. Open a command prompt and change directory to `cxf-cdi`.
2. Run the following curl commands (curl commands may not be available on all platforms):
    
    * Create a customer
 
            curl -X POST -T src/test/resources/add_customer.xml -H "Content-Type: text/xml" http://dockerhost:9901/war-cxf-cdi-servlet-2.0.0-SNAPSHOT/cxfcdi/customerservice/customers
  
    * Retrieve the customer instance with id 123
    
            curl http://dockerhost:9901/war-cxf-cdi-servlet-2.0.0-SNAPSHOT/cxfcdi/customerservice/customers/123

    * Update the customer instance with id 123
  
            curl -X PUT -T src/test/resources/update_customer.xml -H "Content-Type: text/xml" http://dockerhost:9901/war-cxf-cdi-servlet-2.0.0-SNAPSHOT/cxfcdi/customerservice/customers

    * Delete the customer instance with id 123
  
             curl -X DELETE http://dockerhost:9901/war-cxf-cdi-servlet-2.0.0-SNAPSHOT/cxfcdi/customerservice/customers/123

### To run the maven test profile:

    run `mvn -Ptest` to run the java test code against the servlet war
