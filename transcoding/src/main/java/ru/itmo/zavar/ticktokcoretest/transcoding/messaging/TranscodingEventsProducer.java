package ru.itmo.zavar.ticktokcoretest.transcoding.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.ticktokcoretest.common.api.transcoding.TranscodingProgressEvent;

@Component
@RequiredArgsConstructor
public class TranscodingEventsProducer {
    private final RabbitTemplate rabbitTemplate;

    public void sendProgress(TranscodingProgressEvent event) {
        rabbitTemplate.convertAndSend("transcoding.exchange", "transcoding.progress", event);
    }
}
