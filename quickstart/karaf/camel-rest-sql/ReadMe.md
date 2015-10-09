This quickstart demonstrates how to use SQL via JDBC along with Camel's REST DSL to expose a RESTful API.

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

