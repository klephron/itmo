package ru.itmo.zavar.accountservice.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.accountservice.mqtt.MqttOutboundConfig;

@AllArgsConstructor
@Service
public class MqttService {
    private final MqttOutboundConfig.MqttGateway mqttGateway;

    public void publish(String message) {
        mqttGateway.sendToMqtt(message);
    }
}
