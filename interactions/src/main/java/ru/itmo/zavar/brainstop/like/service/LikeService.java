package ru.itmo.zavar.brainstop.like.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.rabbit.VideoLikeEvent;
import ru.itmo.zavar.brainstop.like.model.VideoLikeEntity;
import ru.itmo.zavar.brainstop.like.repo.VideoLikeRepository;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;

import java.time.Instant;
import java.util.UUID;

@Service
// MSPLIT: boundary for future Like Service logic
@RequiredArgsConstructor
public class LikeService {

    private final VideoLikeRepository videoLikeRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final VideoMetadataPublicApi videoMetadataPublicApi;

    @Value("${app.likes.exchange:video.likes}")
    private String likesTopic;

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

        long newCount = this.videoMetadataPublicApi.incrementLikesCount(video.id(), 1);
        if (newCount < 0) {
            newCount = video.likesCount() + 1;
            this.videoMetadataPublicApi.updateLikesCount(video.id(), newCount);
        }

        this.publishEvent("VIDEO_LIKED", video, userId, newCount);

        return newCount;
    }

    @Transactional
    public long unlikeVideo(UUID videoUuid, Long userId) {
        VideoMetadataSummary video = this.videoMetadataPublicApi.getRequired(videoUuid);

        int deleted = this.videoLikeRepository.deleteByVideoIdAndUserId(video.id(), userId);
        if (deleted <= 0) {
            return video.likesCount();
        }

        long newCount = this.videoMetadataPublicApi.incrementLikesCount(video.id(), -1);
        if (newCount < 0) {
            newCount = Math.max(0, video.likesCount() - 1);
            this.videoMetadataPublicApi.updateLikesCount(video.id(), newCount);
        }

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
        this.kafkaTemplate.send(likesTopic, event);
    }
}

