package ru.itmo.zavar.task;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@RequiredArgsConstructor
public final class RabbitMQConnectTask implements Runnable {
    private final ConnectionFactory factory;
    private final ExecutorService executor;
    private final MqttClient client;
    private final String rabbitQueueName;
    private final int maxRetries;
    private int attempt = 1;
    @Getter
    private Connection connection;
    @Getter
    private Channel channel;


    @Override
    public void run() {
        try {
            connection = factory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare(rabbitQueueName, false, false, false, null);

            connection.addShutdownListener(e -> {
                try {
                    client.disconnect();
                } catch (MqttException ex) {
                    LoggerWrapper.log(Level.ERROR, "Failed to disconnect from MQTT topic: {}", e.getMessage());
                }
            });

            ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
                @Override
                public void handleRecovery(Recoverable recoverable) {
                    LoggerWrapper.log(Level.INFO, "Connection recovered successfully!");
                    try {
                        client.reconnect();
                    } catch (MqttException e) {
                        LoggerWrapper.log(Level.ERROR, "Failed to reconnect to MQTT topic: {}", e.getMessage());
                    }
                }

                @Override
                public void handleRecoveryStarted(Recoverable recoverable) {
                    LoggerWrapper.log(Level.INFO, "Started RabbitMQ connection recovery");
                }
            });

            LoggerWrapper.log(Level.INFO, "Connection established successfully on attempt {}", attempt);
            executor.shutdown();
        } catch (Exception e) {
            LoggerWrapper.log(Level.ERROR, "Failed to connect or publish (attempt {}): {}", attempt, e.getMessage());
            if (attempt++ >= maxRetries) {
                LoggerWrapper.log(Level.ERROR, "All retries exhausted. Stopping retries.");
                executor.shutdown();
                System.exit(1);
            }
        }
    }
}
