# REST with CXF QuickStart

This quick start demonstrates how to create a RESTful (JAX-RS) web service using Apache CXF and expose it in a servlet container such as Apache Tomcat.

The REST service provides a customer service that supports the following operations
 
- PUT /customerservice/customers/ - to create or update a customer
- GET /customerservice/customers/{id} - to view a customer with the given id
- DELETE /customerservice/customers/{id} - to delete a customer with the given id
- GET /customerservice/orders/{orderId} - to view an order with the given id
- GET /customerservice/orders/{orderId}/products/{productId} - to view a specific product on an order with the given id

When the application is deployed in Apache Tomcat, you can use the web console to list the Tomcat applications, and easily access the quickstart by clicking the url on the `rest` application.
