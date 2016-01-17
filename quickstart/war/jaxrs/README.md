# Swagger generated server

This project is an example of the PetStore APIs being generated for the 
Server JAX-RS framework and shown running on fabric8 using the swagger-codegen
project: http://swagger.io/swagger-codegen/

The core source code was created from the swagger-gen project's main directory
using the command:

java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate \
  -i http://petstore.swagger.io/v2/swagger.json \
  -l jaxrs \
  -o samples/server/petstore/jaxrs

This project has taken the swagger-gen output and modified it to run under fabric8 simply by changing the pom.xml file

The command:

mvn -Pf8-local-deploy

will deploy the application: swagger-jaxrs-server to the kubernetes instance 
defined by your  DOCKER_HOST variable e.g.
 export DOCKER_HOST=tcp://172.28.128.4:2375

As always you first need to logon to your local kubernetes / opeshift instance
e.g. oc login

Once the application has been deployed it should be accessible at:

http://swagger-jaxrs-server-default.vagrant.f8/tomcat-fabric8/v2/swagger.json

You can then try it out using the Swagger UI
