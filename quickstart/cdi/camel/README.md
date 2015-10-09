# Camel CDI QuickStart

This example shows how to work with Camel in the Java Container using CDI to configure components,
endpoints and beans.

This example is implemented using Java code with CDI injected resources.
The source code is provided in the following java file `src/main/java/io/fabric8/quickstarts/camelcdi/MyRoute.java`,
which can be viewed from [github](https://github.com/fabric8io/quickstarts/blob/master/quickstarts/java/camel-cdi/src/main/java/io/fabric8/quickstarts/camelcdi/MyRoute.java).

This example pickup incoming files, calls a Java bean `SomeBean` to transform the message, and writes the output to a file.

The example can be run locally using the following Maven goal:

    mvn clean install exec:java

# Docker Image configuration

This example specifies an image for the user **fabric8**, using the default registry (`index.docker.io`). Obviously 
won't be able to push this image to `index.docker.io` because this must be done with permissions for the account `fabric8`. 

If you want to push the image to you own account on Docker hub, use the option `-Dfabric8.dockerUser` to specify your
account:

    mvn clean install docker:build docker:push -Dfabric8.dockerUser=morlock

Authentication for this user *morlock* must be done as described in the 
[manual for the docker-maven-plugin](https://github.com/rhuss/docker-maven-plugin). E.g. you can use `-Ddocker.username` 
and `-Ddocker.password` for specifying the credentials.

Alternatively you can push the image also to another registry, like the OpenShift internal registry. Assuming that
you use the [fabric8 Vagrant image](http://fabric8.io/guide/getStarted/vagrant.html) and have set up the routes properly, 
the OpenShift registry is available as `docker-registry.vagrant.f8:80`. If your OpenShift user is authenticated 
against Docker as desribed in the [OpenShift Documentation](https://docs.openshift.com/enterprise/3.0/install_config/install/docker_registry.html#access)
and a project **fabric8** exists (`oc new-project fabric8` if required), then you can push to this registry with

    mvn clean install docker:build docker:push -Dfabric8.dockerPrefix="docker-registry.vagrant.f8:80/" \
                                               -Ddocker.username=$(oc whoami) \
                                               -Ddocker.password=$(oc whoami -t)

Please note the trailing `/` at the end of the prefix. This is mandatory.
 