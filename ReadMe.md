## Fabric8 Archetypes

[![Join the chat at https://gitter.im/fabric8io/ipaas-quickstarts](https://badges.gitter.im/fabric8io/ipaas-quickstarts.svg)](https://gitter.im/fabric8io/ipaas-quickstarts?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This project contains the following [fabric8](http://fabric8.io/) parts: 

* [Archetypes](archetypes) for creating new Java project using Maven Archetypes which are based on the [Quickstarts](quickstart)
* [System Tests](archetype-itests) the [Fabric8 Arquillian](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian) based system tests for the Archetypes in the [Archetype Catalog](archetypes-catalog) 

### Quickstarts

The base quickstarts upon which these archetypes are derived now live in a new github.com repo: [github.com/fabric8-quickstarts](https://github.com/fabric8-quickstarts). Browse around there to see which quickstarts are interesting. Then you can create a quickstart with the archetypes like this:


> mvn archetype:generate -DarchetypeGroupId=io.fabric8.archetypes -DarchetypeArtifactId=%archetype% -DarchetypeVersion=%latest%

Where %archetype% is `jvm-implementation-archetype`.

For example, `spring-boot-camel-archetype` is the name of a Camel quickstart that uses Spring Boot. `karaf-camel-log-archetype` is a Karaf JVM with camel running in it with a timer that logs to developer logs, etc. See the [fabric8 documentation](http://fabric8.io/guide/quickstarts/archetypes.html) for more information.

### Building

The build requires Maven version 3.2.5 or later.

    mvn clean install
    
### Running the system tests

To run the system tests for all the quickstarts check out the [docs on how to run them](archetype-itests)
