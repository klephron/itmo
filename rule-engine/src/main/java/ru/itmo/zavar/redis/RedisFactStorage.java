package ru.itmo.zavar.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;
import ru.itmo.zavar.payload.RadiatorRabbitPayload;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisFactStorage {
    private final RedisCommands<String, String> redis;
    private final int maxFacts;

    public RedisFactStorage(RedisCommands<String, String> redis, int maxFacts) {
        this.redis = redis;
        this.maxFacts = maxFacts;
    }

    public void addFact(RadiatorRabbitPayload payload) {
        String key = "radiator:" + payload.deviceId();
        redis.zadd(key, payload.timestamp(),
                payload.deviceId() + ":" + payload.roomTemp() + ":"
                        + payload.radiatorTemp() + ":" + payload.timestamp());

        long excessCount = redis.zcard(key) - maxFacts;
        if (excessCount > 0) {
            redis.zremrangebyrank(key, 0, excessCount - 1);
        }
    }

    public List<String[]> getFacts(String key) {
        return redis.zrange(key, 0, -1).stream().map(s -> s.split(":")).collect(Collectors.toList());
    }
}
