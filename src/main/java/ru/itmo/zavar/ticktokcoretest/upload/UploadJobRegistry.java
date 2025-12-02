package ru.itmo.zavar.ticktokcoretest.upload;

import org.springframework.stereotype.Component;
import ru.itmo.zavar.ticktokcoretest.dto.UploadJobView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class UploadJobRegistry {

    private final Map<UUID, UploadJobStatus> jobs = new ConcurrentHashMap<>();

    public void put(UUID id, UploadJobStatus status) {
        jobs.put(id, status);
    }

    public Optional<UploadJobStatus> get(UUID id) {
        return Optional.ofNullable(jobs.get(id));
    }

    public void update(UUID id, Consumer<UploadJobStatus> updater) {
        jobs.computeIfPresent(id, (k, v) -> {
            updater.accept(v);
            return v;
        });
    }

    public List<UploadJobView> getActiveJobs() {
        return jobs.entrySet().stream()
                .filter(e -> e.getValue().getStage() != JobStage.DONE
                        && e.getValue().getStage() != JobStage.ERROR)
                .map(e -> {
                    UploadJobStatus s = e.getValue();
                    return new UploadJobView(
                            e.getKey(),
                            s.getStage(),
                            s.getPercent(),
                            s.getMessage(),
                            s.getFileName()
                    );
                })
                .toList();
    }
}

