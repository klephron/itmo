package ru.itmo.zavar.brainstop.interactions.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoCommentsCountDto;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoUuidsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.WatchProgressSummaryDto;

import java.util.List;

@FeignClient(name = "interactions-service", contextId = "interactionsInternalClient", path = "/internal/interactions", fallbackFactory = InteractionsInternalClientFallbackFactory.class)
public interface InteractionsInternalClient {

    @PostMapping("/likes/ids")
    List<Long> getLikedVideoIds(@RequestBody VideoIdsRequest request);

    @PostMapping("/views/ids")
    List<Long> getViewedVideoIds(@RequestBody VideoIdsRequest request);

    @PostMapping("/watch/progress")
    List<WatchProgressSummaryDto> getWatchProgress(@RequestBody VideoIdsRequest request);

    @PostMapping("/comments/counts")
    List<VideoCommentsCountDto> getCommentsCount(@RequestBody VideoUuidsRequest request);
}
