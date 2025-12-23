package ru.itmo.zavar.brainstop.transcoding.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class TranscodingKafkaConfig {
    public static final String TOPIC_COMMAND = "transcoding.command";
    public static final String TOPIC_PROGRESS = "transcoding.progress";
    public static final String TOPIC_RESULT = "transcoding.result";

    @Bean
    public NewTopic transcodingCommandTopic() {
        // multiple partitions allow several transcoders to share the workload
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
}
