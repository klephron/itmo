package ru.itmo.zavar.brainstop.interactions.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class VideoAdminInternalClientFallbackFactory implements FallbackFactory<VideoAdminInternalClient> {

    @Override
    public VideoAdminInternalClient create(Throwable cause) {
        log.warn("VideoAdminInternalClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new VideoAdminInternalClient() {
            @Override
            public void deleteVideo(UUID videoUuid) {
                throw new IllegalStateException("Video service unavailable");
            }
        };
    }
}
