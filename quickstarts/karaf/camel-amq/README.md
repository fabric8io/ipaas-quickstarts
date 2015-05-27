# Camel AMQ QuickStart

This quickstart demonstrates how to connect to the local ActiveMQ broker and use JMS messaging between two Camel routes.

In this quickstart, orders from zoos all over the world will be copied from the input directory into a specific
output directory per country.

In this example we will use two containers, one container to run as a standalone ActiveMQ broker, and another as a client to the broker, where the Camel routes is running.

The two Camel routes send and receives JMS message using the `amq:incomingOrders` endpoint, which is a queue on the ActiveMQ broker.

