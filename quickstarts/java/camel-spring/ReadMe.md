# Camel Spring QuickStart

This quickstarts run in a Java standalone container, using Spring with Apache Camel.

This example is implemented using solely a Spring XML file (there is no Java code). The source code is provided in the following XML file `src/main/resources/META-INF/spring/camel-context.xml`, which can be viewed from [github](https://github.com/fabric8io/fabric8/blob/master/quickstarts/java/camel-spring/src/main/resources/META-INF/spring/camel-context.xml).

This example pickup incoming XML files, and depending on the content of the XML files, they are routed to different endpoints.
