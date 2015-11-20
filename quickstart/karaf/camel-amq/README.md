# Camel AMQ QuickStart

This quickstart demonstrates how to connect to an ActiveMQ broker and use JMS messaging between two Camel routes.

In this example we will use two containers, one container to run as a ActiveMQ broker, and another as a client to the broker, where the Camel routes is running.

This quickstarts requires the ActiveMQ broker has been deployed and running first. This can be done from the web console from the `Apps` page, and then install the `messaging` application.

To install this quickstart from the command line type

The example can be built and deployed using a single goal:

    mvn -Pf8-local-deploy

This requires you have [setup your local computer](http://fabric8.io/guide/getStarted/develop.html) to work with docker and kubernetes.

### Running the example using OpenShift S2I template

The example can also be built and run using the included S2I template quickstart-template.json.

The application can be run directly by first editing the template file and populating S2I build parameters, including the required parameter GIT_REPO and then executing the command:

    oc new-app -f quickstart-template.json

Alternatively the template file can be used to create an OpenShift application template by executing the command:

    oc create -f quickstart-template.json


### More details

You can find more details about running this [quickstart](http://fabric8.io/guide/quickstarts/running.html) on the website. This also includes instructions how to change the Docker image user and registry.
