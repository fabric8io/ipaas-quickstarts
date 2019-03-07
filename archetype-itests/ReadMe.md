## Archetypes Integration Test

This module generates every archetype in the generated [archetype-catalog](../archetypes-catalog) as a project in `target/createdProjects` and then creates an uber pom in `target/createdProjects/pom.xml` and then checks that all the projects build and pass their system tests via  [Fabric8 Arquillian](https://github.com/fabric8io/fabric8/tree/master/components/fabric8-arquillian) 
.

### Prerequisites

In order for the tests to run, the Maven home directory must be known. Usually this is done by 
exporting the Maven home directory via the M2_HOME variable. Before running the tests, please 
ensure that the directory is exported via that variable or passed through using the maven.home
property. 

### Testing all the archetypes build OK

Run the following

```sh
mvn clean test
```

### Running Arquillian tests for each project

To enable the arquillian tests on each project type the following:

```sh
mvn clean test -Dtest.arq=true
```

NOTE: make sure that your current shell can connect to both docker and a kubernetes or openshift cluster before trying to run this!
If you use [minikube](http://fabric8.io/guide/getStarted/minikube.html) or [minishift](http://fabric8.io/guide/getStarted/minishift.html) you will need to run one of these lines in your current shell:

```sh
eval $(minikube docker-env)

eval $(minishift docker-env)
```

NOTE that [OpenShift is currently only supported in `kubernetes` mode for now until we get Arquillian working with S2I binary builds and ImageStreams](https://github.com/fabric8io/ipaas-quickstarts/issues/1369)! When running on OpenShift we [currently default to `kubernetes` mode](https://github.com/fabric8io/fabric8-maven-plugin/issues/240). 

If you want to try things on OpenShift in `openshift` mode (using S2I binary builds and ImageStreams) then run the tests as follows:

```sh
mvn clean test -Dtest.arq=true -Dfabric8.mode=openshift
```

#### Viewing the output
 
Running all the tests can generate quite a bit of output :) 
 
To browse or grep the test output see: `target/test.log`

### Testing a single archetype

Once you have built the generated projects you can just `cd` into the generated project folder and run things there directly

```sh
cd target/createdProjects/spring-boot-camel-archetype-output
mvn clean install  -Dfabric8.mode=openshift -Dfabric8.build.recreate=all
mvn failsafe:integration-test failsafe:verify
 ```

Or to just generate a signle single archetype you can specify a system property `test.archetype` in this folder when you run a build:

```sh
mvn clean test -Dtest.archetype=cdi-camel-archetype
```

### Keeping track of failing archetypes

When an archetype fails its system test we should [remove it from the catalog by excluding it from the archetypes/pom.xml file](https://github.com/fabric8io/ipaas-quickstarts/blob/master/archetypes/pom.xml#L36) and [create an issue for it](https://github.com/fabric8io/ipaas-quickstarts/issues/new).

You can view all the current [failing system test issues](https://github.com/fabric8io/ipaas-quickstarts/issues?q=is%3Aissue+is%3Aopen+label%3A%22system+test%22) 

