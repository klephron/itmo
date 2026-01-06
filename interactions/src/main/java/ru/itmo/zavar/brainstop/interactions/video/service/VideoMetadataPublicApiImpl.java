package ru.itmo.zavar.brainstop.interactions.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.interactions.video.model.VideoMetadataEntity;
import ru.itmo.zavar.brainstop.interactions.video.repo.VideoMetadataRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoMetadataPublicApiImpl implements VideoMetadataPublicApi {

    private static final Duration META_TTL = Duration.ofSeconds(30);
    private static final String META_KEY_PREFIX = "video:meta:interactions:";

    private final VideoMetadataRepository videoMetadataRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public VideoMetadataSummary getRequired(UUID uuid) {
        return findByUuid(uuid)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + uuid));
    }

    @Override
    public Optional<VideoMetadataSummary> findByUuid(UUID uuid) {
        Optional<VideoMetadataSummary> cached = readCache(uuid);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<VideoMetadataSummary> summary = videoMetadataRepository.findByUuid(uuid)
                .map(this::toSummary);
        summary.ifPresent(this::writeCache);
        return summary;
    }

    @Override
    public void updateLikesCount(Long videoId, long newCount) {
        videoMetadataRepository.findById(videoId)
                .ifPresent(meta -> {
                    meta.setLikesCount(newCount);
                    videoMetadataRepository.save(meta);
                    writeCache(toSummary(meta));
                });
    }

    @Override
    public void updateViewsCount(Long videoId, long newCount) {
        videoMetadataRepository.findById(videoId)
                .ifPresent(meta -> {
                    meta.setViewsCount(newCount);
                    videoMetadataRepository.save(meta);
                    writeCache(toSummary(meta));
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
