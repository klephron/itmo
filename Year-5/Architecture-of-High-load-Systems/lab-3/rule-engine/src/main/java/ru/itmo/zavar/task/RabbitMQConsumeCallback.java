package ru.itmo.zavar.task;

import com.google.gson.Gson;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.core.instrument.Counter;
import org.jeasy.rules.api.Facts;
import org.jeasy.rules.api.Rules;
import org.jeasy.rules.api.RulesEngine;
import org.jeasy.rules.core.DefaultRulesEngine;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.metrics.MetricsConfig;
import ru.itmo.zavar.payload.RadiatorRabbitPayload;
import ru.itmo.zavar.redis.RedisFactStorage;
import ru.itmo.zavar.rules.AbstractRule;
import ru.itmo.zavar.rules.ComplexRule;
import ru.itmo.zavar.rules.SimpleRule;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class RabbitMQConsumeCallback implements DeliverCallback {

    private final RedisFactStorage redisFactStorage;
    private final RedisCommands<String, String> redis;
    private final Gson gson = new Gson();
    private final Rules rules = new Rules();
    private final RulesEngine rulesEngine = new DefaultRulesEngine();
    private final Counter factsCounter = MetricsConfig.getRegistry().counter("iotr_facts_count");

    public RabbitMQConsumeCallback(RedisFactStorage redisFactStorage, RedisCommands<String, String> redis,
                                   MongoDBConnectTask mongoDBConnectTask, String mongoDbName, String mongoCollectionName,
                                   String replicaNumber) {
        this.redisFactStorage = redisFactStorage;
        this.redis = redis;
        rules.register(new SimpleRule(mongoDBConnectTask, mongoDbName, mongoCollectionName, replicaNumber),
                new ComplexRule(mongoDBConnectTask, mongoDbName, mongoCollectionName, replicaNumber));
    }

    @Override
    public void handle(String s, Delivery delivery) throws IOException {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        RadiatorRabbitPayload payload = gson.fromJson(message, RadiatorRabbitPayload.class);

        LoggerWrapper.log(Level.DEBUG,"Received: {}", payload);
        redisFactStorage.addFact(payload);
        factsCounter.increment();

        for (String key : redis.keys("radiator:" + payload.deviceId())) {
            List<String[]> facts = redisFactStorage.getFacts(key);
            LoggerWrapper.log(Level.INFO,"Key {} has {} facts:", key, facts.size());
            facts.forEach(s1 -> {
                LoggerWrapper.log(Level.INFO, Arrays.toString(s1));
            });
            LoggerWrapper.log(Level.INFO, "Processing rules for {}", key);
            processKey(facts);
        }
    }

    private void processKey(List<String[]> list) {
        Facts facts = new Facts();
        for (int i = 0; i < list.size(); i++) {
            facts.put(String.valueOf(i), list.get(i));
        }
        rulesEngine.fire(rules, facts);
    }
}
