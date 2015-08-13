# Camel CDI HTTP QuickStart

This example shows how to work with Camel in the Java Container using CDI to configure components,
endpoints and beans.

This quickstart is the client side which embeds a Camel route that triggers every 5th second,
and calls a remote HTTP service and logs the response.

The remote HTTP service is the camel-servlet-quickstart which must be up and running.

## Calling the remote service from a shell script

You can also call the remote HTTP service from a shell script. We have provided a script named `hitme-f8.sh` (no script for windows)
which will call the service once per second. 

You may need to add execution permission to the script before you can execute it

    chmod +x hitme-f8.sh

And then run the script

    ./hitme-f8

While the script runs, you can try to scale up or down the number of pods on the remote HTTP service using either the fabric8 web console,
or from the command line using the openshift client

    oc scale --replicas=3 replicationcontrollers camel-servlet-quickstart

