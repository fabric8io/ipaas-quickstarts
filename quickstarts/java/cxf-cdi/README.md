Note that these instructions are out of date for v2. We need to update these soon.

# REST with CXF and CDI QuickStart

This quick start demonstrates how to create a RESTful (JAX-RS) web service using Apache CXF and expose it using CDI running in a Java standalone container.

The REST service provides a customer service that supports the following operations
 
- PUT /customerservice/customers/ - to create or update a customer
- GET /customerservice/customers/{id} - to view a customer with the given id
- DELETE /customerservice/customers/{id} - to delete a customer with the given id
- GET /customerservice/orders/{orderId} - to view an order with the given id
- GET /customerservice/orders/{orderId}/products/{productId} - to view a specific product on an order with the given id

When the application is deployed, you can access the REST service using a web browser.


### How to try this example

To use the application be sure to have deployed the quickstart in fabric8 as described above. 

### Access services using a web browser

You can use any browser to perform a HTTP GET.  This allows you to very easily test a few of the RESTful services we defined:

Notice: As fabric8 assigns a free dynamic port to the Java container, the port number may vary on your system.

Use this URL to display the root of the REST service, which also allows to access the WADL of the service:

    http://localhost:8080/cxfcdi

Use this URL to display the XML representation for customer 123:

    http://localhost:8080/cxfcdi/cxfcdi/customerservice/customers/123

You can also access the XML representation for order 223 ...

    http://localhost:8080/rest/cxf/customerservice/orders/223

**Note:** if you use Safari, you will only see the text elements but not the XML tags - you can view the entire document with 'View Source'


### To run a command-line utility:

You can use a command-line utility, such as cURL or wget, to perform the HTTP requests.  We have provided a few files with sample XML representations in `src/test/resources`, so we will use those for testing our services.

1. Open a command prompt and change directory to `cxf-cdi`.
2. Run the following curl commands (curl commands may not be available on all platforms):
    
    * Create a customer
 
            curl -X POST -T src/test/resources/add_customer.xml -H "Content-Type: text/xml" http://localhost:8080/cxfcdi/cxfcdi/customerservice/customers
  
    * Retrieve the customer instance with id 123
    
            curl http://localhost:8080/cxfcdi/cxfcdi/customerservice/customers/123

    * Update the customer instance with id 123
  
            curl -X PUT -T src/test/resources/update_customer.xml -H "Content-Type: text/xml" http://localhost:8080/cxfcdi/cxfcdi/customerservice/customers

    * Delete the customer instance with id 123
  
             curl -X DELETE http://localhost:8080/cxfcdi/cxfcdi/customerservice/customers/123



