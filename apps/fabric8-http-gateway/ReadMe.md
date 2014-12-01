## HTTP Gateway

## Gateway

The Gateway provides a HTTP/HTTPS gateway for discovery, load balancing and failover of services running within a Fabric8. This allows simple HTTP URLs to be used to access any web application or web service running within a Fabric.

### Deployment options

There are 2 main deployment strategies

* run the gateway on each machine which needs to discover services; then communicate with it via localhost. You then don't need to hard code any host names in your messaging or web clients and you get nice fast networking on localhost.
* run the gateway on one or more known hosts using DNS or VIP load balancing of host names to machines; then you can use a fixed host name for all your services

### How the Gateway works

The gateway watches Kubernetes for a set of services selected using user defined sets of labels. It then uses the mapping rules to figure out how to expose those services via the HTTP gateway. The Kubernetes registry is automatically populated by fabric8 when you deploy your services. 

### Running the Gateway

From the CLI or Fuse Management Console just run an instance of the **fabric8-http-gateway** on a machine you wish to use as the gateway (e.g. if using Red Hat clustering and VIPs on 2 boxes).

### Configuring the Gateway

The implementation of the HTTP Gateway is a Fabric8 Service, with a Kubernetes/Jube Service proxying one
or more containers.

#### Service Configuration

The service can be configured at deploy time by editing the `kubernetes-extra.json` file.
```
{
  "id": "fabric8-httpgateway-config",
  "kind": "Config",
  "apiVersion": "v1beta1",
  "name": "fabric8httpgateway",
  "description": "Creates a fabric8 HTTP gateway service",
  "items": [
    {
      "id": "fabric8httpgateway",
      "apiVersion": "v1beta1",
      "kind": "Service",
      "containerPort": ${http.port},
      "port": "9000",
      "selector": {
        "container": "java",
        "name": "fabric8Fabric8HttpGateway",
        "group": "defaultGatewayGroup"
      }
    }
  ]
}
```
In particular the `port` setting is of interest as this is the port that the service and therefore the HTTP Gateway will expose to the outside world. When using Jube this setting will appear in the `env.sh` file as 
```
export FABRIC8HTTPGATEWAY_SERVICE_PORT="9000"
```

#### Container Gateway Configuration
The HTTP Gateway can be configured using Environmental or System parameters. If the parameter is specified for both, then the Environmental
value takes precedence. To avoid avoid confusion with other parameters a `namespace` prefix of `<HTTP_GATEWAY_SERVICE_ID>_SERVICE_` is used, where the `<HTTP_GATEWAY_SERVICE_ID>` defaults to `FABRIC8HTTPGATEWAY`. This makes a default prefix of `FABRIC8HTTPGATEWAY_SERVICE_`.
The table below shows a full overview of all parameters that can be used to configure the HTTP Gateway Container.

| Parameter | Description | Default |
| -------- | ----------- | ------ |
| `HTTP_GATEWAY_SERVICE_ID` | Parameter used to construct the `HTTP_GATEWAY` parameters. The complete prefix is defined as `<HTTP_GATEWAY_SERVICE_ID>_SERVICE_` | `FABRIC8HTTPGATEWAY` |
| `API_MANAGER_ENABLED` | Switch to enable the API Manager. When set to `false` no API management is activated and the HTTP_GATEWAY defaults to do simple URL mapping only without any of the API Management features such as security or other policies  | `true` |
| `HOST` | The hostname or IP address of the HTTP Gateway | `localhost` |
| `HTTP_PORT` | The HTTP port of the HTTP Gateway containers. **Note that this parameter has no prefix.** | `9090` |
| `KUBERNETES_MASTER` | The URL pointing to the Kubernates API. By default Kubernetes runs on port `8484`, which Jube runs on `8585` | `http://localhost:8484/` |
| `GATEWAY_SERVICES_SELECTORS` | A JSON structure representing the collection of selectors for the gateway to use to select the services it proxies. | `[{container:java,group:quickstarts},{container:camel,group:quickstarts}]` |

#### HTTP Mapping rules

| Parameter | Description | Default |
| -------- | ----------- | ------ |
| `URI_TEMPLATE` | URI Template to use for this HTTP Gateway instance. See the URI Template section for a full description of all template variables that can be used.  | `/{contextPath}`. 
| `LOAD_BALANCER` | The type of load balancing the gateway should use when connecting to the backend services. This needs to be one `random`, `roundrobin` or `sticky`| `roundrobin` |
| `ENABLED_VERSION` | By default the Gateway supports rolling upgrades, however if want to be completely specific on a version then you can specify that version with this parameter. By default this parameter is not defined. | `null` |
| `ENABLE_INDEX` | If enabled then performing a HTTP GET on the path '/' will return a JSON representation of the gateway mappings. | `true` |
| `REVERSE_HEADERS` | If enabled then the URL in the Location, Content-Location and URI headers from the proxied HTTP responses are rewritten from the back end service URL to match the front end URL on the gateway.This is equivalent to the ProxyPassReverse directive in mod_proxy. | `true` |

##### URL Mapping
When using the HTTP gateway, its common to wish to map different versions of web applications or web services to different URI paths on the gateway. You can perform very flexible mappings using [URI templates](http://en.wikipedia.org/wiki/URL_Template).

The out of the box defaults are to expose all web applications and web services at the context path that they are running in the target server. For example if you use the **example-quickstarts-rest** profile, then that uses a URI like: **/cxf/crm/customerservice/customers/123** on whatever host/port its deployed on; so by default it is visible on the gateway at [http://localhost:9000/cxf/crm/customerservice/customers/123](http://localhost:9000/cxf/crm/customerservice/customers/123)

For this the URI template is:

    /{contextPath}

which means take the context path (in the above case "/cxf/crm" and append "/" to it, making "/cxf/crm/" and then any request within that path is then passed to an instance of the cxf crm service.

You may wish to segregate, say, servlets, web services or web applications into different URI spaces. For example you may want all web services to be within **/api/** and apps to be in **/app/**. To do this just update the URI template to one of the following:

    uriTemplate = /api/{contextPath}/
    uriTemplate = /app/{contextPath}/
    uriTemplate = /rest/{contextPath}/
    uriTemplate = /ws/{contextPath}/

#### Versioning: Explict URIs

You may wish to expose all available versions of each web service and web application at a different URI. e.g. if you change your URI template to:

    /version/{version}/{contextPath}/

Then if you have 1.0 and 1.1 versions of a profile with web services or web apps inside, you can access them using version specific URIs. For example if you are running some version 1.0 and version 1.1 implementations of the **example-quickstarts-rest** profile then you can access either one via these URIs

* version 1.0 via: [http://localhost:9000/version/1.0/cxf/crm/customerservice/customers/123](http://localhost:9000/version/1.0/cxf/crm/customerservice/customers/123)
* version 1.1 via: [http://localhost:9000/version/1.1/cxf/crm/customerservice/customers/123](http://localhost:9000/version/1.1/cxf/crm/customerservice/customers/123)

Then both versions are available to the gateway - provided you include the version information in the URI

#### Versioning: Rolling Upgrades

Another approach to dealing with versions of web services and web applications is to only expose a single version of each web service or web application at a time in a single gateway. This is the default out of the box configuration.

So if you deploy a 1.0 version of the **fabric8-http-gateway** app and run a few services, then you'll see all 1.0 versions of them. Run some 1.1 versions of the services and the gateway won't see them. Then if you do a [rolling upgrade](rollingUpgrade.html) of your gateway to 1.1 it will then switch to only showing the 1.1 versions of the services.

If you want to be completely specific on a version you can specify the exact version on the mapping configuration screen.

#### URI template expressions

The following table outlines the available variables you can use in a URI template expression

| Expression | Description |
|------------|-------------|
|{contextPath} | The context path (the part of the URL after the host and port) of the web service or web application implementation.|
|{version} |  The version of the web service or web application |

#### Viewing all the active HTTP URIs

Once you've run a few web services and web applications and you are runnning the gateway you may be wondering what URIs are available. Assuming you're on a machine with the gateway, just browse the URL [http://localhost:9000/]([http://localhost:9000/) and you should see the JSON of all the URI prefixes and the underlying servers they are bound to.

