package ru.itmo.zavar.rules;

import com.mongodb.client.MongoCollection;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.metrics.MetricsConfig;
import ru.itmo.zavar.payload.RuleMongoPayload;
import ru.itmo.zavar.task.MongoDBConnectTask;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public abstract class AbstractRule {

    private final MongoDBConnectTask mongoDBConnectTask;
    private final String mongoDbName;
    private final String mongoCollectionName;
    private final String replicaNumber;
    private final Counter ruleCounter;
    private final Counter allRuleCounter = MetricsConfig.getRegistry().counter("iotr_all_rule_fire_count");

    public abstract boolean when(Facts facts);

    public abstract void then(Facts facts) throws InterruptedException;

    protected void incCounter() {
        allRuleCounter.increment();
        ruleCounter.increment();
    }

    protected void sendToMongo(String payload, Long radiatorId) throws InterruptedException {
        RuleMongoPayload ruleMongoPayload = new RuleMongoPayload(getClass().getName(), replicaNumber, System.currentTimeMillis(), radiatorId, payload);
        try {
            mongoDBConnectTask.getMongoClient().getDatabase("admin").runCommand(new Document("ping", 1));
        } catch (Exception e) {
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            mongoDBConnectTask.setExecutor(executor);
            executor.scheduleWithFixedDelay(mongoDBConnectTask, 0, 3000, TimeUnit.MILLISECONDS);
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }

        MongoCollection<RuleMongoPayload> ruleCollection = mongoDBConnectTask.getMongoClient().getDatabase(mongoDbName).getCollection(mongoCollectionName, RuleMongoPayload.class);
        ruleCollection.insertOne(ruleMongoPayload);
        LoggerWrapper.log(Level.INFO, "Inserted rule for radiator {} in mongo: {}", radiatorId, payload);
    }
}
