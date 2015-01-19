package io.fabric8.mq;

import org.junit.Assert;
import org.junit.Test;

public class MqttConnectorTest extends Assert {

    @Test
    public void shouldReturnNoPort() {
        System.clearProperty("org.apache.activemq.AMQ_MQTT_PORT");
        assertNull(Main.mqttPortString());
    }

    @Test
    public void shouldReturnPortFromSystemProperty() {
        String port = 1234 + "";
        System.setProperty("org.apache.activemq.AMQ_MQTT_PORT", port);
        assertEquals(port, Main.mqttPortString());
    }

}
