package ru.itmo.zavar.brainstop.common.api.search.dto;

public record SearchSuggestRequest(String vec, String q, Integer limit) {
}
