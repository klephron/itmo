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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Rule(name = "ComplexRule", description = "Test")
public class ComplexRule extends AbstractRule {
    private final Gson gson = new Gson();

    public ComplexRule(MongoDBConnectTask mongoDBConnectTask, String mongoDbName, String mongoCollectionName, String replicaNumber) {
        super(mongoDBConnectTask, mongoDbName, mongoCollectionName, replicaNumber, MetricsConfig.getRegistry().counter("iotr_complex_rule_fire_count"));
    }

    @Condition
    public boolean when(Facts facts) {
        AtomicInteger factsCheck = new AtomicInteger();
        facts.iterator().forEachRemaining(fact -> {
            String[] value = (String[]) fact.getValue();
            if (Double.parseDouble(value[2]) > 60.0 && Double.parseDouble(value[2]) < 70.0) {
                factsCheck.getAndIncrement();
            }
        });
        return factsCheck.get() == 10;
    }

    @Action
    public void then(Facts facts) throws InterruptedException {
        String[] value = (String[]) facts.getFact("0").getValue();
        super.sendToMongo(gson.toJson(facts.asMap()), Long.valueOf(value[0]));
        super.incCounter();
        LoggerWrapper.log(Level.INFO,"Complex rule done for radiator {}", Long.valueOf(value[0]));
    }
}
