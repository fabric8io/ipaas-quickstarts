# Camel CDI ActiveMQ QuickStart

This example shows how to use Camel in the Java Container using CDI to connect to an ActiveMQ broker hosted in Kubernetes.

This example is implemented using Java code with CDI injected resources.
The source code uses the CDI Annotation `@ServiceName` to lookup the ActiveMQ broker Service name.

The broker service name can be changed [here](https://github.com/fabric8io/ipaas-quickstarts/blob/master/quickstart/cdi/camel-cdi-mq/src/main/java/io/fabric8/quickstarts/camelcdi/MyRoutes.java#L33) and defaults to `@ServiceName("fabric8mq")`

This example will connect to the broker and send messages to a queue TEST.FOO
