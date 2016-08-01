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

To just test a single archetype you can specify a system property ``test.archetype`

```sh
mvn clean test -Dtest.archetype=cdi-camel-archetype
```