## Fabric8 Quickstarts

This project contains the following [fabric8](http://fabric8.io/) parts:  

* [Apps](apps) the individual apps for fabric8. Each one creates a single kuberntes app.
* [App Groups](app-groups) the app bundles for the [Fabric8 Apps](http://fabric8.io/guide/fabric8Apps.html)
* [Quickstarts](quickstarts) for developing Java apps on the [iPaaS](http://fabric8.io/guide/ipaas.html) 

### Building

The build requires Maven version 3.2.5 or later.

#### apps and quickstarts profiles

If you wish to only build the apps or the quickstarts you can use profiles

    mvn install -Papps
    mvn install -Pquickstarts
    
If you omit the profiles then it builds everything.    

#### Docker and/or Jube profiles 

This build is designed so that it can be used with docker images and/or jube images. So by default jube images are created unless you specify other profiles. Docker only runs on certain platforms so we've disabled the docker build by default less enabled via a maven profile.

The following maven profiles can be used to enable/disable parts of the build. Note that **jube** is enabled by default until you specify other profiles:

* **docker-build** builds docker images locally
* **docker-push** pushes docker images (i.e. when releasing to the public docker registry or a local registry of **$DOCKER_REGISTRY** is defined to point to a local docker registry
* **jube** creates a jube image zip as part of the build (which is active by default)
* **ts.kube** enables the kubernetes integration tests

#### Building docker images

As described above you need to use the **docker-build** profile or to push the images (when doing a release) you use **docker-push**.

    mvn install -Pdocker-build

### Pushing docker images

If you wish to push docker images to a private or public registry you will need to add a section to your **~/.m2/settings.xml** file with a login/pwd:

```
	<servers>
       <server>
           <id>192.168.59.103:5000</id>
           <username>jolokia</username>
           <password>jolokia</password>
       </server>
        ...
  </servers>
```

#### Building and running the integration tests

    mvn install -Pdocker-push -Pts.kube -Ddocker.username=jolokia -Ddocker.password=jolokia

For local containers they can be dummy login/passwords. For more details [see the docker maven plugin docs](https://github.com/rhuss/docker-maven-plugin/blob/master/doc/manual.md#authentication)
