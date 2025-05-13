package ru.itmo.zavar.account.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.account.mqtt.MqttOutboundConfig;

@Slf4j(topic = "AS-MqttService")
@AllArgsConstructor
@Service
public class MqttService {
    private final MqttOutboundConfig.MqttGateway mqttGateway;

    public void publish(String message) {
        mqttGateway.sendToMqtt(message);
        log.info("MQTT message published: {}", message);
    }
}
