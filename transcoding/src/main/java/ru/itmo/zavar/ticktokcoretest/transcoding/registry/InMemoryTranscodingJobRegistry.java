package ru.itmo.zavar.ticktokcoretest.transcoding.registry;

import org.springframework.stereotype.Component;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class InMemoryTranscodingJobRegistry implements TranscodingJobRegistry {

    private final Map<UUID, TranscodingJobStatus> jobs = new ConcurrentHashMap<>();

    @Override
    public void put(UUID id, TranscodingJobStatus status) {
        jobs.put(id, status);
    }

    @Override
    public Optional<TranscodingJobStatus> get(UUID id) {
        return Optional.ofNullable(jobs.get(id));
    }

    @Override
    public void update(UUID id, Consumer<TranscodingJobStatus> updater) {
        jobs.computeIfPresent(id, (key, val) -> {
            updater.accept(val);
            return val;
        });
    }

    @Override
    public Map<UUID, TranscodingJobStatus> getActiveJobsForUser(Long userId) {
        return Map.of(); // not tracked in this service
    }
}
