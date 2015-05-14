## Fabric8 Apps

The following [Apps](http://fabric8.io/guide/apps.html) are included in the Fabric8 distribution which can be easily [installed on an existing Kubernetes or OpenShift environment](http://fabric8.io/guide/fabric8OnOpenShift.html).

* [Base](base) provides the [Console](http://fabric8.io/guide/console.html) with the [App Library](appLibrary.html) 
* [Management](management) adds to [Base](base):
    * [Logging](logging) provides consolidated logging and visualisation of log statements and events across your environment
    * [Metrics](metrics) provides consolidated historical metric collection and visualisation across your environment
(replicationControllers.html) and [services](http://fabric8.io/guide/services.html)
* [Continuous Delivery](cdelivery) 
    * [CD Core](cdelivery-core) using [Gogs](http://gogs.io/), [Jenkins](https://jenkins-ci.org/), [Nexus](http://www.sonatype.org/nexus/) and [SonarQube](http://www.sonarqube.org/)
    * [Chat](http://fabric8.io/guide/chat.html) provides a [hubot](https://hubot.github.com/) integration with the CD infrastructure
* [iPaaS](ipaas) provides an _Integration Platform As A Service_  
    * [API Registry](http://fabric8.io/guide/apiRegistry.html) provides a global view of all of your RESTful and web service APIs that is displayed in the [Console](http://fabric8.io/guide/console.html) allowing you to inspect and invoke all the endpoints 
    * [MQ](http://fabric8.io/guide/fabric8MQ.html) implements _Messaging As A Service_ with [Apache ActiveMQ](http://activemq.apache.org/) on Kubernetes.
    * [MQ AutoScaler](http://fabric8.io/guide/fabric8MQAutoScaler.html) monitors and scales the [Apache ActiveMQ](http://activemq.apache.org/) brokers running on Kubernetes
