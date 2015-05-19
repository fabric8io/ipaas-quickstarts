Apiman is an ApiManager. This application deploy the Apiman REST API. Apiman requires the 
Elasticsearch service to be up and running. Once the REST API is up and running the hawtio
console activates the Apiman console plugin tab. 

The Fabric8 gateway uses the Apiman engine to enforce policies and plans that are published to the gateway using the Apiman console.

More details on the <a href="http://www.apiman.io/" target="wikipedia">Apiman website</a>.

Once apiman is running you can hit the following urls, using the service-ip from the services tab of the hawtio console:

http://{service-ip}:8998/apiman/system/status

and to make sure it can connect to Elastic ask for the current user information

http://{service-ip}:8998/apiman/currentuser/

and you can tail the logs using

    docker logs -f <apiman-container-id>
    
the output should show something like this:

    17:52:05,282  INFO Connecting to service ELASTICSEARCH on 172.30.17.51:9200 from $ELASTICSEARCH_SERVICE_HOST and $ELASTICSEARCH_SERVICE_PORT
    *** Connecting to Elastic at service http://elasticsearch.default.local:9200

For a full overview of the apiman API see: 

http://www.apiman.io/latest/api-manager-restdocs.html

