package ru.itmo.zavar.brainstop.interactions.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.zavar.brainstop.comment.repo.CommentRepository;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoCommentsCountDto;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoUuidsRequest;
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
@Tag(name = "Интеракции (внутренние)", description = "Внутренние эндпоинты интеракций: лайки, просмотры, комментарии")
public class InteractionsInternalController {

    private final VideoLikeRepository videoLikeRepository;
    private final VideoViewRepository videoViewRepository;
    private final VideoWatchRepository videoWatchRepository;
    private final CommentRepository commentRepository;

    @PostMapping("/likes/ids")
    @Operation(summary = "Список лайкнутых видео пользователя")
    public List<Long> likedVideoIds(@Valid @RequestBody VideoIdsRequest request) {
        if (request == null || request.userId() == null || request.videoIds() == null || request.videoIds().isEmpty()) {
            return List.of();
        }
        return videoLikeRepository.findLikedVideoIdsByUserIdAndVideoIdIn(request.userId(), request.videoIds());
    }

    @PostMapping("/views/ids")
    @Operation(summary = "Список просмотренных видео пользователя")
    public List<Long> viewedVideoIds(@Valid @RequestBody VideoIdsRequest request) {
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
    @Operation(summary = "Прогресс просмотра по видео")
    public List<WatchProgressSummaryDto> watchProgress(@Valid @RequestBody VideoIdsRequest request) {
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

    @PostMapping("/comments/counts")
    @Operation(summary = "Количество комментариев по UUID видео")
    public List<VideoCommentsCountDto> commentsCounts(@Valid @RequestBody VideoUuidsRequest request) {
        if (request == null || request.videoUuids() == null || request.videoUuids().isEmpty()) {
            return List.of();
        }
        List<CommentRepository.CommentCountView> rows = commentRepository.countByVideoUuidIn(request.videoUuids());
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .filter(r -> r != null && r.getVideoUuid() != null)
                .map(r -> new VideoCommentsCountDto(r.getVideoUuid(), r.getCount()))
                .toList();
    }
}
