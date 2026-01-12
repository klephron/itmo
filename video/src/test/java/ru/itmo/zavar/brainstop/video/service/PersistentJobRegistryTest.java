package ru.itmo.zavar.brainstop.video.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.transcoding.model.UploadJobEntity;
import ru.itmo.zavar.brainstop.video.transcoding.repo.UploadJobRepository;
import ru.itmo.zavar.brainstop.video.transcoding.service.PersistentJobRegistry;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-3.1"})
@Layer(TestLayer.BE_UNIT)
class PersistentJobRegistryTest {

    @Mock
    private UploadJobRepository repository;

    @InjectMocks

    private PersistentJobRegistry jobRegistry;

    private UUID testJobId;
    private UploadJobEntity testEntity;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();
        testEntity = new UploadJobEntity();
        testEntity.setId(testJobId);
        testEntity.setStage(TranscodingJobStage.TRANSCODING);
        testEntity.setPercent(50);
        testEntity.setMessage("Processing");
        testEntity.setVideoName("test.mp4");
        testEntity.setUserId(1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-01: put сохраняет новый job")
    void put_savesNewJob() {
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "test.mp4", 1L
        );
        when(repository.findById(testJobId)).thenReturn(Optional.empty());

        jobRegistry.put(testJobId, status);

        verify(repository).save(argThat(entity ->
                entity.getId().equals(testJobId) &&
                        entity.getStage() == TranscodingJobStage.TRANSCODING &&
                        entity.getPercent() == 50
        ));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-02: put обновляет существующий job")
    void put_updatesExistingJob() {
        when(repository.findById(testJobId)).thenReturn(Optional.of(testEntity));

        TranscodingJobStatus newStatus = new TranscodingJobStatus(
                TranscodingJobStage.DONE, 100, "Complete", "test.mp4", 1L
        );

        jobRegistry.put(testJobId, newStatus);

        verify(repository).save(argThat(entity ->
                entity.getStage() == TranscodingJobStage.DONE &&
                        entity.getPercent() == 100
        ));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-03: get возвращает статус")
    void get_returnsStatus() {
        when(repository.findById(testJobId)).thenReturn(Optional.of(testEntity));

        Optional<TranscodingJobStatus> result = jobRegistry.get(testJobId);

        assertThat(result).isPresent();
        assertThat(result.get().getStage()).isEqualTo(TranscodingJobStage.TRANSCODING);
        assertThat(result.get().getPercent()).isEqualTo(50);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-REGISTRY-04: get возвращает empty для несуществующего job")
    void get_returnsEmptyForNonExistent() {
        when(repository.findById(testJobId)).thenReturn(Optional.empty());

        Optional<TranscodingJobStatus> result = jobRegistry.get(testJobId);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-05: update обновляет job")
    void update_updatesJob() {
        when(repository.findById(testJobId)).thenReturn(Optional.of(testEntity));

        jobRegistry.update(testJobId, status -> {
            status.setPercent(75);
            status.setMessage("Almost done");
        });

        verify(repository).save(argThat(entity ->
                entity.getPercent() == 75 &&
                        entity.getMessage().equals("Almost done")
        ));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-06: update не делает ничего если job не найден")
    void update_doesNothingIfNotFound() {
        when(repository.findById(testJobId)).thenReturn(Optional.empty());

        jobRegistry.update(testJobId, status -> status.setPercent(100));

        verify(repository, never()).save(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-07: getActiveJobsForUser возвращает активные джобы")
    void getActiveJobsForUser_returnsActiveJobs() {
        UploadJobEntity job1 = new UploadJobEntity();
        job1.setId(UUID.randomUUID());
        job1.setStage(TranscodingJobStage.TRANSCODING);
        job1.setUserId(1L);
        job1.setVideoName("video1.mp4");
        job1.setPercent(30);

        UploadJobEntity job2 = new UploadJobEntity();
        job2.setId(UUID.randomUUID());
        job2.setStage(TranscodingJobStage.UPLOADING);
        job2.setUserId(1L);
        job2.setVideoName("video2.mp4");
        job2.setPercent(10);

        UploadJobEntity job3 = new UploadJobEntity();
        job3.setId(UUID.randomUUID());
        job3.setStage(TranscodingJobStage.DONE);
        job3.setUserId(1L);

        when(repository.findByUserId(1L)).thenReturn(List.of(job1, job2, job3));

        Map<UUID, TranscodingJobStatus> result = jobRegistry.getActiveJobsForUser(1L);

        assertThat(result).hasSize(2);
        assertThat(result).containsKeys(job1.getId(), job2.getId());
        assertThat(result).doesNotContainKey(job3.getId());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-08: getActiveJobsForUser исключает ERROR джобы")
    void getActiveJobsForUser_excludesErrorJobs() {
        UploadJobEntity errorJob = new UploadJobEntity();
        errorJob.setId(UUID.randomUUID());
        errorJob.setStage(TranscodingJobStage.ERROR);
        errorJob.setUserId(1L);

        when(repository.findByUserId(1L)).thenReturn(List.of(errorJob));

        Map<UUID, TranscodingJobStatus> result = jobRegistry.getActiveJobsForUser(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-09: put устанавливает createdAt для нового job")
    void put_setsCreatedAtForNewJob() {
        when(repository.findById(testJobId)).thenReturn(Optional.empty());

        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 0, "Start", "video.mp4", 1L
        );

        jobRegistry.put(testJobId, status);

        verify(repository).save(argThat(entity -> entity.getCreatedAt() != null));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REGISTRY-10: update устанавливает updatedAt")
    void update_setsUpdatedAt() {
        when(repository.findById(testJobId)).thenReturn(Optional.of(testEntity));

        jobRegistry.update(testJobId, status -> status.setPercent(60));

        verify(repository).save(argThat(entity -> entity.getUpdatedAt() != null));
    }
}
