package ru.itmo.zavar.brainstop.search.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.itmo.zavar.brainstop.common.api.search.dto.EmbeddingResponse;

@FeignClient(name = "search-service", contextId = "searchEmbeddingClient", path = "/internal/embedding")
public interface SearchEmbeddingClient {

    @GetMapping("/vector")
    EmbeddingResponse embed(@RequestParam("text") String text);
}
