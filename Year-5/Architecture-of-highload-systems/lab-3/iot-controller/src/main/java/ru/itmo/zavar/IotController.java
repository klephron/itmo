package ru.itmo.zavar;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.event.Level;

import ru.itmo.zavar.consul.ConsulClient;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.metrics.MetricsServer;
import ru.itmo.zavar.mqtt.IotMqttCallback;
import ru.itmo.zavar.task.MongoDBConnectTask;
import ru.itmo.zavar.task.RabbitMQConnectTask;
import ru.itmo.zavar.task.exithandler.DefaultExitHandler;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.logging.Logger;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class IotController {
    private static String publicAddress = System.getenv("HOSTNAME");
    private static String mqttBrokerUrl = "tcp://localhost:1883";
    private static String mqttTopic = "$share/myGroup/radiator/data";
    private static String rabbitMqHost = "localhost";
    private static int rabbitMqPort = 5672;
    private static String rabbitMqUser = "guest";
    private static String rabbitMqPass = "guest";
    private static String rabbitQueueName = "testQueue";
    private static String mongoDbName = "iot-db";
    private static String mongoUri = "mongodb://zavar:zavar@mongodb:27017/";
    private static String mongoDbCollection = "rules";
    private static String replicaId = String.valueOf(System.currentTimeMillis());
    private static int qosLevel = 2;
    private static String consulHost = "localhost";
    private static int consulPort = 8500;

    public static void main(String[] args) throws MqttException, InterruptedException, IOException {

        Logger.getLogger("org.eclipse.paho.client.mqttv3").setLevel(java.util.logging.Level.OFF);
        Logger.getLogger("org.eclipse.paho.mqttv5.client").setLevel(java.util.logging.Level.OFF);

        if (System.getenv("MQTT_BROKER_URL") != null) {
            mqttBrokerUrl = System.getenv("MQTT_BROKER_URL");
        }
        if (System.getenv("MQTT_TOPIC") != null) {
            mqttTopic = System.getenv("MQTT_TOPIC");
        }
        if (System.getenv("RABBIT_MQ_HOST") != null) {
            rabbitMqHost = System.getenv("RABBIT_MQ_HOST");
        }
        if (System.getenv("RABBIT_MQ_PORT") != null) {
            rabbitMqPort = Integer.parseInt(System.getenv("RABBIT_MQ_PORT"));
        }
        if (System.getenv("RABBIT_MQ_USER") != null) {
            rabbitMqUser = System.getenv("RABBIT_MQ_USER");
        }
        if (System.getenv("RABBIT_MQ_PASS") != null) {
            rabbitMqPass = System.getenv("RABBIT_MQ_PASS");
        }
        if (System.getenv("RABBIT_QUEUE_NAME") != null) {
            rabbitQueueName = System.getenv("RABBIT_QUEUE_NAME");
        }
        if (System.getenv("MONGO_URI") != null) {
            mongoUri = System.getenv("MONGO_URI");
        }
        if (System.getenv("MONGO_DB_NAME") != null) {
            mongoDbName = System.getenv("MONGO_DB_NAME");
        }
        if (System.getenv("MONGO_DB_COLLECTION") != null) {
            mongoDbCollection = System.getenv("MONGO_DB_COLLECTION");
        }
        if (System.getenv("CONSUL_HOST") != null) {
            consulHost = System.getenv("CONSUL_HOST");
        }
        if (System.getenv("CONSUL_PORT") != null) {
            consulPort = Integer.parseInt(System.getenv("CONSUL_PORT"));
        }

        replicaId = args[args.length - 1];
        publicAddress = args[args.length - 2];

        mongoUri = mongoUri + mongoDbName;

        LoggerWrapper.log(Level.INFO, ("=== IotController {}==="), replicaId);
        LoggerWrapper.log(Level.INFO, "BROKER_URL = {}", mqttBrokerUrl);
        LoggerWrapper.log(Level.INFO, "TOPIC      = {}", mqttTopic);

        MetricsServer metricsServer = new MetricsServer(8087);
        metricsServer.start();

        String clientId = "Consumer-" + replicaId;
        MqttClient client = new MqttClient(mqttBrokerUrl, clientId, new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        // options.setSessionExpiryInterval(0L);
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
                new DefaultExitHandler(), MongoClients::create);
        executor.scheduleWithFixedDelay(mongoDBConnectTask, 0, delayMs, TimeUnit.MILLISECONDS);
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        MongoClient mongoClient = mongoDBConnectTask.getMongoClient();
        MongoDatabase database = mongoClient.getDatabase(mongoDbName);
        try {
            database.createCollection(mongoDbCollection);
        } catch (Exception exception) {
            LoggerWrapper.log(Level.DEBUG, "Collection is already exists");
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

        String consulUrl = "http://" + consulHost + ":" + consulPort;
        ConsulClient consulClient = new ConsulClient(consulUrl, publicAddress, 8087, "iot-controller", replicaId);
        consulClient.register();

        ScheduledExecutorService finalExecutor = executor;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                consulClient.deregister();
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
                LoggerWrapper.log(Level.INFO, "MQTT Client disconnected and closed.");
            } catch (MqttException e) {
                LoggerWrapper.log(Level.ERROR, "Error while closing MQTT client: {}", e.getMessage());
            }
            try {
                if (channel.isOpen())
                    channel.close();
                if (connection.isOpen())
                    connection.close();
                LoggerWrapper.log(Level.INFO, "RabbitMQ Client disconnected.");
            } catch (IOException | TimeoutException e) {
                LoggerWrapper.log(Level.ERROR, "Error while closing RabbitMQ client: {}", e.getMessage());
            }
            mongoClient.close();
            metricsServer.stop();
            finalExecutor.close();
        }, "Shutdown hook"));
    }
}
