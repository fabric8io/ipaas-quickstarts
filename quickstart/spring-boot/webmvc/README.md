# Web MVC with Spring-Boot QuickStart

This example assumes that you have a running OpenShift V3.
For more info see http://fabric8.io/guide/getStarted.html.

This example demonstrates how you can use Spring Boot and Spring MVC with as standalone Docker
Container based on [fabric8's base images](https://github.com/fabric8io/base-images#java-base-images)
The quickstart uses Spring Boot to configure a little Spring MVC application that offers a REST service.

When the application is running, you can use a web browser to access the REST service. Assuming that you 
have a [Vagrant setup](http://fabric8.io/guide/getStarted/vagrant.html) you can access the REST service with
`http://springboot-webmvc.vagrant.f8/`.

The URL `http://springboot-webmvc.vagrant.f8/ip` can be used to obtain the IP address to show service load-balancing 
when running with multiple pods.
