package ru.itmo.zavar.ticktokcoretest.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.entity.VideoLikeEntity;
import ru.itmo.zavar.ticktokcoretest.integrations.rabbit.VideoLikeEvent;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.VideoLikeRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final FileMetadataRepository fileMetadataRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange likesExchange;

    @Transactional
    public long likeVideo(UUID videoUuid, Long userId) {
        FileMetadataEntity video = this.fileMetadataRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено"));

        if (this.videoLikeRepository.existsByVideoIdAndUserId(video.getId(), userId)) {
            return video.getLikesCount();
        }

        VideoLikeEntity like = new VideoLikeEntity();
        like.setVideoId(video.getId());
        like.setUserId(userId);
        this.videoLikeRepository.save(like);

        long newCount = video.getLikesCount() + 1;
        video.setLikesCount(newCount);
        this.fileMetadataRepository.save(video);

        this.publishEvent("VIDEO_LIKED", video, userId, newCount);

        return newCount;
    }

    @Transactional
    public long unlikeVideo(UUID videoUuid, Long userId) {
        FileMetadataEntity video = this.fileMetadataRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено"));

        Optional<VideoLikeEntity> existing = this.videoLikeRepository.findByVideoIdAndUserId(video.getId(), userId);
        if (existing.isEmpty()) {
            return video.getLikesCount();
        }

        this.videoLikeRepository.delete(existing.get());

        long newCount = Math.max(0, video.getLikesCount() - 1);
        video.setLikesCount(newCount);
        this.fileMetadataRepository.save(video);

        this.publishEvent("VIDEO_UNLIKED", video, userId, newCount);

        return newCount;
    }

    public boolean isLikedByUser(UUID videoUuid, Long userId) {
        return this.fileMetadataRepository.findByUuid(videoUuid)
                .map(v -> this.videoLikeRepository.existsByVideoIdAndUserId(v.getId(), userId))
                .orElse(false);
    }

    private void publishEvent(String type, FileMetadataEntity video, Long userId, Long likesCount) {
        VideoLikeEvent event = new VideoLikeEvent(
                type,
                userId,
                video.getUuid(),
                video.getId(),
                likesCount,
                Instant.now()
        );
        String routingKey = type.equals("VIDEO_LIKED") ? "video.liked" : "video.unliked";
        this.rabbitTemplate.convertAndSend(this.likesExchange.getName(), routingKey, event);
    }
}

