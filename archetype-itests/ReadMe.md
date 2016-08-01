## Archetypes Integration Test

This module generates every archetype in the generated [archetype-catalog](../archetypes-catalog) as a project in `target/createdProjects` and then creates an uber pom in `target/createdProjects/pom.xml` and then checks that all the projects build.

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

Note make sure that your current shell can connect to both docker and a kubernetes or openshift cluster before trying to run this!

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

