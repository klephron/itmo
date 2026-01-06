package ru.itmo.zavar.brainstop.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToVideoMetadataSummaryMapper;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoMetadataPublicApiImpl implements VideoMetadataPublicApi {

    private static final Duration META_TTL = Duration.ofSeconds(30);
    private static final String META_KEY_PREFIX = "video:meta:video:";

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataToVideoMetadataSummaryMapper videoMetadataSummaryMapper;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public VideoMetadataSummary getRequired(UUID uuid) {
        return findByUuid(uuid).orElseThrow(() -> new IllegalArgumentException("Video not found: " + uuid));
    }

    @Override
    public Optional<VideoMetadataSummary> findByUuid(UUID uuid) {
        Optional<VideoMetadataSummary> cached = readCache(uuid);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<VideoMetadataSummary> summary = this.fileMetadataRepository.findByUuid(uuid)
                .map(this.videoMetadataSummaryMapper::toSummary);
        summary.ifPresent(this::writeCache);
        return summary;
    }

    @Override
    @Transactional
    public long incrementLikesCount(Long videoId, long delta) {
        if (videoId == null || delta == 0L) {
            return -1L;
        }
        return this.fileMetadataRepository.findByIdForUpdate(videoId)
                .map(meta -> {
                    long current = meta.getLikesCount() != null ? meta.getLikesCount() : 0L;
                    long updated = Math.max(0L, current + delta);
                    meta.setLikesCount(updated);
                    this.fileMetadataRepository.save(meta);
                    writeCache(this.videoMetadataSummaryMapper.toSummary(meta));
                    return updated;
                })
                .orElse(-1L);
    }

    @Override
    @Transactional
    public long incrementViewsCount(Long videoId, long delta) {
        if (videoId == null || delta == 0L) {
            return -1L;
        }
        return this.fileMetadataRepository.findByIdForUpdate(videoId)
                .map(meta -> {
                    long current = meta.getViewsCount() != null ? meta.getViewsCount() : 0L;
                    long updated = Math.max(0L, current + delta);
                    meta.setViewsCount(updated);
                    this.fileMetadataRepository.save(meta);
                    writeCache(this.videoMetadataSummaryMapper.toSummary(meta));
                    return updated;
                })
                .orElse(-1L);
    }

    @Override
    public void updateLikesCount(Long videoId, long newCount) {
        this.fileMetadataRepository.findById(videoId)
                .ifPresent(meta -> {
                    meta.setLikesCount(newCount);
                    this.fileMetadataRepository.save(meta);
                    writeCache(this.videoMetadataSummaryMapper.toSummary(meta));
                });
    }

    @Override
    public void updateViewsCount(Long videoId, long newCount) {
        this.fileMetadataRepository.findById(videoId)
                .ifPresent(meta -> {
                    meta.setViewsCount(newCount);
                    this.fileMetadataRepository.save(meta);
                    writeCache(this.videoMetadataSummaryMapper.toSummary(meta));
                });
    }

    private Optional<VideoMetadataSummary> readCache(UUID uuid) {
        if (uuid == null) {
            return Optional.empty();
        }
        String payload = redisTemplate.opsForValue().get(metaKey(uuid));
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(payload, VideoMetadataSummary.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    private void writeCache(VideoMetadataSummary summary) {
        if (summary == null || summary.uuid() == null) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(summary);
            redisTemplate.opsForValue().set(metaKey(summary.uuid()), payload, META_TTL);
        } catch (JsonProcessingException e) {
            return;
        }
    }

    private String metaKey(UUID uuid) {
        return META_KEY_PREFIX + uuid;
    }
}
