/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.apiman;

import io.apiman.manager.api.micro.ManagerApiMicroServiceConfig;
import io.fabric8.utils.Systems;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Starts the API Manager as a jetty8 micro service.
 */
public class ApimanStarter {

    /**
     * Main entry point for the API Manager micro service.
     * @param args the arguments
     * @throws Exception when any unhandled exception occurs
     */
    public static final void main(String [] args) throws Exception {
        
    	Fabric8ManagerApiMicroService microService = new Fabric8ManagerApiMicroService();
    	setFabric8Props();
        microService.start();
        microService.join();
    }
    
    public static void setFabric8Props() {
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
    	 
        if (System.getProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_HOST) == null) 
        	System.setProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_HOST, host);
        if (System.getProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_PORT) == null) 
        	System.setProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_PORT, hp[1]);
        if (System.getProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_PROTOCOL) == null) 
        	System.setProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_METRICS_ES_PROTOCOL, protocol);
        if (System.getProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_CLUSTER_NAME) == null) 
        	System.setProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_CLUSTER_NAME, "elasticsearch");
        System.out.println("Elastic Connection Properties set to:");
        System.out.print(System.getProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_HOST));
        System.out.print(System.getProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_PORT));
        System.out.print(System.getProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_PROTOCOL));
        System.out.println(System.getProperty(ManagerApiMicroServiceConfig.APIMAN_MANAGER_STORAGE_ES_CLUSTER_NAME));
    }
}
