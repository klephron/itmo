package ru.itmo.zavar.brainstop.transcoding.transcoder;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.config.KafkaConfig;
import ru.itmo.zavar.brainstop.transcoding.messaging.TranscodingEventsProducer;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test-kafka")
@EmbeddedKafka(
        partitions = 6,
        topics = {KafkaConfig.TOPIC_COMMAND, KafkaConfig.TOPIC_PROGRESS, KafkaConfig.TOPIC_RESULT}
)
@DirtiesContext
@FR({"FR-3.1", "FR-3.2"})
class KafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private TranscodingEventsProducer eventsProducer;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockBean
    private VideoObjectStorage videoObjectStorage;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-KAFKA-01: Producer отправляет progress событие")
    void producer_sendsProgressEvent() throws Exception {
        BlockingQueue<ConsumerRecord<String, TranscodingProgressEvent>> records = new LinkedBlockingQueue<>();

        KafkaMessageListenerContainer<String, TranscodingProgressEvent> container = createContainer(
                KafkaConfig.TOPIC_PROGRESS,
                TranscodingProgressEvent.class,
                records
        );

        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 50, "Processing", TranscodingJobStage.TRANSCODING, Instant.now()
        );

        eventsProducer.sendProgress(event);

        ConsumerRecord<String, TranscodingProgressEvent> received = records.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.value().percent()).isEqualTo(50);
        assertThat(received.value().stage()).isEqualTo(TranscodingJobStage.TRANSCODING);

        container.stop();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-KAFKA-02: Producer отправляет result событие")
    void producer_sendsResultEvent() throws Exception {
        BlockingQueue<ConsumerRecord<String, TranscodingResult>> records = new LinkedBlockingQueue<>();

        KafkaMessageListenerContainer<String, TranscodingResult> container = createContainer(
                KafkaConfig.TOPIC_RESULT,
                TranscodingResult.class,
                records
        );

        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(),
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                "thumb-key",
                720
        );

        eventsProducer.sendResult(result);

        ConsumerRecord<String, TranscodingResult> received = records.poll(10, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        assertThat(received.value().renditions()).hasSize(3);
        assertThat(received.value().thumbnailKey()).isEqualTo("thumb");

        container.stop();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-KAFKA-03: Command отправляется и обрабатывается")
    void command_sentAndProcessed() throws Exception {
        when(videoObjectStorage.downloadProcessed(anyString()))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));

        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "test-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        kafkaTemplate.send(KafkaConfig.TOPIC_COMMAND, command);

        Thread.sleep(2000);

    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-KAFKA-04: Множественные progress события в правильном порядке")
    void multipleProgressEvents_inCorrectOrder() throws Exception {
        BlockingQueue<ConsumerRecord<String, TranscodingProgressEvent>> records = new LinkedBlockingQueue<>();

        KafkaMessageListenerContainer<String, TranscodingProgressEvent> container = createContainer(
                KafkaConfig.TOPIC_PROGRESS,
                TranscodingProgressEvent.class,
                records
        );

        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

        UUID jobId = UUID.randomUUID();
        eventsProducer.sendProgress(new TranscodingProgressEvent(
                jobId, 25, "25%", TranscodingJobStage.TRANSCODING, Instant.now()
        ));
        eventsProducer.sendProgress(new TranscodingProgressEvent(
                jobId, 50, "50%", TranscodingJobStage.TRANSCODING, Instant.now()
        ));
        eventsProducer.sendProgress(new TranscodingProgressEvent(
                jobId, 75, "75%", TranscodingJobStage.TRANSCODING, Instant.now()
        ));

        ConsumerRecord<String, TranscodingProgressEvent> rec1 = records.poll(10, TimeUnit.SECONDS);
        ConsumerRecord<String, TranscodingProgressEvent> rec2 = records.poll(10, TimeUnit.SECONDS);

        assertThat(rec1.value().percent()).isEqualTo(50);
        assertThat(rec2.value().percent()).isEqualTo(25);

        container.stop();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-INT-KAFKA-05: Result с множественными рендициями")
    void result_withMultipleRenditions() throws Exception {
        BlockingQueue<ConsumerRecord<String, TranscodingResult>> records = new LinkedBlockingQueue<>();

        KafkaMessageListenerContainer<String, TranscodingResult> container = createContainer(
                KafkaConfig.TOPIC_RESULT,
                TranscodingResult.class,
                records
        );

        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());

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

        eventsProducer.sendResult(result);

        ConsumerRecord<String, TranscodingResult> received = records.poll(10, TimeUnit.SECONDS);
        assertThat(received.value().renditions()).hasSize(3);
        assertThat(received.value().defaultHeight()).isEqualTo(1080);

        container.stop();
    }

    private <T> KafkaMessageListenerContainer<String, T> createContainer(
            String topic,
            Class<T> valueType,
            BlockingQueue<ConsumerRecord<String, T>> records
    ) {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + System.currentTimeMillis());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "ru.itmo.zavar.brainstop.*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, valueType.getName());

        DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps);
        ContainerProperties containerProperties = new ContainerProperties(topic);

        KafkaMessageListenerContainer<String, T> container = new KafkaMessageListenerContainer<>(
                consumerFactory, containerProperties
        );

        container.setupMessageListener((MessageListener<String, T>) record -> records.add(record));

        return container;
    }
}