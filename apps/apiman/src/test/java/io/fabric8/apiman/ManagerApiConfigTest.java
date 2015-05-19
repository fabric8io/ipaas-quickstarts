package io.fabric8.apiman;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.apiman.manager.api.core.logging.IApimanLogger;
import io.apiman.manager.api.core.logging.JsonLoggerImpl;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class ManagerApiConfigTest {

    @Spy
    private IApimanLogger log = new JsonLoggerImpl().createLogger(ManagerApiConfigTest.class);

    @InjectMocks
    private ManagerApiMicroServiceConfig config;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
        config.postConstruct();
    }

	@Test
	public void testClustername() {
		assertEquals("elasticsearch", config.getESClusterName());
	}

	@Test
	public void testPort() {
		int esPort = config.getESPort();
		assertTrue("Should have an ES port", esPort > 0);
	}

}
