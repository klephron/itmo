package ru.itmo.zavar.brainstop.interactions.video.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;

import java.util.UUID;

@Component
@Slf4j
public class VideoMetadataInternalClientFallbackFactory implements FallbackFactory<VideoMetadataInternalClient> {

    @Override
    public VideoMetadataInternalClient create(Throwable cause) {
        log.warn("VideoMetadataInternalClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new VideoMetadataInternalClient() {
            @Override
            public VideoMetadataSummary getByUuid(UUID uuid) {
                return null;
            }

            @Override
            public void updateLikes(Long id, long count) {
            }

            @Override
            public void updateViews(Long id, long count) {
            }

            @Override
            public Long incrementLikes(Long id, long delta) {
                return null;
            }

            @Override
            public Long incrementViews(Long id, long delta) {
                return null;
            }
        };
    }
}
