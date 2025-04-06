package ru.itmo.zavar;

import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.task.PublishComplexRandomPayloadTask;
import ru.itmo.zavar.task.PublishRandomPayloadTask;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DataSimulatorMqtt {
    private static int deviceCount = 1;
    private static long sendIntervalMs = 1000;
    private static double skipProbability = 0.1;
    private static double anomalyProbability = 0.1;
    private static double strangeDataProbability = 0.1;

    private static String mqttBrokerUrl = "tcp://localhost:1883";
    private static String mqttTopic = "radiator/data";

    private final static Random random = new Random();

    public static void main(String[] args) throws MqttException {

        if (args.length >= 1) {
            deviceCount = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            sendIntervalMs = Long.parseLong(args[1]);
        }
        if (args.length >= 3) {
            mqttBrokerUrl = args[2];
        }
        if (args.length >= 4) {
            mqttTopic = args[3];
        }

        LoggerWrapper.log(Level.INFO,"Starting Data Simulator (MQTT 5)");
        LoggerWrapper.log(Level.INFO,"DEVICE_COUNT     = {}", deviceCount);
        LoggerWrapper.log(Level.INFO,"SEND_INTERVAL_MS = {}", sendIntervalMs);
        LoggerWrapper.log(Level.INFO,"MQTT_BROKER_URL  = {}", mqttBrokerUrl);
        LoggerWrapper.log(Level.INFO,"MQTT_TOPIC       = {}", mqttTopic);

        String clientId = "DataSimulator";
        MqttClient client = new MqttClient(mqttBrokerUrl, clientId, new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(false);
        options.setSessionExpiryInterval(3600L);
        options.setAutomaticReconnect(true);

        client.connect(options);
        LoggerWrapper.log(Level.INFO,"Connected to MQTT broker: {}", mqttBrokerUrl);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(new PublishComplexRandomPayloadTask(random, deviceCount, skipProbability,
               anomalyProbability, strangeDataProbability, mqttTopic, client),0, sendIntervalMs, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
                LoggerWrapper.log(Level.INFO,"MQTT Client disconnected and closed.");
            } catch (MqttException e) {
                LoggerWrapper.log(Level.ERROR,"Error while closing MQTT client: {}", e.getMessage());
            }
        }, "Shutdown hook"));

    }
}
