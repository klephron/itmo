package ru.itmo.zavar.brainstop.like.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    @Value("${app.likes.exchange:video.likes}")
    private String likesTopic;

    @Value("${app.views.exchange:video.views}")
    private String viewsTopic;

    @Bean
    public NewTopic likesTopic() {
        return TopicBuilder.name(likesTopic).partitions(1).build();
    }

    @Bean
    public NewTopic viewsTopic() {
        return TopicBuilder.name(viewsTopic).partitions(1).build();
    }
}
