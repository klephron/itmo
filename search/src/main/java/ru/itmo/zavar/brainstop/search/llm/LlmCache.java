package ru.itmo.zavar.brainstop.search.llm;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LlmCache<T> {
    private final ConcurrentHashMap<String, CacheEntry<T>> entries = new ConcurrentHashMap<>();
    private final Duration ttl;

    public LlmCache(Duration ttl) {
        this.ttl = ttl;
    }

    public Optional<T> get(String key) {
        if (!enabled() || key == null || key.isBlank()) {
            return Optional.empty();
        }
        CacheEntry<T> entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.ofNullable(entry.value());
    }

    public void put(String key, T value) {
        if (!enabled() || key == null || key.isBlank()) {
            return;
        }
        entries.put(key, new CacheEntry<>(value, Instant.now().plus(ttl)));
    }

    private boolean enabled() {
        return ttl != null && !ttl.isZero() && !ttl.isNegative();
    }

    private record CacheEntry<T>(T value, Instant expiresAt) {
    }
}
