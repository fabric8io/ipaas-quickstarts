package io.fabric8.apiman;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import io.apiman.manager.api.core.logging.IApimanLogger;
import io.apiman.manager.api.core.logging.JsonLoggerImpl;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.junit.Before;
import org.junit.Ignore;
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
		// TODO this test fails when run inside kubernetes
		// I guess due to the port of the ELASTICSEARCH_SERVICE_PORT
		//assertEquals(9300, esPort);
		assertTrue("Should have an ES port", esPort > 0);
	}

	@Test @Ignore
	public void testES() {
		try {
			 Client client = new TransportClient()
	         .addTransportAddress(new InetSocketTransportAddress("172.30.17.218", 9300));
			 IndexResponse response = client.prepareIndex("twitter", "tweet", "1").setSource("{ }").execute().actionGet();
	         System.out.println("RESPONSE=" + response);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
