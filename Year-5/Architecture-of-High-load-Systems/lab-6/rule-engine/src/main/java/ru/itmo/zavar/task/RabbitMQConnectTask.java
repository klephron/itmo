package ru.itmo.zavar.task;

import com.rabbitmq.client.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;

import java.util.concurrent.ExecutorService;

@RequiredArgsConstructor
public final class RabbitMQConnectTask implements Runnable {
    private final ConnectionFactory factory;
    private final ExecutorService executor;
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

            ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
                @Override
                public void handleRecovery(Recoverable recoverable) {
                    LoggerWrapper.log(Level.INFO, "Connection recovered successfully!");
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
