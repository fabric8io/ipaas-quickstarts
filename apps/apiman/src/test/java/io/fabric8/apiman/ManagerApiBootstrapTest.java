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

public class ManagerApiBootstrapTest {

   

	@Test
	public void testLoadPolicies() {
		ManagerApiMicroServiceBootstrap bootstrap = new ManagerApiMicroServiceBootstrap();
		bootstrap.loadDefaultPolicies();
	}

}
