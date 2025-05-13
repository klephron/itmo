package ru.itmo.zavar.redis;

import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.sync.RedisCommands;
import ru.itmo.zavar.payload.RadiatorRabbitPayload;

import java.util.List;
import java.util.stream.Collectors;

public class RedisFactStorage {
    private final RedisCommands<String, String> redis;
    private final int maxFacts;

    public RedisFactStorage(RedisCommands<String, String> redis, int maxFacts) {
        this.redis = redis;
        this.maxFacts = maxFacts;
    }

    public void addFact(RadiatorRabbitPayload payload) {
        String script = "redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2]);" +
                "local excess = redis.call('ZCARD', KEYS[1]) - tonumber(ARGV[3]);" +
                "if excess > 0 then redis.call('ZREMRANGEBYRANK', KEYS[1], 0, excess - 1); end";

        redis.eval(script, ScriptOutputType.STATUS,
                new String[]{"radiator:" + payload.deviceId()},
                String.valueOf(payload.timestamp()),
                payload.deviceId() + ":" + payload.roomTemp() + ":" + payload.radiatorTemp() + ":" + payload.timestamp(),
                String.valueOf(maxFacts));
    }

    public List<String[]> getFacts(String key) {
        return redis.zrange(key, 0, -1).stream().map(s -> s.split(":")).collect(Collectors.toList());
    }
}
