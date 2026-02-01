package ru.itmo.zavar.brainstop.interactions.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoCommentsCountDto;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoUuidsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.WatchProgressSummaryDto;

import java.util.List;

@Component
@Slf4j
public class InteractionsInternalClientFallbackFactory implements FallbackFactory<InteractionsInternalClient> {

    @Override
    public InteractionsInternalClient create(Throwable cause) {
        log.warn("InteractionsInternalClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return new InteractionsInternalClient() {
            @Override
            public List<Long> getLikedVideoIds(VideoIdsRequest request) {
                return List.of();
            }

            @Override
            public List<Long> getViewedVideoIds(VideoIdsRequest request) {
                return List.of();
            }

            @Override
            public List<WatchProgressSummaryDto> getWatchProgress(VideoIdsRequest request) {
                return List.of();
            }

            @Override
            public List<VideoCommentsCountDto> getCommentsCount(VideoUuidsRequest request) {
                return List.of();
            }
        };
    }
}
