package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToVideoMetadataSummaryMapper;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoMetadataPublicApiImpl implements VideoMetadataPublicApi {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataToVideoMetadataSummaryMapper videoMetadataSummaryMapper;

    @Override
    public VideoMetadataSummary getRequired(UUID uuid) {
        return findByUuid(uuid).orElseThrow(() -> new IllegalArgumentException("Video not found: " + uuid));
    }

    @Override
    public Optional<VideoMetadataSummary> findByUuid(UUID uuid) {
        return this.fileMetadataRepository.findByUuid(uuid)
                .map(this.videoMetadataSummaryMapper::toSummary);
    }

    @Override
    public void updateLikesCount(Long videoId, long newCount) {
        this.fileMetadataRepository.findById(videoId)
                .ifPresent(meta -> {
                    meta.setLikesCount(newCount);
                    this.fileMetadataRepository.save(meta);
                });
    }

    @Override
    public void updateViewsCount(Long videoId, long newCount) {
        this.fileMetadataRepository.findById(videoId)
                .ifPresent(meta -> {
                    meta.setViewsCount(newCount);
                    this.fileMetadataRepository.save(meta);
                });
    }

}
