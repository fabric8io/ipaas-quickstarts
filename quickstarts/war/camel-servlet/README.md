# Camel Servlet QuickStart

This example demonstrates how you can use Servlet to expose a http
service in a Camel route, and run that in a servlet container such as
Apache Tomcat. 

The Camel route is illustrated in the figure below. The
`servlet:hello` endpoint is listening for HTTP requests, and being
routed using the Content Based Router.  

![Camel Servlet diagram](images/camel-servlet-diagram.jpg)

The request is being routed whether or not there is a HTTP query
parameter with the name `name`. This is best illustrated as in the
figure below, where we are running this quickstart. The first attempt
there is no `name` parameter, and Camel returns a message that
explains to the user, to add the parameter. In the send attempt we
provide `?name=fabric` in the HTTP url, and Camel responses with a
greeting message. 

![Camel Servlet try](images/camel-servlet-try-quickstart.jpg)


# Building this example

Building and running this quickstart consists of three steps:

## Building

First you compile and package the WAR as usual with 

```bash
 # Change in this quickstart's directory if not already
 cd quickstarts/war/camel-servlet
        
 # Build WAR
 mvn clean install
```

This copies the WAR into you local Maven repository
(`~/.m2/repository`). This will not only build the war file, but also
already creates the JSON descriptor for a Kubernetes deployment. 
   
### Create and push Docker image

Next you need to create a Docker image. This is done with help of the
[docker-maven-plugin](https://github.com/rhuss/docker-maven-plugin/blob/master/doc/manual.md). For
this to work, the environment variable `DOCKER\_HOST` must be set or,
alternatively, given on the command line with the option `docker.host`
(e.g. `-Ddocker.host=tcp://docker.host:2375`)

Now you can

```bash
 # Package and build the container. Note that 'package' is required
 # here
 mvn package docker:build
```
     
After the image has been build it needs to be pushed to a Docker
registry from where Kubernetes can pick it up when it builds up its
pods. This registry must be provided on the commandline, and if
required, a username and password must be provided, too. You can put
the credentials into `~/.m2/settings.xml`, too. See the section on
[Authentication](https://github.com/rhuss/docker-maven-plugin/blob/master/doc/manual.md#authentication)
in the docker-maven-plugin manual for details. 

```bash
 # Push to the registry
 mvn docker:push \
     -Ddocker.registry=172.30.13.165:5000 \
     -Ddocker.username=admin \
     -Ddocker.password=admin
```

### Create and apply Kubernetes configuration

The final step is to apply and send the Kubernetes descriptor to the
Kubernetes installation. You will need to set the variable
`KUBERNETES\_MASTER` to the URL how to reach Kubernetes
(e.g. `https://172.28.128.4:8443`). You can also select the Kubernetes
namespace with the environment variable `KUBERNETESi\_NAMESPACE` (Note
for OpenShift V3 users: You should use the namespace `default` for the
moment, but this will be fixed in a future release of the
fabric8-maven-plugin)

You can set this variables also with system properties, more
information can be found in the documentation to
[fabric8:apply](http://fabric8.io/guide/mavenFabric8Apply.html). 

```bash
 # Apply generated Kubernetes JSON to the Kubernetes Master, which
 # then in turn will create a pod and pulls our Docker images which
 # has just been pushed from the registry.
 mvn fabric8:apply
```

This will generate a POD with associated replication controller and a
service. 

If you are using OpenShift, you can verify this by calling

```bash
 # Get all pods
 osc get pods
 
 # ... and services
 osc get services
 
 # ... and replication controllers
 osc get rc
```

Finally, you can setup routes within OpenShift so that you can use a
fixed address to reach the application:

```bash
 # Creates routes for all services which doesn't have one yet
 mvn fabric8:create-routes
```

Now you can access the example with the URL
`https://quickstart-camelservlet.vagrant.local` (assuming that your
Kubernetes domain is `vagrant.local` and this host can be resolved to
the IP of the Kubernetes master).

### How to try this example

To use the application be sure to have deployed the quickstart in
fabric8 as described above. You probably have to wait a bit until the
pods are in state `running`. If you are on OpenShift, you can use `osc
get pods` to check the status of all pods.

If you set up the OpenShift routes, you can simply go to
`http://quickstart-camelservlet.vagrant.local`. Otherwise, you should
check for the service IP (on OpenShift with `osc get services`) and
use this. 

Follow the instruction from here to test this super-simple example.
