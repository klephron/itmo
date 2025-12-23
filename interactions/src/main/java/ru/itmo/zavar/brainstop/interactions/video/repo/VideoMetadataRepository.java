package ru.itmo.zavar.brainstop.interactions.video.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.brainstop.interactions.video.model.VideoMetadataEntity;

import java.util.Optional;
import java.util.UUID;

public interface VideoMetadataRepository extends JpaRepository<VideoMetadataEntity, Long> {
    Optional<VideoMetadataEntity> findByUuid(UUID uuid);
}
