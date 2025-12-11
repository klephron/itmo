package ru.itmo.zavar.ticktokcoretest.like.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.common.api.rabbit.VideoLikeEvent;
import ru.itmo.zavar.ticktokcoretest.like.model.VideoLikeEntity;
import ru.itmo.zavar.ticktokcoretest.like.repo.VideoLikeRepository;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.ticktokcoretest.common.api.video.VideoMetadataSummary;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
// MSPLIT: boundary for future Like Service logic
@RequiredArgsConstructor
public class LikeService {

    private final VideoLikeRepository videoLikeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange likesExchange;
    private final VideoMetadataPublicApi videoMetadataPublicApi;

    @Transactional
    public long likeVideo(UUID videoUuid, Long userId) {
        VideoMetadataSummary video = this.videoMetadataPublicApi.getRequired(videoUuid);

        if (this.videoLikeRepository.existsByVideoIdAndUserId(video.id(), userId)) {
            return video.likesCount();
        }

        VideoLikeEntity like = new VideoLikeEntity();
        like.setVideoId(video.id());
        like.setUserId(userId);
        this.videoLikeRepository.save(like);

        long newCount = video.likesCount() + 1;
        this.videoMetadataPublicApi.updateLikesCount(video.id(), newCount);

        this.publishEvent("VIDEO_LIKED", video, userId, newCount);

        return newCount;
    }

    @Transactional
    public long unlikeVideo(UUID videoUuid, Long userId) {
        VideoMetadataSummary video = this.videoMetadataPublicApi.getRequired(videoUuid);

        Optional<VideoLikeEntity> existing = this.videoLikeRepository.findByVideoIdAndUserId(video.id(), userId);
        if (existing.isEmpty()) {
            return video.likesCount();
        }

        this.videoLikeRepository.delete(existing.get());

        long newCount = Math.max(0, video.likesCount() - 1);
        this.videoMetadataPublicApi.updateLikesCount(video.id(), newCount);

        this.publishEvent("VIDEO_UNLIKED", video, userId, newCount);

        return newCount;
    }

    public boolean isLikedByUser(UUID videoUuid, Long userId) {
        return this.videoMetadataPublicApi.findByUuid(videoUuid)
                .map(v -> this.videoLikeRepository.existsByVideoIdAndUserId(v.id(), userId))
                .orElse(false);
    }

    private void publishEvent(String type, VideoMetadataSummary video, Long userId, Long likesCount) {
        VideoLikeEvent event = new VideoLikeEvent(
                type,
                userId,
                video.uuid(),
                video.id(),
                likesCount,
                Instant.now()
        );
        String routingKey = type.equals("VIDEO_LIKED") ? "video.liked" : "video.unliked";
        this.rabbitTemplate.convertAndSend(this.likesExchange.getName(), routingKey, event);
    }
}

