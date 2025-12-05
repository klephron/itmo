package ru.itmo.zavar.ticktokcoretest.service;

import jakarta.transaction.Transactional;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.integrations.rabbit.VideoLikeEvent;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.entity.VideoLikeEntity;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.VideoLikeRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class LikeService {

    private final FileMetadataRepository fileMetadataRepository;
    private final VideoLikeRepository videoLikeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange likesExchange;

    public LikeService(FileMetadataRepository fileMetadataRepository,
                       VideoLikeRepository videoLikeRepository,
                       RabbitTemplate rabbitTemplate,
                       DirectExchange likesExchange) {
        this.fileMetadataRepository = fileMetadataRepository;
        this.videoLikeRepository = videoLikeRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.likesExchange = likesExchange;
    }

    @Transactional
    public long likeVideo(UUID videoUuid, Long userId) {
        FileMetadataEntity video = fileMetadataRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено"));

        if (videoLikeRepository.existsByVideoIdAndUserId(video.getId(), userId)) {
            // уже лайкнул — ничего не делаем, просто возвращаем текущий счётчик
            return video.getLikesCount();
        }

        VideoLikeEntity like = new VideoLikeEntity();
        like.setVideoId(video.getId());
        like.setUserId(userId);
        videoLikeRepository.save(like);

        // увеличиваем локальный счётчик
        long newCount = video.getLikesCount() + 1;
        video.setLikesCount(newCount);
        fileMetadataRepository.save(video);

        // публикуем событие
        publishEvent("VIDEO_LIKED", video, userId, newCount);

        return newCount;
    }

    @Transactional
    public long unlikeVideo(UUID videoUuid, Long userId) {
        FileMetadataEntity video = fileMetadataRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено"));

        Optional<VideoLikeEntity> existing = videoLikeRepository.findByVideoIdAndUserId(video.getId(), userId);
        if (existing.isEmpty()) {
            // не лайкал — просто возвращаем текущий счётчик
            return video.getLikesCount();
        }

        videoLikeRepository.delete(existing.get());

        long newCount = Math.max(0, video.getLikesCount() - 1);
        video.setLikesCount(newCount);
        fileMetadataRepository.save(video);

        publishEvent("VIDEO_UNLIKED", video, userId, newCount);

        return newCount;
    }

    public boolean isLikedByUser(UUID videoUuid, Long userId) {
        return fileMetadataRepository.findByUuid(videoUuid)
                .map(v -> videoLikeRepository.existsByVideoIdAndUserId(v.getId(), userId))
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
        rabbitTemplate.convertAndSend(likesExchange.getName(), routingKey, event);
    }
}

