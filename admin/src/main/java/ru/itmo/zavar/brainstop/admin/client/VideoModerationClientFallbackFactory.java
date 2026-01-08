package ru.itmo.zavar.brainstop.admin.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;

import java.util.List;

@Component
@Slf4j
public class VideoModerationClientFallbackFactory implements FallbackFactory<VideoModerationClient> {

    @Override
    public VideoModerationClient create(Throwable cause) {
        log.warn("VideoModerationClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return userId -> List.of();
    }
}
