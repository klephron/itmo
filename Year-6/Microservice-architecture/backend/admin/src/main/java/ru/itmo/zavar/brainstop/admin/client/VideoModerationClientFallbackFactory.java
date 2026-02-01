package ru.itmo.zavar.brainstop.admin.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class VideoModerationClientFallbackFactory implements FallbackFactory<VideoModerationClient> {

    @Override
    public VideoModerationClient create(Throwable cause) {
        log.warn("VideoModerationClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new VideoModerationClient() {
            @Override
            public List<UserVideoDto> getUserVideos(Long userId) {
                return List.of();
            }

            @Override
            public Map<String, Object> rebuildEmbeddings() {
                return Map.of("updated", 0, "fallback", true);
            }
        };
    }
}
