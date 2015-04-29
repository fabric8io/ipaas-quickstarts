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
		assertEquals(9300, config.getESPort());
	}

}
