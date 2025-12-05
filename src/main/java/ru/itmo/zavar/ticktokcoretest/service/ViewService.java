package ru.itmo.zavar.ticktokcoretest.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.ticktokcoretest.dto.response.ViewResponse;
import ru.itmo.zavar.ticktokcoretest.entity.FileMetadataEntity;
import ru.itmo.zavar.ticktokcoretest.entity.VideoViewEntity;
import ru.itmo.zavar.ticktokcoretest.integrations.rabbit.VideoViewEvent;
import ru.itmo.zavar.ticktokcoretest.repo.FileMetadataRepository;
import ru.itmo.zavar.ticktokcoretest.repo.VideoViewRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ViewService {

    private final FileMetadataRepository fileMetadataRepository;
    private final VideoViewRepository videoViewRepository;
    private final RabbitTemplate rabbitTemplate;
    private final DirectExchange viewsExchange;

    @Transactional
    public long viewVideo(UUID videoUuid, Long userId) {
        FileMetadataEntity video = this.fileMetadataRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено"));

        if (this.videoViewRepository.existsByVideoIdAndUserId(video.getId(), userId)) {
            return video.getViewsCount();
        }

        VideoViewEntity view = new VideoViewEntity();
        view.setVideoId(video.getId());
        view.setUserId(userId);
        this.videoViewRepository.save(view);

        long newCount = video.getViewsCount() + 1;
        video.setViewsCount(newCount);
        this.fileMetadataRepository.save(video);

        this.publishEvent(video, userId, newCount);

        return newCount;
    }

    public long getViews(UUID videoUuid) {
        FileMetadataEntity video = this.fileMetadataRepository.findByUuid(videoUuid)
                .orElseThrow(() -> new IllegalArgumentException("Видео не найдено"));
        return video.getViewsCount();
    }

    private void publishEvent(FileMetadataEntity video, Long userId, Long viewsCount) {
        VideoViewEvent event = new VideoViewEvent(
                userId,
                video.getUuid(),
                video.getId(),
                viewsCount,
                Instant.now()
        );
        this.rabbitTemplate.convertAndSend(this.viewsExchange.getName(), "video.viewed", event);
    }
}