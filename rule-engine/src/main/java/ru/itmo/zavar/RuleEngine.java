package ru.itmo.zavar;

import ch.qos.logback.classic.LoggerContext;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import net.logstash.logback.appender.LogstashTcpSocketAppender;
import net.logstash.logback.encoder.LogstashEncoder;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.metrics.MetricsServer;
import ru.itmo.zavar.redis.RedisFactStorage;
import ru.itmo.zavar.task.MongoDBConnectTask;
import ru.itmo.zavar.task.RabbitMQConnectTask;
import ru.itmo.zavar.task.RabbitMQConsumeCallback;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class RuleEngine {

    private static String rabbitMqHost = "localhost";
    private static int rabbitMqPort = 5672;
    private static String rabbitMqUser = "guest";
    private static String rabbitMqPass = "guest";
    private static String rabbitQueueName = "testQueue";
    private static String redisHost = "localhost";
    private static int redisPort = 6379;
    private static String mongoDbName = "iot-db";
    private static String mongoUri = "mongodb://zavar:zavar@mongodb:27017/";
    private static String mongoDbCollection = "rules";
    private static String replicaNumber = String.valueOf(System.currentTimeMillis());

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length >= 1) {
            redisHost = args[0];
        }
        if (args.length >= 2) {
            redisPort = Integer.parseInt(args[1]);
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

        MetricsServer metricsServer = new MetricsServer(8087);
        metricsServer.start();

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
        RabbitMQConnectTask rabbitMQConnectTask = new RabbitMQConnectTask(factory, executor, rabbitQueueName, maxRetries);

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
            LoggerWrapper.log(Level.DEBUG,"Collection is already exists");
        }

        RedisClient redisClient = RedisClient.create("redis://" + redisHost + ":" + redisPort);
        RedisCommands<String, String> redis = redisClient.connect().sync();
        RedisFactStorage redisFactStorage = new RedisFactStorage(redis, 10);

        try {
            channel.queueDeclare(rabbitQueueName, false, false, false, null);
            LoggerWrapper.log(Level.INFO,"Waiting for messages...");
            channel.basicConsume(rabbitQueueName, true, new RabbitMQConsumeCallback(redisFactStorage, redis,
                    mongoDBConnectTask, mongoDbName, mongoDbCollection, replicaNumber), consumerTag -> {});
        } catch (IOException e) {
            LoggerWrapper.log(Level.ERROR, e.getMessage());
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (channel.isOpen())
                    channel.close();
                if (connection.isOpen())
                    connection.close();
                LoggerWrapper.log(Level.INFO,"RabbitMQ Client disconnected.");
            } catch (IOException | TimeoutException e) {
                LoggerWrapper.log(Level.ERROR,"Error while closing RabbitMQ client: {}", e.getMessage());
            }
            metricsServer.stop();
            redisClient.shutdown();
        }));
    }
}