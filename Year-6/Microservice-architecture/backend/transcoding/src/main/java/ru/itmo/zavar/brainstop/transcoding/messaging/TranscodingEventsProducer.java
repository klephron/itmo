package ru.itmo.zavar.brainstop.transcoding.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingProgressEvent;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingResult;
import ru.itmo.zavar.brainstop.transcoding.config.KafkaConfig;

@Component
@RequiredArgsConstructor
public class TranscodingEventsProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendProgress(TranscodingProgressEvent event) {
        kafkaTemplate.send(KafkaConfig.TOPIC_PROGRESS, event);
    }

    public void sendResult(TranscodingResult result) {
        kafkaTemplate.send(KafkaConfig.TOPIC_RESULT, result);
    }
}
