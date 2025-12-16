package ru.itmo.zavar.brainstop.transcoding.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingResult;
import ru.itmo.zavar.brainstop.transcoding.config.TranscodingRabbitConfig;
import ru.itmo.zavar.brainstop.video.streaming.service.VideoStreamingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodingResultListener {

    private final VideoStreamingService streamingService;

    @RabbitListener(queues = TranscodingRabbitConfig.QUEUE_RESULT)
    public void handleResult(TranscodingResult result) {
        if (result == null || result.jobId() == null) {
            return;
        }
        log.info("Received transcoding result for job {}", result.jobId());
        streamingService.handleTranscodingResult(result);
    }
}
