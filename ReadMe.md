## Fabric8 Quickstarts

This project contains the following [fabric8](http://fabric8.io/) parts: 

* [Archetypes](archetypes) for creating new Java project using Maven Archetypes which are based on the [Quickstarts](quickstart)
* [Quickstarts](quickstart) for developing Java apps on the [iPaaS](http://fabric8.io/guide/ipaas.html) 

### Building

The build requires Maven version 3.2.5 or later.

    mvn clean install
    
#### Building docker images

The docker images can be build by using the `f8-build` maven profile:

    mvn -Pf8-build

### Pushing docker images 

The docker images can be built and pushed using the following maven command:

    mvn -Pf8-build docker:push

#### Pushing docker images to registry

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

For more details [see the docker maven plugin docs](http://ro14nd.de/docker-maven-plugin/authentication.html)
