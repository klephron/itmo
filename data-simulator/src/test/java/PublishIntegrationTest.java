import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;
import ru.itmo.zavar.task.PublishComplexRandomPayloadTask;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class PublishIntegrationTest {

    private GenericContainer<?> mosquitto;
    private MqttClient subscriber;
    private ArrayBlockingQueue<String> receivedMessages;

    @BeforeEach
    void setup() throws Exception {
        mosquitto = new GenericContainer<>("eclipse-mosquitto:2.0.18")
                .withExposedPorts(1883)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("mosquitto.conf"),
                        "/mosquitto/config/mosquitto.conf"
                );
        mosquitto.start();

        String brokerUrl = "tcp://" + mosquitto.getHost() + ":" + mosquitto.getMappedPort(1883);
        String topic = "test/topic";

        receivedMessages = new ArrayBlockingQueue<>(1);

        subscriber = new MqttClient(brokerUrl, "test-subscriber", new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        subscriber.connect(options);

        subscriber.setCallback(new MqttCallback() {
            @Override
            public void disconnected(MqttDisconnectResponse disconnectResponse) {}

            @Override
            public void mqttErrorOccurred(org.eclipse.paho.mqttv5.common.MqttException exception) {}

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                receivedMessages.offer(new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(org.eclipse.paho.mqttv5.client.IMqttToken token) {}

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {}

            @Override
            public void authPacketArrived(int reasonCode, org.eclipse.paho.mqttv5.common.packet.MqttProperties properties) {}
        });

        subscriber.subscribe("test/topic", 2);

        MqttClient publisherClient = new MqttClient(brokerUrl, "test-publisher", new MemoryPersistence());
        publisherClient.connect(options);

        long[] prevHeat = new long[1];
        PublishComplexRandomPayloadTask task = new PublishComplexRandomPayloadTask(
                new Random(42),
                1,
                0.0,
                0.0,
                0.0,
                topic,
                "test/reset",
                publisherClient,
                prevHeat
        );

        task.run();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (subscriber != null && subscriber.isConnected()) {
            subscriber.disconnect();
            subscriber.close();
            mosquitto.stop();
        }
    }

    @Test
    void testMessagePublishedAndReceived() throws Exception {
        String message = receivedMessages.poll(5, TimeUnit.SECONDS);
        assertNotNull(message, "Message was not received in time!");
        assertTrue(message.contains("\"deviceId\":1"), "Payload should contain deviceId=1");
    }
}
