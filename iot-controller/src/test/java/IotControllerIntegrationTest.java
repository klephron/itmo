import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.junit.jupiter.api.*;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.testcontainers.utility.MountableFile;
import ru.itmo.zavar.IotController;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.metrics.MetricsServer;
import ru.itmo.zavar.mqtt.IotMqttCallback;
import ru.itmo.zavar.task.MongoDBConnectTask;
import ru.itmo.zavar.task.RabbitMQConnectTask;
import ru.itmo.zavar.task.exithandler.DefaultExitHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IotControllerIntegrationTest {

    @Container
    private static final RabbitMQContainer rabbitMQContainer =
        new RabbitMQContainer("rabbitmq:3.9-management")
            .withExposedPorts(5672, 15672);

    @Container
    private static final MongoDBContainer mongoDBContainer =
        new MongoDBContainer("mongo:6.0.4")
            .withExposedPorts(27017);

    @Container
    private static final GenericContainer<?> mosquittoContainer =
        new GenericContainer<>("eclipse-mosquitto:2.0.15")
            .withExposedPorts(1883)
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("mosquitto.conf"),
                "/mosquitto/config/mosquitto.conf"
            );

    private static final String RABBIT_QUEUE_NAME = "testQueue";
    private static final String MONGO_COLLECTION = "rules";
    private static final String MONGO_DB_NAME = "iot-db";

    static Logger logger;

    static {
        logger = ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME));
        logger.setLevel(ch.qos.logback.classic.Level.convertAnSLF4JLevel(Level.INFO));
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        Thread appThread = new Thread(() -> {
            try {
                String mqttBrokerUrl = "tcp://" + mosquittoContainer.getHost() + ":" + mosquittoContainer.getMappedPort(1883);
                String mqttTopic = "radiator/data";
                String rabbitMqHost = rabbitMQContainer.getHost();
                int rabbitMqPort = rabbitMQContainer.getMappedPort(5672);
                String rabbitMqUser = rabbitMQContainer.getAdminUsername();
                String rabbitMqPass = rabbitMQContainer.getAdminPassword();
                String rabbitQueueName = RABBIT_QUEUE_NAME;
                String mongoDbName = MONGO_DB_NAME;
                String mongoUri = mongoDBContainer.getReplicaSetUrl();
                String mongoDbCollection = MONGO_COLLECTION;
                String replicaId = String.valueOf(System.currentTimeMillis());

                int qosLevel = 2;

                LoggerWrapper.log(Level.INFO, ("=== IotController {}==="), replicaId);
                LoggerWrapper.log(Level.INFO, "BROKER_URL = {}", mqttBrokerUrl);
                LoggerWrapper.log(Level.INFO, "TOPIC      = {}", mqttTopic);

                String clientId = "Consumer-" + replicaId;
                MqttClient client = new MqttClient(mqttBrokerUrl, clientId, new MemoryPersistence());
                MqttConnectionOptions options = new MqttConnectionOptions();
                options.setCleanStart(true);
                options.setAutomaticReconnect(true);

                ConnectionFactory factory = new ConnectionFactory();
                factory.setAutomaticRecoveryEnabled(true);
                factory.setTopologyRecoveryEnabled(true);
                factory.setNetworkRecoveryInterval(5000);
                factory.setHost(rabbitMqHost);
                factory.setUsername(rabbitMqUser);
                factory.setPassword(rabbitMqPass);
                factory.setPort(rabbitMqPort);

                int maxRetries = 10;
                int delayMs = 3000;

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                RabbitMQConnectTask rabbitMQConnectTask = new RabbitMQConnectTask(factory, executor, client, rabbitQueueName,
                    maxRetries, new DefaultExitHandler());
                executor.scheduleWithFixedDelay(rabbitMQConnectTask, 0, delayMs, TimeUnit.MILLISECONDS);
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                Connection connection = rabbitMQConnectTask.getConnection();
                Channel channel = rabbitMQConnectTask.getChannel();

                CodecRegistry pojoCodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build()));
                MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(mongoUri))
                    .codecRegistry(pojoCodecRegistry)
                    .build();

                executor = Executors.newSingleThreadScheduledExecutor();
                MongoDBConnectTask mongoDBConnectTask = new MongoDBConnectTask(settings, executor, maxRetries,
                    new DefaultExitHandler());
                executor.scheduleWithFixedDelay(mongoDBConnectTask, 0, delayMs, TimeUnit.MILLISECONDS);
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

                MongoClient mongoClient = mongoDBConnectTask.getMongoClient();
                MongoDatabase database = mongoClient.getDatabase(mongoDbName);
                try {
                    database.createCollection(mongoDbCollection);
                } catch (Exception ignored) {
                    LoggerWrapper.log(Level.DEBUG, "Collection already exists");
                }

                Runnable subscribeRunnable = () -> {
                    try {
                        client.subscribe(mqttTopic, qosLevel);
                        LoggerWrapper.log(Level.INFO, "Subscribed to " + mqttTopic);
                    } catch (MqttException e) {
                        LoggerWrapper.log(Level.ERROR, "Failed to subscribe to MQTT topic: {}", e.getMessage());
                    }
                };

                client.setCallback(new IotMqttCallback(channel, connection, rabbitQueueName, mongoDBConnectTask, mongoDbName,
                    mongoDbCollection, subscribeRunnable));
                client.connect(options);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        appThread.setDaemon(true);
        appThread.start();

        Thread.sleep(8000);
    }


    @Test
    @Order(1)
    void testContainersRunning() {
        Assertions.assertTrue(rabbitMQContainer.isRunning(), "RabbitMQ container не запущен");
        Assertions.assertTrue(mongoDBContainer.isRunning(), "MongoDB container не запущен");
        Assertions.assertTrue(mosquittoContainer.isRunning(), "Mosquitto container не запущен");
    }

    @Test
    @Order(2)
    void testMessageFlow() throws Exception {
        String mqttUrl = "tcp://" + mosquittoContainer.getHost() + ":" + mosquittoContainer.getMappedPort(1883);
        String clientId = "TestClientSender";
        MqttClient mqttClient = new MqttClient(mqttUrl, clientId, new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        mqttClient.connect(options);

        String validJsonPayload = """
            {
              "deviceId": 123,
              "radiatorTemp": 60,
              "roomTemp": 22,
              "timestamp": 1681300000000,
              "heat": 10
            }
            """;

        MqttMessage message = new MqttMessage(validJsonPayload.getBytes(StandardCharsets.UTF_8));
        message.setQos(2);

        mqttClient.publish("radiator/data", message);

        Thread.sleep(5000);

        com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
        factory.setHost(rabbitMQContainer.getHost());
        factory.setPort(rabbitMQContainer.getMappedPort(5672));
        factory.setUsername(rabbitMQContainer.getAdminUsername());
        factory.setPassword(rabbitMQContainer.getAdminPassword());
        Connection conn = factory.newConnection();
        Channel ch = conn.createChannel();
        ch.queueDeclare(RABBIT_QUEUE_NAME, false, false, false, null);
        GetResponse response = ch.basicGet(RABBIT_QUEUE_NAME, true);
        assertNotNull(response, "No message in RabbitMQ");
        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        System.out.println("RabbitMQ message body: " + body);
        ch.close();
        conn.close();
        mqttClient.disconnect();
        mqttClient.close();

        String realMongoUri = "mongodb://" + mongoDBContainer.getHost() + ":" + mongoDBContainer.getMappedPort(27017);
        try (var mongoClient = MongoClients.create(new ConnectionString(realMongoUri))) {
            MongoDatabase db = mongoClient.getDatabase(MONGO_DB_NAME);
            MongoCollection<Document> collection = db.getCollection(MONGO_COLLECTION);
            long count = collection.countDocuments();
            Assertions.assertTrue(count > 0, "No docs in MongoDB");

            Document found = collection.find(new Document("device_id", 123L)).first();
            assertNotNull(found, "Can't find doc with device_id=123 in MongoDB");
            assertEquals(60.0, found.getDouble("radiatorTemp"), 0.001, "radiatorTemp didn't match");
            assertEquals(22.0, found.getDouble("roomTemp"), 0.001, "roomTemp didn't match");
        }
    }
}
