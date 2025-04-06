package ru.itmo.zavar.task;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.radiator.RadiatorMqttPayload;

import java.util.Random;

@RequiredArgsConstructor
public final class PublishRandomPayloadTask implements Runnable {
    private final Random random;
    private final double deviceCount;
    private final double skipProbability;
    private final double anomalyProbability;
    private final String mqttTopic;
    private final MqttClient client;

    @Override
    public void run() {
        try {
            final Gson gson = new Gson();
            for (int deviceIndex = 1; deviceIndex <= deviceCount; deviceIndex++) {
                if (random.nextDouble() < skipProbability) {
                    continue;
                }
                MqttMessage message = new MqttMessage(gson
                        .toJson(RadiatorMqttPayload.createRandom(deviceIndex, random, anomalyProbability))
                        .getBytes());
                int QOS = 2;
                message.setQos(QOS);
                client.publish(mqttTopic, message);
            }
        } catch (MqttException e) {
            LoggerWrapper.log(Level.ERROR,"Error while publishing message: {}", e.getMessage());
        }
    }
}
