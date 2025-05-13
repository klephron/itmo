package ru.itmo.zavar;

import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.client.logging.LoggerFactory;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
    private static String MQTT_BROKER_URL = "tcp://localhost:1883";
    private static String MQTT_TOPIC = "$share/myGroup/radiator/data";
    private static int    QOS_LEVEL        = 2;

    public static void main(String[] args) throws MqttException, InterruptedException {

        Logger.getLogger("org.eclipse.paho.client.mqttv3").setLevel(Level.OFF);
        Logger.getLogger("org.eclipse.paho.mqttv5.client").setLevel(Level.OFF);

        if (args.length >= 1) {
            MQTT_BROKER_URL = args[0];
        }
        if (args.length >= 2) {
            MQTT_TOPIC = args[1];
        }

        System.out.println("=== MQTT Consumer (v5) ===");
        System.out.println("  BROKER_URL = " + MQTT_BROKER_URL);
        System.out.println("  TOPIC      = " + MQTT_TOPIC);

        String clientId = "Consumer-" + System.currentTimeMillis();
        MqttClient client = new MqttClient(MQTT_BROKER_URL, clientId, new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(false);
        options.setSessionExpiryInterval(10L);
        options.setAutomaticReconnect(true);

        client.setCallback(new MqttCallback() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                System.out.println("Connected to " + serverURI + " (reconnect=" + reconnect + ")");
            }

            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {
                System.err.println("Disconnected: " + disconnectResponse.getReasonString());
            }

            @Override
            public void mqttErrorOccurred(MqttException exception) {
                System.err.println("MQTT Error: " + exception.getMessage());
            }

            @Override
            public void messageArrived(String incomingTopic, MqttMessage message) throws Exception {
                System.out.println("=== Message Arrived ===");
                System.out.println("Topic: " + incomingTopic);
                System.out.println("Payload: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttToken token) {
                System.out.println("Delivery completed for token: " + token.getMessageId());
            }

            @Override
            public void authPacketArrived(int reasonCode, MqttProperties properties) {
                // Необязательный метод, если используется расширенная авторизация MQTT 5
            }
        });

        client.connect(options);

        client.subscribe(MQTT_TOPIC, QOS_LEVEL);
        System.out.println("Subscribed to " + MQTT_TOPIC);

        while (true) {
            Thread.sleep(1000);
        }
    }
}
