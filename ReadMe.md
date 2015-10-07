## Fabric8 Quickstarts

This project contains the following [fabric8](http://fabric8.io/) parts: 

* [Archetypes](archetypes) for creating new Java project using Maven Archetypes which are based on the [Quickstarts](quickstarts)
* [Quickstarts](quickstarts) for developing Java apps on the [iPaaS](http://fabric8.io/guide/ipaas.html) 

### Building

The build requires Maven version 3.2.5 or later.

    mvn clean install
    
#### Docker and/or Jube profiles 

This build is designed so that it can be used with docker images. Docker only runs on certain platforms so we've disabled the docker build by default less enabled via a maven profile.

* **docker-build** builds docker images locally
* **docker-push** pushes docker images (i.e. when releasing to the public docker registry or a local registry of **$DOCKER_REGISTRY** is defined to point to a local docker registry

#### Building docker images

As described above you need to use the **docker-build** profile or to push the images (when doing a release) you use **docker-push**.

    mvn clean install -Pdocker-build

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

For more details [see the docker maven plugin docs](https://github.com/rhuss/docker-maven-plugin/blob/master/doc/manual.md#authentication)
