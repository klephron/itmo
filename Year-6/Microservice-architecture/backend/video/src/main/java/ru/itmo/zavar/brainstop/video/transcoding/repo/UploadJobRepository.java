package ru.itmo.zavar.brainstop.video.transcoding.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.zavar.brainstop.video.transcoding.model.UploadJobEntity;

import java.util.List;
import java.util.UUID;
import java.util.Optional;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;

public interface UploadJobRepository extends JpaRepository<UploadJobEntity, UUID> {
    List<UploadJobEntity> findByUserId(Long userId);

    Optional<UploadJobEntity> findFirstByUserIdAndVideoNameAndStageIn(Long userId, String videoName, List<TranscodingJobStage> stages);

    List<UploadJobEntity> findByStageIn(List<TranscodingJobStage> stages);
}
