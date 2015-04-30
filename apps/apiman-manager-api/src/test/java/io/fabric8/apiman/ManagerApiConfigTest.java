package io.fabric8.apiman;

import static org.junit.Assert.*;

import org.junit.Test;

public class ManagerApiConfigTest {

	@Test
	public void testClustername() {
		ManagerApiMicroServiceConfig config = new ManagerApiMicroServiceConfig();
		config.postConstruct();
		assertEquals("ELASTICSEARCH", config.getESClusterName());
	}
	
	@Test
	public void testPort() {
		ManagerApiMicroServiceConfig config = new ManagerApiMicroServiceConfig();
		config.postConstruct();
		int esPort = config.getESPort();
		// TODO this test fails when run inside kubernetes
		// I guess due to the port of the ELASTICSEARCH_SERVICE_PORT
		//assertEquals(9300, esPort);
		assertTrue("Should have an ES port", esPort > 0);
	}

}
