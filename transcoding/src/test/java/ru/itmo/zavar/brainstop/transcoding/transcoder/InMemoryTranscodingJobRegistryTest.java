package ru.itmo.zavar.brainstop.transcoding.transcoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.registry.InMemoryTranscodingJobRegistry;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-3.1"})
@Layer(TestLayer.BE_UNIT)
class InMemoryTranscodingJobRegistryTest {

    private InMemoryTranscodingJobRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryTranscodingJobRegistry();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-REG-01: put сохраняет статус джоба")
    void put_savesJobStatus() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "video.mp4", 1L
        );

        registry.put(jobId, status);

        Optional<TranscodingJobStatus> result = registry.get(jobId);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(status);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-REG-02: get возвращает empty для несуществующего джоба")
    void get_returnsEmptyForNonExistentJob() {
        Optional<TranscodingJobStatus> result = registry.get(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-REG-04: update не падает для несуществующего джоба")
    void update_doesNotFailForNonExistentJob() {
        registry.update(UUID.randomUUID(), st -> st.setPercent(100));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-REG-06: поддерживает множественные джобы")
    void supportsMultipleJobs() {
        UUID job1 = UUID.randomUUID();
        UUID job2 = UUID.randomUUID();
        UUID job3 = UUID.randomUUID();

        registry.put(job1, new TranscodingJobStatus(TranscodingJobStage.TRANSCODING, 33, "1", "v1.mp4", 1L));
        registry.put(job2, new TranscodingJobStatus(TranscodingJobStage.TRANSCODING, 66, "2", "v2.mp4", 2L));
        registry.put(job3, new TranscodingJobStatus(TranscodingJobStage.DONE, 100, "3", "v3.mp4", 3L));

        assertThat(registry.get(job1)).isPresent();
        assertThat(registry.get(job2)).isPresent();
        assertThat(registry.get(job3)).isPresent();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-REG-07: update изменяет только указанные поля")
    void update_changesOnlySpecifiedFields() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "video.mp4", 1L
        );
        registry.put(jobId, status);

        registry.update(jobId, st -> st.setPercent(75));

        Optional<TranscodingJobStatus> result = registry.get(jobId);
        assertThat(result.get().getMessage()).isEqualTo("Processing");
        assertThat(result.get().getStage()).isEqualTo(TranscodingJobStage.TRANSCODING);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-REG-08: getActiveJobsForUser возвращает пустую карту")
    void getActiveJobsForUser_returnsEmptyMap() {
        assertThat(registry.getActiveJobsForUser(1L)).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-REG-09: потокобезопасность для конкурентных put")
    void threadSafetyForConcurrentPuts() throws InterruptedException {
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                UUID jobId = UUID.randomUUID();
                registry.put(jobId, new TranscodingJobStatus(
                        TranscodingJobStage.TRANSCODING, index * 10, "Thread " + index, "v.mp4", (long) index
                ));
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-REG-10: update может изменить stage")
    void update_canChangeStage() {
        UUID jobId = UUID.randomUUID();
        registry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "In progress", "v.mp4", 1L
        ));

        registry.update(jobId, st -> st.setStage(TranscodingJobStage.DONE));

        assertThat(registry.get(jobId).get().getStage()).isEqualTo(TranscodingJobStage.DONE);
    }
}
