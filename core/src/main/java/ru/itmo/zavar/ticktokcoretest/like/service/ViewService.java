package ru.itmo.zavar.ticktokcoretest.like.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.rabbit.VideoViewEvent;
import ru.itmo.zavar.ticktokcoretest.like.model.VideoViewEntity;
import ru.itmo.zavar.ticktokcoretest.like.repo.VideoViewRepository;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoMetadataSummary;

import java.time.Instant;
import java.util.UUID;

@Service
// MSPLIT: boundary for future Metrics service logic
@RequiredArgsConstructor
public class ViewService {

    private final VideoViewRepository videoViewRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange viewsExchange;
    private final VideoMetadataPublicApi videoMetadataPublicApi;

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

        long newCount = video.viewsCount() + 1;
        this.videoMetadataPublicApi.updateViewsCount(video.id(), newCount);

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
        this.rabbitTemplate.convertAndSend(this.viewsExchange.getName(), "video.viewed", event);
    }
}
