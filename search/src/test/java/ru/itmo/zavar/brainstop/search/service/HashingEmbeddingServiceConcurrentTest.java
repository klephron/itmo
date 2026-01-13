package ru.itmo.zavar.brainstop.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.embedding.HashingEmbeddingService;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-5.1", "NFR-1.2"})
@Layer(TestLayer.BE_UNIT)
class HashingEmbeddingServiceConcurrentTest {

    private HashingEmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new HashingEmbeddingService();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-CONC-01: Потокобезопасность")
    void embed_threadSafe() throws InterruptedException {
        int threadCount = 10;
        int iterationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        float[][] results = new float[threadCount][];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    results[index] = service.embed("test query");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        float[] first = results[0];
        for (int i = 1; i < threadCount; i++) {
            assertThat(results[i]).isEqualTo(first);
        }
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-CONC-02: Параллельные вызовы с разными запросами")
    void embed_parallelDifferentQueries() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        String[] queries = {"query1", "query2", "query3", "query4", "query5"};
        float[][] results = new float[5][];

        for (int i = 0; i < 5; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    results[index] = service.embed(queries[index]);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        for (int i = 0; i < 5; i++) {
            assertThat(results[i]).isNotNull();
            assertThat(results[i]).hasSize(384);
        }
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-CONC-03: Высокая нагрузка")
    void embed_highLoad() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(50);
        CountDownLatch latch = new CountDownLatch(1000);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                try {
                    service.embed("test");
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
    }
}
