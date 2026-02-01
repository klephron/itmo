package ru.itmo.zavar.brainstop.profile.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;

import java.util.List;

@Component
@Slf4j
public class VideoInternalClientFallbackFactory implements FallbackFactory<VideoInternalClient> {

    @Override
    public VideoInternalClient create(Throwable cause) {
        log.warn("VideoInternalClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return (uploaderId, page, size, sort, currentUserId) ->
                new PageResponse<>(List.of(), page, size, 0);
    }
}
