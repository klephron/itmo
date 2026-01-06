package ru.itmo.zavar.brainstop.common.api.interactions.dto;

public record WatchProgressSummaryDto(Long videoId, Long totalWatchedSeconds, Long durationSeconds) {
}
