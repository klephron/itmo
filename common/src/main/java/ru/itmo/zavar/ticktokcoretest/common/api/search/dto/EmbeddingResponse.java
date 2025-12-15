package ru.itmo.zavar.ticktokcoretest.common.api.search.dto;

import java.util.List;

public record EmbeddingResponse(List<Float> vector) {
}
