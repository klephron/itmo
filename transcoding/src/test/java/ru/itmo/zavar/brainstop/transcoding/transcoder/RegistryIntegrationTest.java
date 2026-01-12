package ru.itmo.zavar.brainstop.transcoding.transcoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@FR({"FR-3.1"})
class RegistryIntegrationTest {

    @Autowired
    private TranscodingJobRegistry jobRegistry;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-01: Registry сохраняет и извлекает статус")
    void registry_savesAndRetrievesStatus() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "video.mp4", 1L
        );

        jobRegistry.put(jobId, status);
        Optional<TranscodingJobStatus> retrieved = jobRegistry.get(jobId);

        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getPercent()).isEqualTo(50);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-02: Registry обновляет статус")
    void registry_updatesStatus() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "video.mp4", 1L
        );

        jobRegistry.put(jobId, status);
        jobRegistry.update(jobId, st -> {
            st.setPercent(75);
            st.setMessage("Almost done");
        });

        Optional<TranscodingJobStatus> retrieved = jobRegistry.get(jobId);
        assertThat(retrieved.get().getPercent()).isEqualTo(75);
        assertThat(retrieved.get().getMessage()).isEqualTo("Almost done");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-03: Registry потокобезопасен")
    void registry_isThreadSafe() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        UUID jobId = UUID.randomUUID();

        jobRegistry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 0, "Starting", "video.mp4", 1L
        ));

        for (int i = 0; i < threadCount; i++) {
            final int percent = (i + 1) * 10;
            new Thread(() -> {
                jobRegistry.update(jobId, st -> st.setPercent(percent));
                latch.countDown();
            }).start();
        }

        latch.await(5, TimeUnit.SECONDS);

        Optional<TranscodingJobStatus> result = jobRegistry.get(jobId);
        assertThat(result).isPresent();
        assertThat(result.get().getPercent()).isBetween(10, 100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-04: Registry обрабатывает множественные джобы")
    void registry_handlesMultipleJobs() {
        UUID job1 = UUID.randomUUID();
        UUID job2 = UUID.randomUUID();
        UUID job3 = UUID.randomUUID();

        jobRegistry.put(job1, new TranscodingJobStatus(TranscodingJobStage.TRANSCODING, 33, "1", "v1.mp4", 1L));
        jobRegistry.put(job2, new TranscodingJobStatus(TranscodingJobStage.TRANSCODING, 66, "2", "v2.mp4", 2L));
        jobRegistry.put(job3, new TranscodingJobStatus(TranscodingJobStage.DONE, 100, "3", "v3.mp4", 3L));

        assertThat(jobRegistry.get(job1).get().getPercent()).isEqualTo(33);
        assertThat(jobRegistry.get(job2).get().getPercent()).isEqualTo(66);
        assertThat(jobRegistry.get(job3).get().getPercent()).isEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-05: Registry возвращает empty для несуществующих джобов")
    void registry_returnsEmptyForNonExistent() {
        Optional<TranscodingJobStatus> result = jobRegistry.get(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-06: Registry update не падает для несуществующих джобов")
    void registry_updateDoesNotFailForNonExistent() {
        UUID nonExistentJob = UUID.randomUUID();

        jobRegistry.update(nonExistentJob, st -> st.setPercent(100));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-07: Registry переписывает существующий статус")
    void registry_overwritesExistingStatus() {
        UUID jobId = UUID.randomUUID();

        jobRegistry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Old", "video.mp4", 1L
        ));
        jobRegistry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.DONE, 100, "New", "video.mp4", 1L
        ));

        assertThat(jobRegistry.get(jobId).get().getMessage()).isEqualTo("New");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-08: Registry update изменяет stage")
    void registry_updateChangesStage() {
        UUID jobId = UUID.randomUUID();
        jobRegistry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "video.mp4", 1L
        ));

        jobRegistry.update(jobId, st -> st.setStage(TranscodingJobStage.DONE));

        assertThat(jobRegistry.get(jobId).get().getStage()).isEqualTo(TranscodingJobStage.DONE);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-09: Registry сохраняет все поля")
    void registry_preservesAllFields() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 75, "Almost done", "my-video.mp4", 999L
        );

        jobRegistry.put(jobId, status);
        TranscodingJobStatus retrieved = jobRegistry.get(jobId).get();

        assertThat(retrieved.getStage()).isEqualTo(TranscodingJobStage.TRANSCODING);
        assertThat(retrieved.getPercent()).isEqualTo(75);
        assertThat(retrieved.getMessage()).isEqualTo("Almost done");
        assertThat(retrieved.getUserId()).isEqualTo(999L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-REG-10: Registry update с несколькими полями")
    void registry_updateMultipleFields() {
        UUID jobId = UUID.randomUUID();
        jobRegistry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "video.mp4", 1L
        ));

        jobRegistry.update(jobId, st -> {
            st.setStage(TranscodingJobStage.UPLOADING_TO_MINIO);
            st.setPercent(90);
            st.setMessage("Uploading");
        });

        TranscodingJobStatus retrieved = jobRegistry.get(jobId).get();
        assertThat(retrieved.getStage()).isEqualTo(TranscodingJobStage.UPLOADING_TO_MINIO);
        assertThat(retrieved.getPercent()).isEqualTo(90);
        assertThat(retrieved.getMessage()).isEqualTo("Uploading");
    }
}
