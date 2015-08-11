# Wildlfy QuickStart

This example shows a Wildfly quickstart on Kubernetes/Openshift with Fabric8.

This quickstart exposes a simple application deployed on top of Wildfly 9.0.1.Final.

There is a simple REST service. This service can be called from the home page of the application.

The example is based on Markus Eisele blog post:

http://blog.eisele.net/2015/07/running-wildfly-on-openshift-3-with-kubernetes-fabric8-on-windows.html

# Installation

You'll need to have Kubernetes and openshift v3 installed and running on your machine.

You can use the Fabric8-installer: https://github.com/fabric8io/fabric8-installer

In particular this vagrant image: https://github.com/fabric8io/fabric8-installer/tree/master/vagrant/openshift

```
mvn clean package docker:build
mvn fabric8:json fabric8:apply

```

You'll need to modify you `/etc/hosts` file by adding this address to `172.28.128.4` 

```
172.28.128.4 quickstart-wildfly.vagrant.f8

```

Now you should see the homepage on http://quickstart-wildfly.vagrant.f8/. If you follow the "get greetings" link you should get the following message:

```
Hi fabric8 you're receiving this message from pod <Pod_name>

```

## Calling the REST service from a shell script

You can also call the REST service from a shell script. We have provided a script named `test-script.sh` (no script for windows)
which will call the service once every five second for one hundred times. 

You may need to add execution permission to the script before you can execute it

    chmod +x test-script.sh

And then run the script

    ./test-script.sh

While the script runs, you can try to scale up or down the number of pods on the REST service using either the fabric8 web console,
or from the command line using the openshift client

    oc scale --replicas=3 replicationcontrollers quickstart-wildfly
