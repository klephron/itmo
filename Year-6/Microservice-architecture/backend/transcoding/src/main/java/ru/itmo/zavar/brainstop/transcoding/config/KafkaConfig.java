package ru.itmo.zavar.brainstop.transcoding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConfig {
    public static final String TOPIC_COMMAND = "transcoding.command";
    public static final String TOPIC_PROGRESS = "transcoding.progress";
    public static final String TOPIC_RESULT = "transcoding.result";

    @Bean
    public NewTopic transcodingCommandTopic() {
        // multiple partitions allow multiple transcoders to share commands
        return TopicBuilder.name(TOPIC_COMMAND).partitions(6).build();
    }

    @Bean
    public NewTopic transcodingProgressTopic() {
        return TopicBuilder.name(TOPIC_PROGRESS).partitions(6).build();
    }

    @Bean
    public NewTopic transcodingResultTopic() {
        return TopicBuilder.name(TOPIC_RESULT).partitions(6).build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // Retry the same record indefinitely with backoff; do not commit on failure
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(5000L, FixedBackOff.UNLIMITED_ATTEMPTS)));
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
