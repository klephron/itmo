package ru.itmo.zavar.accountservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.accountservice.mqtt.MqttOutboundConfig;

import static org.mockito.Mockito.*;

class MqttServiceTest {

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
