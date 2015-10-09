# Camel CDI Jetty Server QuickStart

This example shows how to work with Camel in the Java Container using CDI to configure components,
endpoints and beans.

This quickstart is the server side which embeds a Jetty HTTP server in a Camel route that
exposes a HTTP service.

The `quickstart-cdi-camel-http` is the client to this quickstart that can be started which will call this
Jetty HTTP server every 5 second.

## Calling the HTTP service from a shell script

You can also call the remote HTTP service from a shell script. We have provided a script named `src/test/resources/hitme-f8.sh` (no script for windows)
in the source code for the quickstart, not in the docker image, which will call the service once per second.

You may need to add execution permission to the script before you can execute it

    chmod +x src/test/resources/hitme-f8.sh

And then run the script

    .src/test/resources/hitme-f8.sh

While the script runs, you can try to scale up or down the number of pods on the Jetty HTTP service using either the fabric8 web console,
or from the command line using the openshift client

    oc scale --replicas=3 replicationcontrollers quickstart-cdi-camel-jetty-server


