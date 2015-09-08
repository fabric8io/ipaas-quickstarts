package io.fabric8.apiman.gateway;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.apiman.gateway.platforms.war.micro.GatewayMicroService;
import io.apiman.gateway.platforms.war.micro.GatewayMicroServicePlatform;
import io.fabric8.utils.Systems;

public class Fabric8GatewayMicroService extends GatewayMicroService {

	@Override
	protected void configureGlobalVars() {
    	
//        System.setProperty("apiman.es.protocol", "http");
//        System.setProperty("apiman.es.host", "localhost");
//        System.setProperty("apiman.es.port", "9200");
//        System.setProperty("apiman.es.cluster-name", "elasticsearch");
        
    	String host = null;
		try {
			InetAddress initAddress = InetAddress.getByName("ELASTICSEARCH");
			host = initAddress.getCanonicalHostName();
		} catch (UnknownHostException e) {
		    System.out.println("Could not resolve DNS for ELASTICSEARCH, trying ENV settings next.");
		}
    	String hostAndPort = Systems.getServiceHostAndPort("ELASTICSEARCH", "localhost", "9200");
    	String[] hp = hostAndPort.split(":");
    	if (host == null) {
    	    System.out.println("ELASTICSEARCH host:port is set to " + hostAndPort + " using ENV settings.");
    		host = hp[0];
    	}
    	String protocol = Systems.getEnvVarOrSystemProperty("ELASTICSEARCH_PROTOCOL", "http");
    	 
        if (System.getProperty("apiman.es.protocol") == null)
        	System.setProperty("apiman.es.protocol", protocol);
        if (System.getProperty("apiman.es.host") == null)
        	System.setProperty("apiman.es.host", host);
        if (System.getProperty("apiman.es.port") == null)
        	System.setProperty("apiman.es.port", hp[1]);
        if (System.getProperty("apiman.es.cluster-name") == null)
        	System.setProperty("apiman.es.cluster-name", "elasticsearch");
        
        if (System.getProperty(GatewayMicroServicePlatform.APIMAN_GATEWAY_ENDPOINT) == null) {
        	String kubernetesDomain = System.getProperty("KUBERNETES_DOMAIN", "vagrant.f8");
        	System.setProperty(GatewayMicroServicePlatform.APIMAN_GATEWAY_ENDPOINT, "http://apiman-gateway." + kubernetesDomain + "/gateway/");
        }
     
	}

}
