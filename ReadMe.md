## Fabric8 Quickstarts

[![Join the chat at https://gitter.im/fabric8io/ipaas-quickstarts](https://badges.gitter.im/fabric8io/ipaas-quickstarts.svg)](https://gitter.im/fabric8io/ipaas-quickstarts?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This project contains the following [fabric8](http://fabric8.io/) parts: 

* [Archetypes](archetypes) for creating new Java project using Maven Archetypes which are based on the [Quickstarts](quickstart)
* [Quickstarts](quickstart) for developing Java apps on the [iPaaS](http://fabric8.io/guide/ipaas.html) 
* [System Tests](archetype-itests) the [Fabric8 Arquillian](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian) based system tests for the Archetypes in the [Archetype Catalog](archetypes-catalog) 

### Building

The build requires Maven version 3.2.5 or later.

    mvn clean install
    
### Running the system tests

To run the system tests for all the quickstarts check out the [docs on how to run them](archetype-itests)
