package ru.itmo.zavar;

import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.task.PublishComplexRandomPayloadTask;
import ru.itmo.zavar.task.PublishRandomPayloadTask;

import java.io.*;
import java.util.Arrays;
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
    private static String mqttResetTopic = "radiator/reset";

    private final static Random random = new Random();

    public static void main(String[] args) throws MqttException {

        if (System.getenv("DEVICE_COUNT") != null) {
            deviceCount = Integer.parseInt(System.getenv("DEVICE_COUNT"));
        }
        if (System.getenv("SEND_INTERVAL_MS") != null) {
            sendIntervalMs = Long.parseLong(System.getenv("SEND_INTERVAL_MS"));
        }
        if (System.getenv("MQTT_BROKER_URL") != null) {
            mqttBrokerUrl = System.getenv("MQTT_BROKER_URL");
        }
        if (System.getenv("MQTT_TOPIC") != null) {
            mqttTopic = System.getenv("MQTT_TOPIC");
        }
        if (System.getenv("MQTT_RESET_TOPIC") != null) {
            mqttResetTopic = System.getenv("MQTT_RESET_TOPIC");
        }

        LoggerWrapper.log(Level.INFO,"Starting Data Simulator (MQTT 5)");
        LoggerWrapper.log(Level.INFO,"DEVICE_COUNT     = {}", deviceCount);
        LoggerWrapper.log(Level.INFO,"SEND_INTERVAL_MS = {}", sendIntervalMs);
        LoggerWrapper.log(Level.INFO,"MQTT_BROKER_URL  = {}", mqttBrokerUrl);
        LoggerWrapper.log(Level.INFO,"MQTT_TOPIC       = {}", mqttTopic);
        LoggerWrapper.log(Level.INFO,"MQTT_RESET_TOPIC = {}", mqttResetTopic);

        String clientId = "DataSimulator";
        MqttClient client = new MqttClient(mqttBrokerUrl, clientId, new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(false);
        options.setSessionExpiryInterval(3600L);
        options.setAutomaticReconnect(true);

        client.connect(options);
        LoggerWrapper.log(Level.INFO,"Connected to MQTT broker: {}", mqttBrokerUrl);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


        long[] prevHeat;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("prevHeat.bin"))) {
            prevHeat = (long[]) in.readObject();
            LoggerWrapper.log(Level.INFO,"Got prev heat values: {}", Arrays.toString(prevHeat));
        } catch (IOException | ClassNotFoundException e) {
            prevHeat = new long[deviceCount];
            LoggerWrapper.log(Level.ERROR, "Error while reading array: {}", e.getMessage());
        }
        client.subscribe(mqttResetTopic, 2);
        PublishComplexRandomPayloadTask publishComplexRandomPayloadTask = new PublishComplexRandomPayloadTask(random, deviceCount, skipProbability,
                anomalyProbability, strangeDataProbability, mqttTopic, mqttResetTopic, client, prevHeat);

        client.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse mqttDisconnectResponse) {

            }

            @Override
            public void mqttErrorOccurred(MqttException e) {

            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                String stringPayload = new String(mqttMessage.getPayload());
                if (stringPayload.equals("reset")) {
                    publishComplexRandomPayloadTask.reset();
                }
            }

            @Override
            public void deliveryComplete(IMqttToken iMqttToken) {

            }

            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void authPacketArrived(int i, MqttProperties mqttProperties) {

            }
        });

        scheduler.scheduleAtFixedRate(publishComplexRandomPayloadTask,0, sendIntervalMs, TimeUnit.MILLISECONDS);

        long[] finalPrevHeat = prevHeat;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();

            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("prevHeat.bin"))) {
                out.writeObject(finalPrevHeat);
            } catch (IOException e) {
                LoggerWrapper.log(Level.ERROR, "Error while writing array: {}", e.getMessage());
            }

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
