package ru.itmo.zavar.brainstop.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchSuggestRequest;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchVideosRequest;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.search.client.SearchInternalClient;
import ru.itmo.zavar.brainstop.search.embedding.EmbeddingService;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmService;
import com.pgvector.PGvector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Поиск видео по тексту/эмбеддингам (отдельный сервис)")
public class SearchController {

    private final SearchInternalClient searchInternalClient;
    private final EmbeddingService embeddingService;
    private final SearchLlmService searchLlmService;

    @GetMapping("/videos")
    @Operation(summary = "Поиск видео", description = "Возвращает страницу совпадений")
    public ResponseEntity<PageResponse<FileMetadataDto>> searchVideos(@RequestParam("q") String query,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size,
                                                                       @RequestParam(name = "ai", defaultValue = "true") boolean ai) {
        String rawQuery = query == null ? "" : query;
        PageResponse<FileMetadataDto> response = executeSearch(rawQuery, page, size, ai);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/videos/suggest")
    @Operation(summary = "Подсказки поиска", description = "Топ подходящих видео")
    public ResponseEntity<List<FileMetadataDto>> suggestVideos(@RequestParam("q") String query,
                                                                @RequestParam(defaultValue = "5") int limit,
                                                                @RequestParam(name = "ai", defaultValue = "true") boolean ai) {
        if (limit <= 0) {
            return ResponseEntity.ok(List.of());
        }
        String safeQuery = query == null ? "" : query.trim();
        if (safeQuery.isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<FileMetadataDto> list = this.searchInternalClient.suggest(
                new SearchSuggestRequest("", safeQuery, limit));
        return ResponseEntity.ok(list);
    }

    @GetMapping(value = "/llm/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "LLM stream", description = "Stream LLM rewrite/expand output for debugging.")
    public SseEmitter streamLlm(@RequestParam("q") String query,
                                @RequestParam(name = "ai", defaultValue = "true") boolean ai,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        SseEmitter emitter = new SseEmitter(120_000L);
        CompletableFuture.runAsync(() -> {
            String safeQuery = query == null ? "" : query.trim();
            if (!ai || safeQuery.isBlank()) {
                sendEvent(emitter, "status", "disabled");
                emitter.complete();
                return;
            }
            if (!searchLlmService.isLlmEnabled()) {
                sendEvent(emitter, "status", "llm:disabled");
                emitter.complete();
                return;
            }
            int minLen = searchLlmService.minQueryLength();
            if (minLen > 0 && safeQuery.length() < minLen) {
                sendEvent(emitter, "status", "too_short");
                emitter.complete();
                return;
            }
            try {
                String context = "";
                PageResponse<FileMetadataDto> pageResult = executeSearch(safeQuery, page, size, false);
                context = buildLlmContext(pageResult.content());
                if (!context.isBlank()) {
                    sendEvent(emitter, "context", context);
                }
                if (!searchLlmService.shouldUseLlmVariants(safeQuery)) {
                    sendEvent(emitter, "status", "skip:short");
                    sendEvent(emitter, "done", "ok");
                    emitter.complete();
                    return;
                }
                StringBuilder expandBuffer = new StringBuilder();
                if (searchLlmService.isRewriteEnabled()) {
                    sendEvent(emitter, "status", "rewrite:start");
                    searchLlmService.streamRewrite(safeQuery, context,
                            delta -> sendEvent(emitter, "rewrite", delta));
                    sendEvent(emitter, "status", "rewrite:done");
                } else {
                    sendEvent(emitter, "status", "rewrite:skip");
                }
                if (searchLlmService.isExpansionEnabled()) {
                    sendEvent(emitter, "status", "expand:start");
                    searchLlmService.streamExpand(safeQuery, context,
                            delta -> {
                                if (delta != null) {
                                    expandBuffer.append(delta);
                                }
                                sendEvent(emitter, "expand", delta);
                            });
                    List<String> normalized = searchLlmService.cacheExpansionFromStream(safeQuery, expandBuffer.toString());
                    if (!normalized.isEmpty()) {
                        sendEvent(emitter, "expand-final", formatExpansionList(normalized));
                    }
                    sendEvent(emitter, "status", "expand:done");
                } else {
                    sendEvent(emitter, "status", "expand:skip");
                }
                sendEvent(emitter, "done", "ok");
                emitter.complete();
            } catch (Exception e) {
                sendEvent(emitter, "llm-error", e.getMessage());
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    private String itemKey(FileMetadataDto item) {
        if (item == null) {
            return null;
        }
        if (item.id() != null) {
            return "id:" + item.id();
        }
        if (item.uuid() != null) {
            return "uuid:" + item.uuid();
        }
        return null;
    }

    private RagContext prepareRagContext(String query) {
        if (!searchLlmService.isRagEnabled()) {
            return new RagContext("", null, "");
        }
        String baseQuery = query == null ? "" : query.trim();
        if (baseQuery.isBlank()) {
            return new RagContext("", null, "");
        }
        int limit = searchLlmService.ragLimit();
        if (limit <= 0) {
            return new RagContext("", null, baseQuery);
        }
        String vecLiteral = new PGvector(embeddingService.embed(baseQuery)).toString();
        PageResponse<FileMetadataDto> response = this.searchInternalClient.searchVideos(
                new SearchVideosRequest(vecLiteral, baseQuery, 0, limit));
        String context = searchLlmService.buildRagContext(response.content());
        return new RagContext(context, vecLiteral, baseQuery);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data == null ? "" : data));
        } catch (Exception ignored) {
        }
    }

    private String buildLlmContext(List<FileMetadataDto> candidates) {
        String context = searchLlmService.buildCandidateContext(candidates);
        String rag = searchLlmService.buildRagContext(candidates);
        if (rag.isBlank()) {
            return context;
        }
        if (context.isBlank()) {
            return rag;
        }
        return context + "\n" + rag;
    }

    private String formatExpansionList(List<String> expansions) {
        if (expansions == null || expansions.isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String item : expansions) {
            if (item == null || item.isBlank()) {
                continue;
            }
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append("\"").append(escapeJson(item)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String resolveVectorLiteral(String query, RagContext ragContext) {
        if (ragContext.vecLiteral() != null &&
                !ragContext.baseQuery().isBlank() &&
                query != null &&
                query.trim().equals(ragContext.baseQuery())) {
            return ragContext.vecLiteral();
        }
        return new PGvector(embeddingService.embed(query)).toString();
    }

    private PageResponse<FileMetadataDto> executeSearch(String rawQuery, int page, int size, boolean ai) {
        if (size <= 0) {
            return new PageResponse<>(List.of(), page, size, 0);
        }
        String safeQuery = rawQuery == null ? "" : rawQuery;
        if (!ai) {
            String vecLiteral = new PGvector(embeddingService.embed(safeQuery)).toString();
            return this.searchInternalClient.searchVideos(
                    new SearchVideosRequest(vecLiteral, safeQuery, page, size));
        }
        int poolSize = searchLlmService.poolSize(page, size);
        String llmContext = "";
        List<FileMetadataDto> corpusCandidates = List.of();
        if (searchLlmService.isLlmEnabled() && !safeQuery.isBlank()) {
            String baseVec = new PGvector(embeddingService.embed(safeQuery)).toString();
            PageResponse<FileMetadataDto> baseResponse = this.searchInternalClient.searchVideos(
                    new SearchVideosRequest(baseVec, safeQuery, 0, poolSize));
            corpusCandidates = baseResponse.content();
            if (searchLlmService.isRewriteEnabled() || searchLlmService.isExpansionEnabled()) {
                llmContext = buildLlmContext(corpusCandidates);
            }
        }
        List<String> variants = searchLlmService.buildQueryVariants(safeQuery, llmContext, corpusCandidates);
        List<FileMetadataDto> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        long total = 0;
        for (String variant : variants) {
            String vecLiteral = new PGvector(embeddingService.embed(variant)).toString();
            PageResponse<FileMetadataDto> response = this.searchInternalClient.searchVideos(
                    new SearchVideosRequest(vecLiteral, variant, 0, poolSize));
            total = Math.max(total, response.totalElements());
            for (FileMetadataDto item : response.content()) {
                String key = itemKey(item);
                if (key != null && seen.add(key)) {
                    merged.add(item);
                }
            }
        }
        List<FileMetadataDto> filtered = searchLlmService.filterCandidates(safeQuery, variants, merged, merged);
        int from = Math.max(page, 0) * size;
        int to = Math.min(from + size, filtered.size());
        List<FileMetadataDto> slice = from >= filtered.size() ? List.of() : filtered.subList(from, to);
        long effectiveTotal = searchLlmService.isFilterEnabled()
                ? filtered.size()
                : Math.max(total, merged.size());
        return new PageResponse<>(slice, page, size, effectiveTotal);
    }

    private record RagContext(String context, String vecLiteral, String baseQuery) {
    }
}
