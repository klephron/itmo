
package ru.itmo.zavar.ticktokcoretest.transcoding.jobs;

import org.springframework.stereotype.Component;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.dto.UploadJobViewDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class UploadJobRegistry implements TranscodingJobRegistry {

    private final Map<UUID, UploadJobStatus> jobs = new ConcurrentHashMap<>();

    @Override
    public void put(UUID id, TranscodingJobStatus status) {
        this.jobs.put(id, status instanceof UploadJobStatus uj ? uj : new UploadJobStatus(status));
    }

    @Override
    public Optional<TranscodingJobStatus> get(UUID id) {
        return Optional.ofNullable(this.jobs.get(id));
    }

    @Override
    public void update(UUID id, Consumer<TranscodingJobStatus> updater) {
        this.jobs.computeIfPresent(id, (k, v) -> {
            updater.accept(v);
            return v;
        });
    }

    @Override
    public Map<UUID, TranscodingJobStatus> getActiveJobsForUser(Long userId) {
        return this.jobs.entrySet().stream()
                .filter(e -> e.getValue().getUserId() != null
                        && e.getValue().getUserId().equals(userId))
                .filter(e -> e.getValue().getStage() != TranscodingJobStage.DONE
                        && e.getValue().getStage() != TranscodingJobStage.ERROR)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public List<UploadJobViewDto> getActiveJobs() {
        return this.jobs.entrySet().stream()
                .filter(e -> e.getValue().getStage() != TranscodingJobStage.DONE
                        && e.getValue().getStage() != TranscodingJobStage.ERROR)
                .map(e -> {
                    TranscodingJobStatus s = e.getValue();
                    return new UploadJobViewDto(
                            e.getKey(),
                            s.getStage(),
                            s.getPercent(),
                            s.getMessage(),
                            s.getFileName()
                    );
                })
                .toList();
    }

    public List<UploadJobViewDto> getActiveJobsForUserView(Long userId) {
        return this.jobs.entrySet().stream()
                .filter(e -> e.getValue().getUserId() != null
                        && e.getValue().getUserId().equals(userId))
                .filter(e -> e.getValue().getStage() != TranscodingJobStage.DONE
                        && e.getValue().getStage() != TranscodingJobStage.ERROR)
                .map(e -> {
                    TranscodingJobStatus s = e.getValue();
                    return new UploadJobViewDto(
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
