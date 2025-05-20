package ru.itmo.zavar.rules;

import com.google.gson.Gson;
import org.jeasy.rules.annotation.Action;
import org.jeasy.rules.annotation.Condition;
import org.jeasy.rules.annotation.Rule;
import org.jeasy.rules.api.Facts;
import org.slf4j.event.Level;
import ru.itmo.zavar.log.LoggerWrapper;
import ru.itmo.zavar.metrics.MetricsConfig;
import ru.itmo.zavar.task.MongoDBConnectTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Rule(name = "SimpleRule", description = "Test")
public class SimpleRule extends AbstractRule {
    private final Gson gson = new Gson();

    public SimpleRule(MongoDBConnectTask mongoDBConnectTask, String mongoDbName, String mongoCollectionName, String replicaNumber) {
        super(mongoDBConnectTask, mongoDbName, mongoCollectionName, replicaNumber, MetricsConfig.getRegistry().counter("iotr_simple_rule_fire_count"));
    }

    @Condition
    public boolean when(Facts facts) {
        String[] strings = (String[]) facts.getFact("0").getValue();
        return Double.parseDouble(strings[2]) > 70.0;
    }

    @Action
    public void then(Facts facts) throws InterruptedException {
        String[] fact = (String[]) facts.getFact("0").getValue();
        Map<String, Object> temp = new HashMap<>();
        temp.put("0", facts.getFact("0").getValue());
        super.sendToMongo(gson.toJson(temp), Long.valueOf(fact[0]));
        super.incCounter();
        LoggerWrapper.log(Level.INFO,"Simple rule done for radiator {}: {}", fact[0], facts.getFact("0"));
    }
}
