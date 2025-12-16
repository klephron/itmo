package ru.itmo.zavar.brainstop.common.api.search.dto;

import java.util.List;

public record EmbeddingResponse(List<Float> vector) {
}
