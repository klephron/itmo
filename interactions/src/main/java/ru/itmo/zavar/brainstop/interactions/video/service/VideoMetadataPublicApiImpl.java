package ru.itmo.zavar.brainstop.interactions.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.interactions.video.model.VideoMetadataEntity;
import ru.itmo.zavar.brainstop.interactions.video.repo.VideoMetadataRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoMetadataPublicApiImpl implements VideoMetadataPublicApi {

    private final VideoMetadataRepository videoMetadataRepository;

    @Override
    public VideoMetadataSummary getRequired(UUID uuid) {
        return findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + uuid));
    }

    @Override
    public Optional<VideoMetadataSummary> findByUuid(UUID uuid) {
        return videoMetadataRepository.findByUuid(uuid)
                .map(this::toSummary);
    }

    @Override
    public void updateLikesCount(Long videoId, long newCount) {
        videoMetadataRepository.findById(videoId)
                .ifPresent(meta -> {
                    meta.setLikesCount(newCount);
                    videoMetadataRepository.save(meta);
                });
    }

    @Override
    public void updateViewsCount(Long videoId, long newCount) {
        videoMetadataRepository.findById(videoId)
                .ifPresent(meta -> {
                    meta.setViewsCount(newCount);
                    videoMetadataRepository.save(meta);
                });
    }

    private VideoMetadataSummary toSummary(VideoMetadataEntity meta) {
        return new VideoMetadataSummary(
                meta.getId(),
                meta.getUuid(),
                meta.getLikesCount(),
                meta.getViewsCount(),
                null,
                null,
                null,
                null,
                null
        );
    }
}
