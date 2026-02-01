package ru.itmo.zavar.brainstop.like.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.rabbit.VideoLikeEvent;
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

        int inserted = this.videoLikeRepository.insertIgnore(video.id(), userId);
        if (inserted <= 0) {
            return safeLikesCount(video);
        }

        long newCount = this.videoMetadataPublicApi.incrementLikesCount(video.id(), 1);
        if (newCount < 0) {
            return safeLikesCount(video);
        }

        this.publishEvent("VIDEO_LIKED", video, userId, newCount);

        return newCount;
    }

    @Transactional
    public long unlikeVideo(UUID videoUuid, Long userId) {
        VideoMetadataSummary video = this.videoMetadataPublicApi.getRequired(videoUuid);

        int deleted = this.videoLikeRepository.deleteByVideoIdAndUserId(video.id(), userId);
        if (deleted <= 0) {
            return safeLikesCount(video);
        }

        long newCount = this.videoMetadataPublicApi.incrementLikesCount(video.id(), -1);
        if (newCount < 0) {
            return safeLikesCount(video);
        }

        this.publishEvent("VIDEO_UNLIKED", video, userId, newCount);

        return newCount;
    }

    public long getCount(UUID videoUuid) {
        return this.videoMetadataPublicApi.findByUuid(videoUuid)
                .map(v -> {
                    if (v.likesCount() != null) {
                        return v.likesCount();
                    }
                    return this.videoLikeRepository.countByVideoId(v.id());
                })
                .orElse(0L);
    }

    public boolean isLikedByUser(UUID videoUuid, Long userId) {
        return this.videoMetadataPublicApi.findByUuid(videoUuid)
                .map(v -> this.videoLikeRepository.existsByVideoIdAndUserId(v.id(), userId))
                .orElse(false);
    }

    private long safeLikesCount(VideoMetadataSummary video) {
        if (video == null || video.likesCount() == null) {
            return 0L;
        }
        return video.likesCount();
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
