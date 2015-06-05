# Camel CDI QuickStart

This example shows how to work with Camel in the Java Container using CDI to configure components,
endpoints and beans.

This example is implemented using Java code with CDI injected resources.
The source code is provided in the following java file `src/main/java/io/fabric8/quickstarts/camelcdi/MyRoute.java`,
which can be viewed from [github](https://github.com/fabric8io/quickstarts/blob/master/quickstarts/java/camel-cdi/src/main/java/io/fabric8/quickstarts/camelcdi/MyRoute.java).

This example pickup incoming files, calls a Java bean `SomeBean` to transform the message, and writes the output to a file.
