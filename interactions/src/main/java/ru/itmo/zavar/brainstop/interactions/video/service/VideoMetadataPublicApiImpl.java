package ru.itmo.zavar.brainstop.interactions.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.interactions.video.client.VideoMetadataInternalClient;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoMetadataPublicApiImpl implements VideoMetadataPublicApi {

    private static final Duration META_TTL = Duration.ofSeconds(30);
    private static final String META_KEY_PREFIX = "video:meta:interactions:";

    private final VideoMetadataInternalClient videoMetadataInternalClient;
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

        VideoMetadataSummary summary;
        try {
            summary = videoMetadataInternalClient.getByUuid(uuid);
        } catch (FeignException.NotFound e) {
            return Optional.empty();
        }
        writeCache(summary);
        return Optional.of(summary);
    }

    @Override
    public long incrementLikesCount(Long videoId, long delta) {
        try {
            Long updated = videoMetadataInternalClient.incrementLikes(videoId, delta);
            return updated != null ? updated : -1L;
        } catch (FeignException.NotFound e) {
            return -1L;
        }
    }

    @Override
    public long incrementViewsCount(Long videoId, long delta) {
        try {
            Long updated = videoMetadataInternalClient.incrementViews(videoId, delta);
            return updated != null ? updated : -1L;
        } catch (FeignException.NotFound e) {
            return -1L;
        }
    }

    @Override
    public void updateLikesCount(Long videoId, long newCount) {
        try {
            videoMetadataInternalClient.updateLikes(videoId, newCount);
        } catch (FeignException.NotFound e) {
            return;
        }
    }

    @Override
    public void updateViewsCount(Long videoId, long newCount) {
        try {
            videoMetadataInternalClient.updateViews(videoId, newCount);
        } catch (FeignException.NotFound e) {
            return;
        }
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
