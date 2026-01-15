package ru.itmo.zavar.brainstop.common.api.search.dto;

public record SearchVideosRequest(String vec, String q, Integer page, Integer size) {
}
