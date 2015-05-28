# Camel Spring QuickStart

This quickstarts run in a Java standalone container, using Spring with Apache Camel.

This example is implemented using solely a Spring XML file (there is no custom Java code).
The source code is provided in the following XML file `src/main/resources/META-INF/spring/camel-context.xml`,
which can be viewed from [github](https://github.com/fabric8io/fabric8/blob/master/quickstarts/java/camel-spring/src/main/resources/META-INF/spring/camel-context.xml).

This example uses a timer to trigger a message every 5th second containing a random number, which is routed using a content based router.

### Run locally

You can run this example locally using

    mvn camel:run

