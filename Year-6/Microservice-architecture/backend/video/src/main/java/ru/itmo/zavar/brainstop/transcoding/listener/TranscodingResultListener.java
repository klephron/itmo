package ru.itmo.zavar.brainstop.transcoding.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingResult;
import ru.itmo.zavar.brainstop.transcoding.config.TranscodingKafkaConfig;
import ru.itmo.zavar.brainstop.video.streaming.service.VideoStreamingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscodingResultListener {

    private final VideoStreamingService streamingService;

    @KafkaListener(topics = TranscodingKafkaConfig.TOPIC_RESULT, groupId = "video-service")
    public void handleResult(TranscodingResult result) {
        if (result == null || result.jobId() == null) {
            return;
        }
        log.info("Received transcoding result for job {}", result.jobId());
        streamingService.handleTranscodingResult(result);
    }
}
