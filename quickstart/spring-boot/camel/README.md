# Camel with Spring-Boot QuickStart

This example demonstrates how you can use Apache Camel with Spring Boot 
based on a [fabric8 Java base image](https://github.com/fabric8io/base-images#java-base-images)

The quickstart uses Spring Boot to configure a little application that includes a Camel 
route that triggeres a message every 5th second, and routes the message to a log.

You can run this example locally using the spring-boot Maven goal:

    mvn spring-boot:run

