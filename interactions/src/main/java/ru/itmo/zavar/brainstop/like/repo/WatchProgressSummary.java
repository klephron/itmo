package ru.itmo.zavar.brainstop.like.repo;

public interface WatchProgressSummary {
    Long getVideoId();

    Long getTotalWatchedSeconds();

    Long getDurationSeconds();
}
