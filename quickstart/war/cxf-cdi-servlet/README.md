# REST with CXF and CDI as a WAR QuickStart

This quick start demonstrates how to create a RESTful (JAX-RS) web service using Apache CXF and expose it using CDI running in servlet container as a war

The REST service provides a customer service that supports the following operations
 
- PUT /api/customerservice/customers/ - to create or update a customer
- GET /api/customerservice/customers/{id} - to view a customer with the given id
- DELETE /api/customerservice/customers/{id} - to delete a customer with the given id
- GET /api/customerservice/orders/{orderId} - to view an order with the given id
- GET /api/customerservice/orders/{orderId}/products/{productId} - to view a specific product on an order with the given id

When the application is deployed, you can access the REST service using a web browser.
