package ru.itmo.zavar.ticktokcoretest.service;

import jakarta.transaction.Transactional;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.dto.VideoLikeEvent;
import ru.itmo.zavar.ticktokcoretest.model.FileMetadata;
import ru.itmo.zavar.ticktokcoretest.model.VideoLike;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepo;
import ru.itmo.zavar.ticktokcoretest.repo.VideoLikeRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class LikeService {

    private final FileMetadataRepo fileMetadataRepo;
    private final VideoLikeRepository videoLikeRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange likesExchange;

    public LikeService(FileMetadataRepo fileMetadataRepo,
                       VideoLikeRepository videoLikeRepository,
                       RabbitTemplate rabbitTemplate,
                       DirectExchange likesExchange) {
        this.fileMetadataRepo = fileMetadataRepo;
        this.videoLikeRepository = videoLikeRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.likesExchange = likesExchange;
    }

    @Transactional
    public long likeVideo(UUID videoUuid, Long userId) {
        FileMetadata video = fileMetadataRepo.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено"));

        if (videoLikeRepository.existsByVideoIdAndUserId(video.getId(), userId)) {
            // уже лайкнул — ничего не делаем, просто возвращаем текущий счётчик
            return video.getLikesCount();
        }

        VideoLike like = new VideoLike(video.getId(), userId);
        videoLikeRepository.save(like);

        // увеличиваем локальный счётчик
        long newCount = video.getLikesCount() + 1;
        video.setLikesCount(newCount);
        fileMetadataRepo.save(video);

        // публикуем событие
        publishEvent("VIDEO_LIKED", video, userId, newCount);

        return newCount;
    }

    @Transactional
    public long unlikeVideo(UUID videoUuid, Long userId) {
        FileMetadata video = fileMetadataRepo.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено"));

        Optional<VideoLike> existing = videoLikeRepository.findByVideoIdAndUserId(video.getId(), userId);
        if (existing.isEmpty()) {
            // не лайкал — просто возвращаем текущий счётчик
            return video.getLikesCount();
        }

        videoLikeRepository.delete(existing.get());

        long newCount = Math.max(0, video.getLikesCount() - 1);
        video.setLikesCount(newCount);
        fileMetadataRepo.save(video);

        publishEvent("VIDEO_UNLIKED", video, userId, newCount);

        return newCount;
    }

    public boolean isLikedByUser(UUID videoUuid, Long userId) {
        return fileMetadataRepo.findByUuid(videoUuid)
                .map(v -> videoLikeRepository.existsByVideoIdAndUserId(v.getId(), userId))
                .orElse(false);
    }

    private void publishEvent(String type, FileMetadata video, Long userId, Long likesCount) {
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

