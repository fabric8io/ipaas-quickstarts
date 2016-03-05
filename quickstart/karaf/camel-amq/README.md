# Camel AMQ QuickStart

This quickstart demonstrates how to connect to an ActiveMQ broker and use JMS messaging between two Camel routes.

In this example we will use two containers, one container to run as a ActiveMQ broker, and another as a client to the broker, where the Camel routes is running.

This quickstart requires the ActiveMQ broker has been deployed and running first. This can be done from the web console from the `Apps` page, and then install the `messaging` application.

To install this quickstart from the command line type

The example can be built and deployed using a single goal:

    mvn -Pf8-local-deploy

This requires you have [setup your local computer](http://fabric8.io/guide/getStarted/develop.html) to work with docker and kubernetes.
