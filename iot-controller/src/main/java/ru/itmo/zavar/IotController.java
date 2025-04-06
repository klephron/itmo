package ru.itmo.zavar;

import ch.qos.logback.classic.LoggerContext;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.metrics.MetricsConfig;
import ru.itmo.zavar.metrics.MetricsServer;
import ru.itmo.zavar.mqtt.IotMqttCallback;
import ru.itmo.zavar.task.MongoDBConnectTask;
import ru.itmo.zavar.task.RabbitMQConnectTask;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class IotController {
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
    private static String replicaNumber = String.valueOf(System.currentTimeMillis());
    private static int qosLevel = 2;

    public static void main(String[] args) throws MqttException, InterruptedException, IOException {

        Logger.getLogger("org.eclipse.paho.client.mqttv3").setLevel(java.util.logging.Level.OFF);
        Logger.getLogger("org.eclipse.paho.mqttv5.client").setLevel(java.util.logging.Level.OFF);

        if (args.length >= 1) {
            mqttBrokerUrl = args[0];
        }
        if (args.length >= 2) {
            mqttTopic = args[1];
        }
        if (args.length >= 3) {
            rabbitMqHost = args[2];
        }
        if (args.length >= 4) {
            rabbitMqPort = Integer.parseInt(args[3]);
        }
        if (args.length >= 5) {
            rabbitMqUser = args[4];
        }
        if (args.length >= 6) {
            rabbitMqPass = args[5];
        }
        if (args.length >= 7) {
            rabbitQueueName = args[6];
        }
        if (args.length >= 8) {
            mongoUri = args[7];
        }
        if (args.length >= 9) {
            mongoDbName = args[8];
        }
        if (args.length >= 10) {
            mongoDbCollection = args[9];
        }
        if (args.length >= 11) {
            replicaNumber = args[10];
        }

        mongoUri = mongoUri + mongoDbName;

        LoggerWrapper.log(Level.INFO, ("=== IotController {}==="), replicaNumber);
        LoggerWrapper.log(Level.INFO, "BROKER_URL = {}", mqttBrokerUrl);
        LoggerWrapper.log(Level.INFO, "TOPIC      = {}", mqttTopic);

        MetricsServer metricsServer = new MetricsServer(8087);
        metricsServer.start();

        String clientId = "Consumer-" + replicaNumber;
        MqttClient client = new MqttClient(mqttBrokerUrl, clientId, new MemoryPersistence());
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setCleanStart(true);
        //options.setSessionExpiryInterval(0L);
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
        RabbitMQConnectTask rabbitMQConnectTask = new RabbitMQConnectTask(factory, executor, client, rabbitQueueName, maxRetries);

        executor.scheduleWithFixedDelay(rabbitMQConnectTask, 0, delayMs, TimeUnit.MILLISECONDS);
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        Connection connection = rabbitMQConnectTask.getConnection();
        Channel channel = rabbitMQConnectTask.getChannel();


        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .codecRegistry(pojoCodecRegistry)
                .build();

        executor = Executors.newSingleThreadScheduledExecutor();
        MongoDBConnectTask mongoDBConnectTask = new MongoDBConnectTask(settings, executor, maxRetries);
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


        client.setCallback(new IotMqttCallback(channel, connection, rabbitQueueName, mongoDBConnectTask, mongoDbName, mongoDbCollection, subscribeRunnable));
        client.connect(options);


        ScheduledExecutorService finalExecutor = executor;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
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
