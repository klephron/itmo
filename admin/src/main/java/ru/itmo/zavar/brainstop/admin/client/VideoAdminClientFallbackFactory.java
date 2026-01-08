package ru.itmo.zavar.brainstop.admin.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class VideoAdminClientFallbackFactory implements FallbackFactory<VideoAdminClient> {

    @Override
    public VideoAdminClient create(Throwable cause) {
        log.warn("VideoAdminClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return force -> Map.of(
                "status", "unavailable",
                "message", "video service not available"
        );
    }
}
