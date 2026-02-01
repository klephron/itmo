package ru.itmo.zavar.brainstop.like.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.rabbit.VideoViewEvent;
import ru.itmo.zavar.brainstop.like.model.VideoViewEntity;
import ru.itmo.zavar.brainstop.like.repo.VideoViewRepository;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;

import java.time.Instant;
import java.util.UUID;

@Service
// MSPLIT: boundary for future Metrics service logic
@RequiredArgsConstructor
public class ViewService {

    private final VideoViewRepository videoViewRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final VideoMetadataPublicApi videoMetadataPublicApi;

    @Value("${app.views.exchange:video.views}")
    private String viewsTopic;

    @Transactional
    public long viewVideo(UUID videoUuid, Long userId) {
        VideoMetadataSummary video = this.videoMetadataPublicApi.getRequired(videoUuid);

        if (this.videoViewRepository.existsByVideoIdAndUserId(video.id(), userId)) {
            return video.viewsCount();
        }

        VideoViewEntity view = new VideoViewEntity();
        view.setVideoId(video.id());
        view.setUserId(userId);
        this.videoViewRepository.save(view);

        long newCount = this.videoMetadataPublicApi.incrementViewsCount(video.id(), 1);
        if (newCount < 0) {
            newCount = video.viewsCount() + 1;
            this.videoMetadataPublicApi.updateViewsCount(video.id(), newCount);
        }

        this.publishEvent(video, userId, newCount);

        return newCount;
    }

    public long getViews(UUID videoUuid) {
        return this.videoMetadataPublicApi.getRequired(videoUuid).viewsCount();
    }

    private void publishEvent(VideoMetadataSummary video, Long userId, Long viewsCount) {
        VideoViewEvent event = new VideoViewEvent(
                userId,
                video.uuid(),
                video.id(),
                viewsCount,
                Instant.now()
        );
        this.kafkaTemplate.send(viewsTopic, event);
    }
}
