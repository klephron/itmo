package ru.itmo.zavar.brainstop.search.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.itmo.zavar.brainstop.common.api.search.dto.EmbeddingResponse;

@Component
@Slf4j
public class SearchEmbeddingClientFallbackFactory implements FallbackFactory<SearchEmbeddingClient> {

    @Override
    public SearchEmbeddingClient create(Throwable cause) {
        log.warn("SearchEmbeddingClient fallback triggered: {}", cause == null ? "unknown" : cause.toString());
        return text -> null;
    }
}
