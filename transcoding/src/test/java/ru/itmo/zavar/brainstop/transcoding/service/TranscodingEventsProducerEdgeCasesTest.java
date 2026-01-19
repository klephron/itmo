package ru.itmo.zavar.brainstop.transcoding.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.config.KafkaConfig;
import ru.itmo.zavar.brainstop.transcoding.messaging.TranscodingEventsProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class TranscodingEventsProducerEdgeCasesTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private TranscodingEventsProducer producer;

    @BeforeEach
    void setUp() {
        producer = new TranscodingEventsProducer(kafkaTemplate);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-01: sendProgress с 0% прогресса")
    void sendProgress_withZeroProgress() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 0, "Starting", TranscodingJobStage.UPLOADING, Instant.now()
        );

        producer.sendProgress(event);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_PROGRESS, event);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-02: sendProgress с 100% прогресса")
    void sendProgress_with100Progress() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 100, "Done", TranscodingJobStage.DONE, Instant.now()
        );

        producer.sendProgress(event);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_PROGRESS, event);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-03: sendProgress с отрицательным процентом")
    void sendProgress_withNegativePercent() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), -10, "Error", TranscodingJobStage.ERROR, Instant.now()
        );

        producer.sendProgress(event);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_PROGRESS, event);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-04: sendProgress с процентом > 100")
    void sendProgress_withPercentOver100() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 150, "Overflow", TranscodingJobStage.TRANSCODING, Instant.now()
        );

        producer.sendProgress(event);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_PROGRESS, event);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-05: sendProgress с очень длинным сообщением")
    void sendProgress_withVeryLongMessage() {
        String longMsg = "msg".repeat(10000);
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 50, longMsg, TranscodingJobStage.TRANSCODING, Instant.now()
        );

        producer.sendProgress(event);

        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_PROGRESS), eq(event));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-06: sendProgress с null message")
    void sendProgress_withNullMessage() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 50, null, TranscodingJobStage.TRANSCODING, Instant.now()
        );

        producer.sendProgress(event);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_PROGRESS, event);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-07: sendProgress с пустым message")
    void sendProgress_withEmptyMessage() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 50, "", TranscodingJobStage.TRANSCODING, Instant.now()
        );

        producer.sendProgress(event);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_PROGRESS, event);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-08: sendResult с одной рендицией")
    void sendResult_withSingleRendition() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(),
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb",
                720
        );

        producer.sendResult(result);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_RESULT, result);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-09: sendResult с 10+ рендициями")
    void sendResult_withManyRenditions() {
        List<TranscodedRenditionMetadata> renditions = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            renditions.add(new TranscodedRenditionMetadata(
                    i * 100, "key" + i, (long) (i * 1000), "video/mp4"
            ));
        }

        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(), renditions, "thumb", 1080
        );

        producer.sendResult(result);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_RESULT, result);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-10: sendResult без рендиций")
    void sendResult_withoutRenditions() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(), List.of(), null, 720
        );

        producer.sendResult(result);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_RESULT, result);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-11: sendProgress все стадии")
    void sendProgress_allStages() {
        UUID jobId = UUID.randomUUID();

        for (TranscodingJobStage stage : TranscodingJobStage.values()) {
            TranscodingProgressEvent event = new TranscodingProgressEvent(
                    jobId, 50, stage.name(), stage, Instant.now()
            );
            producer.sendProgress(event);
        }

        verify(kafkaTemplate, times(TranscodingJobStage.values().length))
                .send(eq(KafkaConfig.TOPIC_PROGRESS), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-12: sendResult с рендициями разных размеров")
    void sendResult_withRenditionsOfDifferentSizes() {
        List<TranscodedRenditionMetadata> renditions = List.of(
                new TranscodedRenditionMetadata(360, "key1", 500L, "video/mp4"),
                new TranscodedRenditionMetadata(720, "key2", 2000L, "video/mp4"),
                new TranscodedRenditionMetadata(1080, "key3", 5000L, "video/mp4"),
                new TranscodedRenditionMetadata(2160, "key4", 15000L, "video/mp4")
        );

        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(), renditions, "thumb", 1080
        );

        producer.sendResult(result);

        ArgumentCaptor<TranscodingResult> captor = ArgumentCaptor.forClass(TranscodingResult.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_RESULT), captor.capture());

        assertThat(captor.getValue().renditions()).hasSize(4);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-13: sendProgress быстрая последовательность")
    void sendProgress_rapidSequence() {
        UUID jobId = UUID.randomUUID();

        for (int i = 0; i <= 100; i++) {
            producer.sendProgress(new TranscodingProgressEvent(
                    jobId, i, "Progress " + i, TranscodingJobStage.TRANSCODING, Instant.now()
            ));
        }

        verify(kafkaTemplate, times(101)).send(eq(KafkaConfig.TOPIC_PROGRESS), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-14: sendResult с defaultHeight = 0")
    void sendResult_withZeroDefaultHeight() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(),
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb",
                0
        );

        producer.sendResult(result);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_RESULT, result);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-PROD-EDGE-15: sendResult с очень большим defaultHeight")
    void sendResult_withVeryLargeDefaultHeight() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(),
                List.of(new TranscodedRenditionMetadata(8192, "key", 1000L, "video/mp4")),
                "thumb",
                8192
        );

        producer.sendResult(result);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_RESULT, result);
    }
}
