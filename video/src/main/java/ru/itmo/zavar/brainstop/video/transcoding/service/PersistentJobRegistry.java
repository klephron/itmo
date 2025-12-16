package ru.itmo.zavar.brainstop.video.transcoding.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.brainstop.common.api.transcoding.dto.UploadJobViewDto;
import ru.itmo.zavar.brainstop.video.transcoding.model.UploadJobEntity;
import ru.itmo.zavar.brainstop.video.transcoding.repo.UploadJobRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PersistentJobRegistry implements TranscodingJobRegistry {

    private final UploadJobRepository repository;

    @Override
    @Transactional
    public void put(UUID id, TranscodingJobStatus status) {
        UploadJobEntity entity = repository.findById(id).orElseGet(UploadJobEntity::new);
        entity.setId(id);
        applyStatus(entity, status);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TranscodingJobStatus> get(UUID id) {
        return repository.findById(id).map(this::toStatus);
    }

    @Override
    @Transactional
    public void update(UUID id, Consumer<TranscodingJobStatus> updater) {
        repository.findById(id).ifPresent(entity -> {
            TranscodingJobStatus status = toStatus(entity);
            updater.accept(status);
            applyStatus(entity, status);
            entity.setUpdatedAt(Instant.now());
            repository.save(entity);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, TranscodingJobStatus> getActiveJobsForUser(Long userId) {
        return repository.findByUserId(userId).stream()
                .filter(e -> e.getStage() != TranscodingJobStage.DONE && e.getStage() != TranscodingJobStage.ERROR)
                .collect(Collectors.toMap(UploadJobEntity::getId, this::toStatus));
    }

    public List<UploadJobViewDto> getActiveJobsForUserView(Long userId) {
        return repository.findByUserId(userId).stream()
                .filter(e -> e.getStage() != TranscodingJobStage.DONE && e.getStage() != TranscodingJobStage.ERROR)
                .map(e -> new UploadJobViewDto(e.getId(), e.getStage(), e.getPercent(), e.getMessage(), e.getVideoName()))
                .toList();
    }

    private TranscodingJobStatus toStatus(UploadJobEntity entity) {
        return new TranscodingJobStatus(entity.getStage(), entity.getPercent(), entity.getMessage(), entity.getVideoName(), entity.getUserId());
    }

    private void applyStatus(UploadJobEntity entity, TranscodingJobStatus status) {
        entity.setStage(status.getStage());
        entity.setPercent(status.getPercent());
        entity.setMessage(status.getMessage());
        entity.setVideoName(status.getFileName());
        entity.setUserId(status.getUserId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setUpdatedAt(Instant.now());
    }
}
