package ru.itmo.zavar.brainstop.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {
}
