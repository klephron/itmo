package ru.itmo.zavar.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.account.mqtt.MqttOutboundConfig;
import ru.itmo.zavar.account.service.MqttService;

import static org.mockito.Mockito.*;

@Tag("unit")
public class MqttServiceTest {

    private MqttOutboundConfig.MqttGateway mqttGateway;
    private MqttService mqttService;

    @BeforeEach
    void setUp() {
        mqttGateway = mock(MqttOutboundConfig.MqttGateway.class);
        mqttService = new MqttService(mqttGateway);
    }

    @Test
    void testPublish() {
        String message = "Hello MQTT!";
        mqttService.publish(message);

        verify(mqttGateway).sendToMqtt(message);
    }
}
