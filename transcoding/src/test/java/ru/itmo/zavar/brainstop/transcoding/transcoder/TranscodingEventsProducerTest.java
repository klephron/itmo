package ru.itmo.zavar.brainstop.transcoding.transcoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-3.1", "FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class TranscodingEventsProducerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private TranscodingEventsProducer producer;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PROD-01: sendProgress отправляет событие в правильный топик")
    void sendProgress_sendsToCorrectTopic() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 50, "Processing", TranscodingJobStage.TRANSCODING, Instant.now()
        );

        producer.sendProgress(event);

        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_PROGRESS), eq(event));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PROD-02: sendResult отправляет результат в правильный топик")
    void sendResult_sendsToCorrectTopic() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(),
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb-key",
                720
        );

        producer.sendResult(result);

        verify(kafkaTemplate).send(eq(KafkaConfig.TOPIC_RESULT), eq(result));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PROD-03: sendProgress с разными процентами")
    void sendProgress_withDifferentPercents() {
        UUID jobId = UUID.randomUUID();

        for (int i = 0; i <= 100; i += 10) {
            TranscodingProgressEvent event = new TranscodingProgressEvent(
                    jobId, i, "Progress " + i, TranscodingJobStage.TRANSCODING, Instant.now()
            );
            producer.sendProgress(event);
        }

        verify(kafkaTemplate, times(11)).send(eq(KafkaConfig.TOPIC_PROGRESS), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PROD-04: sendResult с множественными рендициями")
    void sendResult_withMultipleRenditions() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(),
                List.of(
                        new TranscodedRenditionMetadata(360, "key1", 500L, "video/mp4"),
                        new TranscodedRenditionMetadata(720, "key2", 1000L, "video/mp4"),
                        new TranscodedRenditionMetadata(1080, "key3", 2000L, "video/mp4")
                ),
                "thumb",
                1080
        );

        producer.sendResult(result);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_RESULT, result);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PROD-05: sendProgress с разными стадиями")
    void sendProgress_withDifferentStages() {
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
    @DisplayName("BE-UNIT-TRANS-PROD-06: sendResult без thumbnail")
    void sendResult_withoutThumbnail() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(),
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                null,
                720
        );

        producer.sendResult(result);

        verify(kafkaTemplate).send(KafkaConfig.TOPIC_RESULT, result);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PROD-07: sendProgress отправляет события последовательно")
    void sendProgress_sendsEventsSequentially() {
        UUID jobId = UUID.randomUUID();

        TranscodingProgressEvent event1 = new TranscodingProgressEvent(jobId, 25, "25%", TranscodingJobStage.TRANSCODING, Instant.now());
        TranscodingProgressEvent event2 = new TranscodingProgressEvent(jobId, 50, "50%", TranscodingJobStage.TRANSCODING, Instant.now());
        TranscodingProgressEvent event3 = new TranscodingProgressEvent(jobId, 75, "75%", TranscodingJobStage.TRANSCODING, Instant.now());

        producer.sendProgress(event1);
        producer.sendProgress(event2);
        producer.sendProgress(event3);

        verify(kafkaTemplate, times(3)).send(eq(KafkaConfig.TOPIC_PROGRESS), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PROD-08: sendResult с разными default heights")
    void sendResult_withDifferentDefaultHeights() {
        for (int height : List.of(360, 480, 720, 1080)) {
            TranscodingResult result = new TranscodingResult(
                    UUID.randomUUID(),
                    List.of(new TranscodedRenditionMetadata(height, "key", 1000L, "video/mp4")),
                    "thumb",
                    height
            );
            producer.sendResult(result);
        }

        verify(kafkaTemplate, times(4)).send(eq(KafkaConfig.TOPIC_RESULT), any());
    }
}
