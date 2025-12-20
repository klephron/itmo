package ru.itmo.zavar.brainstop.like.dto.request;

public record WatchProgressRequest(Long positionSeconds, Long durationSeconds, Integer percent) {
}
