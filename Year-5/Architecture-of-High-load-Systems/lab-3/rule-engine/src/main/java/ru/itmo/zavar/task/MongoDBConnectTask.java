package ru.itmo.zavar.task;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;

import java.util.concurrent.ExecutorService;

public final class MongoDBConnectTask implements Runnable {
    @Getter
    private MongoClient mongoClient = null;
    private final MongoClientSettings settings;
    @Setter
    private ExecutorService executor;
    private final int maxRetries;
    private int attempt = 1;

    public MongoDBConnectTask(MongoClientSettings settings, ExecutorService executor, int maxRetries) {
        this.settings = settings;
        this.executor = executor;
        this.maxRetries = maxRetries;
    }

    @Override
    public void run() {
        try {
            LoggerWrapper.log(Level.INFO,"Connecting to MongoDB...");
            mongoClient = MongoClients.create(settings);

            mongoClient.getDatabase("admin").runCommand(new Document("ping", 1));
            LoggerWrapper.log(Level.INFO,"MongoDB is available");
            executor.shutdown();
        } catch (Exception e) {
            LoggerWrapper.log(Level.ERROR,"MongoDB is unavailable: " + e.getMessage());
            if (mongoClient != null) {
                mongoClient.close();
                mongoClient = null;
            }
            if (attempt++ >= maxRetries) {
                LoggerWrapper.log(Level.ERROR, "All retries exhausted. Stopping retries.");
                executor.shutdown();
                System.exit(1);
            }
        }
    }
}
