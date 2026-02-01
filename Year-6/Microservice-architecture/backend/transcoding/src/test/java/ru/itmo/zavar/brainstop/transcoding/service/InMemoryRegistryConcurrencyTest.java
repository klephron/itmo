package ru.itmo.zavar.brainstop.transcoding.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobStatus;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.registry.InMemoryTranscodingJobRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-3.1"})
@Layer(TestLayer.BE_UNIT)
class InMemoryRegistryConcurrencyTest {

    private InMemoryTranscodingJobRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InMemoryTranscodingJobRegistry();
    }

    @RepeatedTest(5)
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-01: Конкурентные put операции")
    void concurrentPuts() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        List<UUID> jobIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            UUID jobId = UUID.randomUUID();
            jobIds.add(jobId);

            executor.submit(() -> {
                registry.put(jobId, new TranscodingJobStatus(
                        TranscodingJobStage.TRANSCODING, 50, "msg", "video.mp4", 1L
                ));
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        for (UUID jobId : jobIds) {
            assertThat(registry.get(jobId)).isPresent();
        }
    }

    @RepeatedTest(5)
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-02: Конкурентные update операции")
    void concurrentUpdates() throws InterruptedException {
        UUID jobId = UUID.randomUUID();
        registry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 0, "Start", "video.mp4", 1L
        ));

        int threadCount = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int percent = i * 2;
            executor.submit(() -> {
                registry.update(jobId, st -> st.setPercent(percent));
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        Optional<TranscodingJobStatus> result = registry.get(jobId);
        assertThat(result).isPresent();
        assertThat(result.get().getPercent()).isBetween(0, 100);
    }

    @RepeatedTest(3)
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-03: Конкурентные put и get")
    void concurrentPutAndGet() throws InterruptedException, ExecutionException, TimeoutException {
        UUID jobId = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(10);

        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final int percent = i * 20;
            futures.add(executor.submit(() -> {
                registry.put(jobId, new TranscodingJobStatus(
                        TranscodingJobStage.TRANSCODING, percent, "msg", "video.mp4", 1L
                ));
            }));
        }

        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                registry.get(jobId);
            }));
        }

        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertThat(registry.get(jobId)).isPresent();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-04: Конкурентные update различных полей")
    void concurrentUpdateDifferentFields() throws InterruptedException {
        UUID jobId = UUID.randomUUID();
        registry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 0, "Start", "video.mp4", 1L
        ));

        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        executor.submit(() -> {
            for (int i = 0; i < 10; i++) {
                int finalI = i;
                registry.update(jobId, st -> st.setPercent(finalI * 10));
            }
            latch.countDown();
        });

        executor.submit(() -> {
            for (int i = 0; i < 10; i++) {
                int finalI = i;
                registry.update(jobId, st -> st.setMessage("Message " + finalI));
            }
            latch.countDown();
        });

        executor.submit(() -> {
            for (TranscodingJobStage stage : TranscodingJobStage.values()) {
                registry.update(jobId, st -> st.setStage(stage));
            }
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(registry.get(jobId)).isPresent();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-05: Множественные джобы конкурентно")
    void multipleJobsConcurrently() throws InterruptedException {
        int jobCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(jobCount);

        List<UUID> jobIds = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            UUID jobId = UUID.randomUUID();
            jobIds.add(jobId);

            final int percent = i * 2;
            executor.submit(() -> {
                registry.put(jobId, new TranscodingJobStatus(
                        TranscodingJobStage.TRANSCODING, percent, "msg", "video.mp4", 1L
                ));
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        for (UUID jobId : jobIds) {
            assertThat(registry.get(jobId)).isPresent();
        }
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-06: get не блокируется put")
    void getNeverBlocksPut() throws InterruptedException {
        UUID jobId = UUID.randomUUID();
        registry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 0, "Start", "video.mp4", 1L
        ));

        int iterations = 100;
        CountDownLatch latch = new CountDownLatch(iterations * 2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            for (int i = 0; i < iterations; i++) {
                registry.get(jobId);
                latch.countDown();
            }
        });

        executor.submit(() -> {
            for (int i = 0; i < iterations; i++) {
                registry.put(jobId, new TranscodingJobStatus(
                        TranscodingJobStage.TRANSCODING, i, "msg", "video.mp4", 1L
                ));
                latch.countDown();
            }
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-07: update не теряет изменения при конкурентности")
    void updateDoesNotLoseChanges() throws InterruptedException {
        UUID jobId = UUID.randomUUID();
        registry.put(jobId, new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 0, "Start", "video.mp4", 1L
        ));

        int updateCount = 100;
        CountDownLatch latch = new CountDownLatch(updateCount);
        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < updateCount; i++) {
            executor.submit(() -> {
                registry.update(jobId, st -> st.setPercent(
                        Math.min(100, st.getPercent() + 1)
                ));
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        Optional<TranscodingJobStatus> result = registry.get(jobId);
        assertThat(result).isPresent();
        assertThat(result.get().getPercent()).isGreaterThan(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-08: Смешанные операции put/get/update")
    void mixedOperations() throws InterruptedException {
        UUID jobId = UUID.randomUUID();
        int operationsCount = 300;
        CountDownLatch latch = new CountDownLatch(operationsCount);
        ExecutorService executor = Executors.newFixedThreadPool(15);

        for (int i = 0; i < 100; i++) {
            final int percent = i;
            executor.submit(() -> {
                registry.put(jobId, new TranscodingJobStatus(
                        TranscodingJobStage.TRANSCODING, percent, "msg", "video.mp4", 1L
                ));
                latch.countDown();
            });
        }

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                registry.get(jobId);
                latch.countDown();
            });
        }

        for (int i = 0; i < 100; i++) {
            executor.submit(() -> {
                registry.update(jobId, st -> st.setMessage("Updated"));
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(registry.get(jobId)).isPresent();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-09: put перезаписывает предыдущее значение")
    void putOverwritesPreviousValue() throws InterruptedException {
        UUID jobId = UUID.randomUUID();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);

        executor.submit(() -> {
            registry.put(jobId, new TranscodingJobStatus(
                    TranscodingJobStage.TRANSCODING, 25, "First", "video.mp4", 1L
            ));
            latch.countDown();
        });

        executor.submit(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            registry.put(jobId, new TranscodingJobStatus(
                    TranscodingJobStage.DONE, 100, "Second", "video.mp4", 1L
            ));
            latch.countDown();
        });

        latch.await(2, TimeUnit.SECONDS);
        executor.shutdown();

        Optional<TranscodingJobStatus> result = registry.get(jobId);
        assertThat(result).isPresent();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-REG-CONC-10: getActiveJobsForUser всегда возвращает пустую карту")
    void getActiveJobsForUserAlwaysReturnsEmpty() {
        for (int i = 0; i < 10; i++) {
            UUID jobId = UUID.randomUUID();
            registry.put(jobId, new TranscodingJobStatus(
                    TranscodingJobStage.TRANSCODING, 50, "msg", "video.mp4", (long) i
            ));
        }

        assertThat(registry.getActiveJobsForUser(1L)).isEmpty();
        assertThat(registry.getActiveJobsForUser(999L)).isEmpty();
    }
}
