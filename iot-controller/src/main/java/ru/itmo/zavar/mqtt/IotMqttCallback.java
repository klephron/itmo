package ru.itmo.zavar.mqtt;

import com.google.gson.Gson;
import com.mongodb.client.MongoCollection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Tags;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.metrics.MetricsConfig;
import ru.itmo.zavar.payload.RadiatorMongoData;
import ru.itmo.zavar.payload.RadiatorMqttPayload;
import ru.itmo.zavar.task.MongoDBConnectTask;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public final class IotMqttCallback implements MqttCallback {
    private final Channel channel;
    private final Connection connection;
    private final String rabbitQueueName;
    private final MongoDBConnectTask mongoDBConnectTask;
    private final String mongoDbName;
    private final String mongoCollectionName;
    private final Runnable subscribeRunnable;
    private final Counter incomeMessagesCounter = MetricsConfig.getRegistry().counter("iotc_income_messages");
    private final Counter outcomeMessagesCounter = MetricsConfig.getRegistry().counter("iotc_outcome_messages");
    private final Counter invalidMessagesCounter = MetricsConfig.getRegistry().counter("iotc_invalid_messages");
    private final DistributionSummary requestExecutionSummary = MetricsConfig.getRegistry().summary("iotc_request_execution");
    private final AtomicLong lastExecutionTime = new AtomicLong(0);

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        MetricsConfig.getRegistry().gauge("iotc_request_execution_time", Tags.of("type", "task"), lastExecutionTime, AtomicLong::get);
        subscribeRunnable.run();
        LoggerWrapper.log(Level.INFO,"Connected to " + serverURI + " (reconnect=" + reconnect + ")");
    }

    @Override
    public void disconnected(MqttDisconnectResponse disconnectResponse) {
        try {
            channel.close();
            connection.close();
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
        LoggerWrapper.log(Level.ERROR,"Disconnected: {}", disconnectResponse.getReasonString());
    }

    @Override
    public void mqttErrorOccurred(MqttException exception) {
        LoggerWrapper.log(Level.ERROR,"MQTT Error: {}", exception.getMessage());
    }

    @Override
    public void messageArrived(String incomingTopic, MqttMessage message) throws Exception {
        long startTime = System.nanoTime();
        incomeMessagesCounter.increment();
        LoggerWrapper.log(Level.INFO,"=== Message Arrived ===");
        LoggerWrapper.log(Level.INFO,"Topic: " + incomingTopic);
        LoggerWrapper.log(Level.INFO,"Payload: " + new String(message.getPayload()));
        RadiatorMqttPayload payload = new Gson().fromJson(new String(message.getPayload()), RadiatorMqttPayload.class);

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<RadiatorMqttPayload>> violations = validator.validate(payload);
        boolean error = false;
        if (!violations.isEmpty()) {
            for (ConstraintViolation<RadiatorMqttPayload> v : violations) {
                error = true;
                LoggerWrapper.log(Level.ERROR,"Validation error: {} {}", v.getPropertyPath(), v.getMessage());
            }
        }
        if (!error) {
            try {
                mongoDBConnectTask.getMongoClient().getDatabase("admin").runCommand(new Document("ping", 1));
            } catch (Exception e) {
                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                mongoDBConnectTask.setExecutor(executor);
                executor.scheduleWithFixedDelay(mongoDBConnectTask, 0, 3000, TimeUnit.MILLISECONDS);
                try {
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ex) {
                    LoggerWrapper.log(Level.ERROR,"Interrupted while waiting for mongoDB connect task");
                }
            }


            MongoCollection<RadiatorMongoData> radiatorCollection = mongoDBConnectTask.getMongoClient().getDatabase(mongoDbName).getCollection(mongoCollectionName, RadiatorMongoData.class);
            radiatorCollection.insertOne(
                    new RadiatorMongoData(
                            new ObjectId().toString(),
                            payload.deviceId(),
                            payload.radiatorTemp(),
                            payload.roomTemp(),
                            payload.timestamp(),
                            payload.heat()
                    ));
            try {
                channel.basicPublish("", rabbitQueueName, null, new String(message.getPayload()).getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                LoggerWrapper.log(Level.ERROR,"IO Error: {}", e.getMessage());
            }
            outcomeMessagesCounter.increment();
        } else {
            invalidMessagesCounter.increment();
        }
        long duration = System.nanoTime() - startTime;
        this.requestExecutionSummary.record(duration);
        lastExecutionTime.set(TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS));
    }

    @Override
    public void deliveryComplete(IMqttToken token) {
        LoggerWrapper.log(Level.INFO,"Delivery completed for token: " + token.getMessageId());
    }

    @Override
    public void authPacketArrived(int reasonCode, MqttProperties properties) {

    }
}
