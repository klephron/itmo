package ru.itmo.zavar.brainstop.interactions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.WatchProgressSummaryDto;
import ru.itmo.zavar.brainstop.like.repo.VideoLikeRepository;
import ru.itmo.zavar.brainstop.like.repo.VideoViewRepository;
import ru.itmo.zavar.brainstop.like.repo.VideoWatchRepository;
import ru.itmo.zavar.brainstop.like.repo.WatchProgressSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/interactions")
@RequiredArgsConstructor
@Tag(name = "Internal-Interactions", description = "Internal endpoints for interactions data")
public class InteractionsInternalController {

    private final VideoLikeRepository videoLikeRepository;
    private final VideoViewRepository videoViewRepository;
    private final VideoWatchRepository videoWatchRepository;

    @PostMapping("/likes/ids")
    @Operation(summary = "Internal: list liked video IDs")
    public List<Long> likedVideoIds(@RequestBody VideoIdsRequest request) {
        if (request == null || request.userId() == null || request.videoIds() == null || request.videoIds().isEmpty()) {
            return List.of();
        }
        return videoLikeRepository.findLikedVideoIdsByUserIdAndVideoIdIn(request.userId(), request.videoIds());
    }

    @PostMapping("/views/ids")
    @Operation(summary = "Internal: list viewed video IDs")
    public List<Long> viewedVideoIds(@RequestBody VideoIdsRequest request) {
        if (request == null || request.userId() == null || request.videoIds() == null || request.videoIds().isEmpty()) {
            return List.of();
        }
        Set<Long> viewed = videoViewRepository.findViewedVideoIds(request.userId(), request.videoIds());
        if (viewed == null || viewed.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(viewed);
    }

    @PostMapping("/watch/progress")
    @Operation(summary = "Internal: list watch progress summaries")
    public List<WatchProgressSummaryDto> watchProgress(@RequestBody VideoIdsRequest request) {
        if (request == null || request.userId() == null || request.videoIds() == null || request.videoIds().isEmpty()) {
            return List.of();
        }
        List<WatchProgressSummary> progress = videoWatchRepository.findWatchProgress(request.userId(), request.videoIds());
        if (progress == null || progress.isEmpty()) {
            return List.of();
        }
        return progress.stream()
                .map(p -> new WatchProgressSummaryDto(p.getVideoId(), p.getTotalWatchedSeconds(), p.getDurationSeconds()))
                .toList();
    }
}
