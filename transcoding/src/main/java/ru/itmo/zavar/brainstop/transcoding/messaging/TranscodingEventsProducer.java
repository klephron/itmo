package ru.itmo.zavar.brainstop.transcoding.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingProgressEvent;

@Component
@RequiredArgsConstructor
public class TranscodingEventsProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendProgress(TranscodingProgressEvent event) {
        rabbitTemplate.convertAndSend("transcoding.exchange", "transcoding.progress", event);
    }
}
