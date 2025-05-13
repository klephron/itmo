package ru.itmo.zavar.task;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.event.Level;
import ru.itmo.zavar.generatorV2.ComplexRadiatorGenerator;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.radiator.RadiatorMqttPayload;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RequiredArgsConstructor
public final class PublishComplexRandomPayloadTask implements Runnable {

    private final Random random;
    private final int deviceCount;
    private final double skipProbability;
    private final double anomalyProbability;
    private final double strangeDataProbability;
    private final String mqttTopic;
    private final String mqttResetTopic;
    private final MqttClient client;
    private final long[] prevHeat;
    private boolean resetFlag = false;

    private static class DeviceState {
        boolean offline;
        int messagesToSkip;
    }

    private final Map<Long, DeviceState> deviceStates = new HashMap<>();

    @Override
    public void run() {
        ComplexRadiatorGenerator generator = new ComplexRadiatorGenerator(
                random,
                strangeDataProbability,
                anomalyProbability
        );

        final Gson gson = new Gson();

        for (long deviceId = 1; deviceId <= deviceCount; deviceId++) {
            deviceStates.putIfAbsent(deviceId, new DeviceState());
            DeviceState state = deviceStates.get(deviceId);
            if (state.offline && state.messagesToSkip > 0) {
                state.messagesToSkip--;
                if (state.messagesToSkip == 0) {
                    state.offline = false;
                    LoggerWrapper.log(Level.INFO, "Device {} is online again at {}", deviceId, Instant.now());
                }
                continue;
            } else {
                if (random.nextDouble() < 0.01) {
                    state.messagesToSkip = 6 + random.nextInt(10);
                    state.offline = true;
                    LoggerWrapper.log(Level.INFO, "Device {} is offline for {} messages at {}", deviceId, state.messagesToSkip, Instant.now());
                    continue;
                }
            }

            if (random.nextDouble() < skipProbability) {
                continue;
            }

            if (resetFlag) {
                Arrays.fill(prevHeat, 0);
                resetFlag = false;
            }
            RadiatorMqttPayload payload = generator.createPayload(deviceId, prevHeat[(int) (deviceId - 1)]);
            prevHeat[(int) (deviceId - 1)] = payload.heat();

            try {
                MqttMessage message = new MqttMessage(gson.toJson(payload).getBytes());
                message.setQos(2);
                client.publish(mqttTopic, message);
            } catch (MqttException e) {
                LoggerWrapper.log(Level.ERROR, "Error while publishing message: {}", e.getMessage());
            }
        }
    }

    public void reset() {
        this.resetFlag = true;
    }
}
