package ru.itmo.zavar.brainstop.search.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchSuggestRequest;
import ru.itmo.zavar.brainstop.common.api.search.dto.SearchVideosRequest;
import ru.itmo.zavar.brainstop.common.api.video.dto.FileMetadataDto;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/internal/search")
@RequiredArgsConstructor
@Tag(name = "Internal-Search", description = "Р'Р?С?С'С?РчР?Р?РёР№ API Р?Р>С? РїР?РёС?РєР° (РёС?РїР?Р>С?Р·С?РчС'С?С? search-service)")
public class SearchInternalController {

    private static final int HYBRID_MULTIPLIER = 3;
    private static final int HYBRID_MAX_CANDIDATES = 200;
    private static final double VECTOR_WEIGHT = 0.6;
    private static final double TEXT_WEIGHT = 0.4;
    private static final int SHORT_QUERY_MAX_LEN = 4;
    private static final double SHORT_VECTOR_WEIGHT = 0.35;
    private static final double SHORT_TEXT_WEIGHT = 0.65;
    private static final double SHORT_TOKEN_NAME_BOOST = 0.25;
    private static final double SHORT_TOKEN_DESC_BOOST = 0.15;
    private static final double SHORT_TOKEN_UPLOADER_BOOST = 0.1;
    private static final double SHORT_TOKEN_PREFIX_RATIO = 0.7;
    private static final int SHORT_TOKEN_PREFIX_MAX_EXTRA = 2;
    private static final double EXACT_MATCH_BOOST = 0.4;
    private static final double PREFIX_MATCH_BOOST = 0.2;
    private static final double NAME_MATCH_BOOST = 0.1;
    private static final double DESCRIPTION_MATCH_BOOST = 0.05;
    private static final double UPLOADER_MATCH_BOOST = 0.05;
    private static final int MAX_TSQUERY_TOKENS = 5;
    private static final Map<String, List<String>> SYNONYMS = Map.ofEntries(
            Map.entry("volkswagen", List.of("vw")),
            Map.entry("vw", List.of("volkswagen"))
    );

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataMapper fileMetadataMapper;
    @Value("${search.embedding.max-distance:0.6}")
    private double maxEmbeddingDistance;
    @Value("${search.embedding.strict-max-distance:0.35}")
    private double strictEmbeddingDistance;

    @GetMapping("/videos")
    @Operation(summary = "Р'Р?С?С'С?РчР?Р?РёР№ РїР?РёС?Рє Р?РёР?РчР?")
    public PageResponse<FileMetadataDto> searchVideos(@RequestParam("vec") String queryVecLiteral,
                                                      @RequestParam(value = "q", defaultValue = "") String query,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return searchVideosInternal(queryVecLiteral, query, page, size);
    }

    @PostMapping("/videos")
    @Operation(summary = "Internal search videos (POST)")
    public PageResponse<FileMetadataDto> searchVideosPost(@RequestBody SearchVideosRequest request) {
        if (request == null) {
            return new PageResponse<>(List.of(), 0, 0, 0);
        }
        int page = request.page() == null ? 0 : request.page();
        int size = request.size() == null ? 10 : request.size();
        return searchVideosInternal(request.vec(), request.q(), page, size);
    }

    private PageResponse<FileMetadataDto> searchVideosInternal(String queryVecLiteral,
                                                               String query,
                                                               int page,
                                                               int size) {
        if (size <= 0) {
            return new PageResponse<>(List.of(), page, size, 0);
        }

        ExpandedQuery expanded = expandQuery(query);
        String normalized = expanded.base();
        if (normalized.isBlank()) {
            if (queryVecLiteral == null || queryVecLiteral.isBlank()) {
                return new PageResponse<>(List.of(), page, size, 0);
            }
            Pageable pageable = PageRequest.of(page, size);
            Page<FileMetadataEntity> result = this.fileMetadataRepository.searchByEmbedding(queryVecLiteral, maxEmbeddingDistance, pageable);
            return new PageResponse<>(
                    result.getContent().stream().map(fileMetadataMapper::toDto).toList(),
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements()
            );
        }
        if (queryVecLiteral == null || queryVecLiteral.isBlank()) {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileMetadataEntity> result = searchByText(expanded, pageable, true);
            return new PageResponse<>(
                    result.getContent().stream().map(fileMetadataMapper::toDto).toList(),
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements()
            );
        }

        HybridResult hybrid = searchHybrid(queryVecLiteral, expanded, page, size);
        return new PageResponse<>(
                hybrid.items().stream().map(fileMetadataMapper::toDto).toList(),
                page,
                size,
                hybrid.total()
        );
    }

    @GetMapping("/videos/suggest")
    @Operation(summary = "Р'Р?С?С'С?РчР?Р?РёРч РїР?Р?С?РєР°Р·РєРё РїР?РёС?РєР°")
    public List<FileMetadataDto> suggest(@RequestParam("vec") String queryVecLiteral,
                                         @RequestParam(value = "q", defaultValue = "") String query,
                                         @RequestParam(defaultValue = "5") int limit) {
        return suggestInternal(queryVecLiteral, query, limit);
    }

    @PostMapping("/videos/suggest")
    @Operation(summary = "Internal suggest (POST)")
    public List<FileMetadataDto> suggestPost(@RequestBody SearchSuggestRequest request) {
        if (request == null) {
            return List.of();
        }
        int limit = request.limit() == null ? 5 : request.limit();
        return suggestInternal(request.vec(), request.q(), limit);
    }

    private List<FileMetadataDto> suggestInternal(String queryVecLiteral,
                                                  String query,
                                                  int limit) {
        if (limit <= 0) {
            return List.of();
        }
        ExpandedQuery expanded = expandQuery(query);
        String normalized = expanded.base();
        if (normalized.isBlank()) {
            if (queryVecLiteral == null || queryVecLiteral.isBlank()) {
                return List.of();
            }
            Page<FileMetadataEntity> page = this.fileMetadataRepository.searchByEmbedding(queryVecLiteral, maxEmbeddingDistance, PageRequest.of(0, limit));
            return page.getContent().stream().map(fileMetadataMapper::toDto).toList();
        }
        String tsQuery = buildTsQuery(expanded, true);
        if (tsQuery.isBlank()) {
            return List.of();
        }
        Page<FileMetadataEntity> textPage = this.fileMetadataRepository.searchByTextPrefix(tsQuery, PageRequest.of(0, limit));
        return textPage.getContent().stream().map(fileMetadataMapper::toDto).toList();
    }

    private HybridResult searchHybrid(String queryVecLiteral, ExpandedQuery expanded, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int candidateLimit = Math.min((safePage + 1) * safeSize * HYBRID_MULTIPLIER, HYBRID_MAX_CANDIDATES);
        Page<FileMetadataEntity> vectorPage = this.fileMetadataRepository.searchByEmbedding(
                queryVecLiteral,
                maxEmbeddingDistance,
                PageRequest.of(0, candidateLimit));
        Page<FileMetadataEntity> textPage = searchByText(expanded, PageRequest.of(0, candidateLimit), true);
        if (textPage.getTotalElements() == 0) {
            double strictDistance = Math.min(maxEmbeddingDistance, strictEmbeddingDistance);
            if (strictDistance <= 0) {
                return new HybridResult(List.of(), 0);
            }
            Page<FileMetadataEntity> strictVectorPage = this.fileMetadataRepository.searchByEmbedding(
                    queryVecLiteral,
                    strictDistance,
                    PageRequest.of(0, candidateLimit));
            List<FileMetadataEntity> ranked = filterByMinimalText(strictVectorPage.getContent(), expanded);
            if (ranked.isEmpty()) {
                return new HybridResult(List.of(), 0);
            }
            int from = safePage * safeSize;
            int to = Math.min(from + safeSize, ranked.size());
            List<FileMetadataEntity> slice = from >= ranked.size()
                    ? Collections.emptyList()
                    : ranked.subList(from, to);
            return new HybridResult(slice, ranked.size());
        }

        List<FileMetadataEntity> ranked = mergeAndRank(vectorPage.getContent(), textPage.getContent(), expanded.base());
        int from = safePage * safeSize;
        int to = Math.min(from + safeSize, ranked.size());
        List<FileMetadataEntity> slice = from >= ranked.size()
                ? Collections.emptyList()
                : ranked.subList(from, to);
        long total = textPage.getTotalElements() > 0
                ? textPage.getTotalElements()
                : vectorPage.getTotalElements();
        return new HybridResult(slice, total);
    }

    private Page<FileMetadataEntity> searchByText(ExpandedQuery expanded, Pageable pageable, boolean prefix) {
        String tsQuery = buildTsQuery(expanded, prefix);
        if (tsQuery.isBlank()) {
            return Page.empty(pageable);
        }
        return this.fileMetadataRepository.searchByTextPrefix(tsQuery, pageable);
    }

    private List<FileMetadataEntity> mergeAndRank(List<FileMetadataEntity> vectorHits,
                                                  List<FileMetadataEntity> textHits,
                                                  String query) {
        List<FileMetadataEntity> vectorList = vectorHits == null ? List.of() : vectorHits;
        List<FileMetadataEntity> textList = textHits == null ? List.of() : textHits;
        Map<String, FileMetadataEntity> merged = new LinkedHashMap<>();
        if (!textList.isEmpty()) {
            for (FileMetadataEntity entity : textList) {
                putIfPresent(merged, entity);
            }
        } else {
            for (FileMetadataEntity entity : vectorList) {
                putIfPresent(merged, entity);
            }
        }
        Map<String, Integer> vectorRanks = buildRankMap(vectorList);
        Map<String, Integer> textRanks = buildRankMap(textList);
        int vectorSize = vectorList.size();
        int textSize = textList.size();
        String normalized = normalizeQuery(query);
        boolean shortQuery = normalized.length() > 0 && normalized.length() <= SHORT_QUERY_MAX_LEN;
        double vectorWeight = shortQuery ? SHORT_VECTOR_WEIGHT : VECTOR_WEIGHT;
        double textWeight = shortQuery ? SHORT_TEXT_WEIGHT : TEXT_WEIGHT;

        List<ScoredEntity> scored = new ArrayList<>();
        for (Map.Entry<String, FileMetadataEntity> entry : merged.entrySet()) {
            String key = entry.getKey();
            FileMetadataEntity entity = entry.getValue();
            double vectorScore = rankScore(vectorRanks.get(key), vectorSize);
            double textScore = rankScore(textRanks.get(key), textSize);
            double boost = boostScore(entity, normalized, shortQuery);
            double score = vectorWeight * vectorScore + textWeight * textScore + boost;
            scored.add(new ScoredEntity(entity, score, vectorScore));
        }
        scored.sort(Comparator.comparingDouble(ScoredEntity::score)
                .thenComparingDouble(ScoredEntity::vectorScore)
                .reversed());

        List<FileMetadataEntity> result = new ArrayList<>(scored.size());
        for (ScoredEntity item : scored) {
            result.add(item.entity());
        }
        return result;
    }

    private List<FileMetadataEntity> filterByMinimalText(List<FileMetadataEntity> candidates, ExpandedQuery expanded) {
        if (candidates == null || candidates.isEmpty() || expanded == null) {
            return List.of();
        }
        String base = expanded.base();
        List<List<String>> groups = expanded.groups();
        if ((base == null || base.isBlank()) && (groups == null || groups.isEmpty())) {
            return List.copyOf(candidates);
        }
        List<FileMetadataEntity> filtered = new ArrayList<>();
        for (FileMetadataEntity entity : candidates) {
            if (matchesAnyToken(entity, base, groups)) {
                filtered.add(entity);
            }
        }
        return filtered;
    }

    private boolean matchesAnyToken(FileMetadataEntity entity, String base, List<List<String>> groups) {
        if (entity == null) {
            return false;
        }
        String combined = String.join(" ",
                safeLower(entity.getName()),
                safeLower(entity.getDescription()),
                safeLower(entity.getUploaderName()));
        if (base != null && !base.isBlank() && combined.contains(base)) {
            return true;
        }
        if (groups == null || groups.isEmpty()) {
            return false;
        }
        for (List<String> group : groups) {
            if (group == null) {
                continue;
            }
            for (String token : group) {
                if (token == null) {
                    continue;
                }
                String trimmed = token.trim();
                if (trimmed.length() < 2) {
                    continue;
                }
                if (combined.contains(trimmed)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void putIfPresent(Map<String, FileMetadataEntity> merged, FileMetadataEntity entity) {
        if (entity == null) {
            return;
        }
        String key = entity.getId() != null ? "id:" + entity.getId()
                : entity.getUuid() != null ? "uuid:" + entity.getUuid()
                : null;
        if (key == null) {
            return;
        }
        merged.putIfAbsent(key, entity);
    }

    private Map<String, Integer> buildRankMap(List<FileMetadataEntity> items) {
        Map<String, Integer> ranks = new HashMap<>();
        if (items == null || items.isEmpty()) {
            return ranks;
        }
        int rank = 0;
        for (FileMetadataEntity entity : items) {
            String key = entity.getId() != null ? "id:" + entity.getId()
                    : entity.getUuid() != null ? "uuid:" + entity.getUuid()
                    : null;
            if (key != null && !ranks.containsKey(key)) {
                ranks.put(key, rank);
            }
            rank++;
        }
        return ranks;
    }

    private double rankScore(Integer rank, int total) {
        if (rank == null || total <= 0) {
            return 0d;
        }
        return 1d - (rank / (double) total);
    }

    private double boostScore(FileMetadataEntity entity, String query, boolean shortQuery) {
        if (entity == null || query == null || query.isBlank()) {
            return 0d;
        }
        double boost = 0d;
        String name = safeLower(entity.getName());
        String description = safeLower(entity.getDescription());
        String uploader = safeLower(entity.getUploaderName());
        if (name.equals(query)) {
            boost += EXACT_MATCH_BOOST;
        }
        if (name.startsWith(query)) {
            boost += PREFIX_MATCH_BOOST;
        }
        if (name.contains(query)) {
            boost += NAME_MATCH_BOOST;
        }
        if (!description.isEmpty() && description.contains(query)) {
            boost += DESCRIPTION_MATCH_BOOST;
        }
        if (!uploader.isEmpty() && uploader.contains(query)) {
            boost += UPLOADER_MATCH_BOOST;
        }
        if (shortQuery) {
            String queryToken = firstToken(query);
            if (!queryToken.isBlank()) {
                boost += tokenMatchBoost(name, queryToken, SHORT_TOKEN_NAME_BOOST);
                boost += tokenMatchBoost(description, queryToken, SHORT_TOKEN_DESC_BOOST);
                boost += tokenMatchBoost(uploader, queryToken, SHORT_TOKEN_UPLOADER_BOOST);
            }
        }
        return boost;
    }

    private double tokenMatchBoost(String text, String queryToken, double weight) {
        if (text == null || text.isBlank() || queryToken == null || queryToken.isBlank()) {
            return 0d;
        }
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return 0d;
        }
        for (String token : tokens) {
            if (token.equals(queryToken)) {
                return weight;
            }
            if (token.startsWith(queryToken)
                    && token.length() <= queryToken.length() + SHORT_TOKEN_PREFIX_MAX_EXTRA) {
                return weight * SHORT_TOKEN_PREFIX_RATIO;
            }
        }
        return 0d;
    }

    private String firstToken(String query) {
        List<String> tokens = tokenize(query);
        if (tokens.isEmpty()) {
            return "";
        }
        return tokens.get(0);
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private ExpandedQuery expandQuery(String query) {
        String base = normalizeQuery(query);
        if (base.isBlank()) {
            return new ExpandedQuery("", List.of());
        }
        List<String> tokens = tokenize(base);
        if (tokens.isEmpty()) {
            return new ExpandedQuery(base, List.of());
        }
        if (tokens.size() > MAX_TSQUERY_TOKENS) {
            tokens = tokens.subList(0, MAX_TSQUERY_TOKENS);
        }
        List<List<String>> groups = new ArrayList<>();
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            Set<String> group = new LinkedHashSet<>();
            group.add(token);
            List<String> synonyms = SYNONYMS.get(token);
            if (synonyms != null) {
                group.addAll(synonyms);
            }
            groups.add(new ArrayList<>(group));
        }
        return new ExpandedQuery(base, groups);
    }

    private List<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String cleaned = query.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").trim();
        if (cleaned.isBlank()) {
            return List.of();
        }
        String[] parts = cleaned.split("\\s+");
        List<String> tokens = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!part.isBlank()) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    private String buildTsQuery(ExpandedQuery expanded, boolean prefix) {
        if (expanded == null || expanded.groups().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (List<String> group : expanded.groups()) {
            if (group == null || group.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" & ");
            }
            if (group.size() == 1) {
                sb.append(group.get(0));
                if (prefix) {
                    sb.append(":*");
                }
                continue;
            }
            sb.append("(");
            boolean first = true;
            for (String token : group) {
                if (token == null || token.isBlank()) {
                    continue;
                }
                if (!first) {
                    sb.append(" | ");
                }
                sb.append(token);
                if (prefix) {
                    sb.append(":*");
                }
                first = false;
            }
            sb.append(")");
        }
        return sb.toString();
    }

    private String safeLower(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private record HybridResult(List<FileMetadataEntity> items, long total) {
    }

    private record ScoredEntity(FileMetadataEntity entity, double score, double vectorScore) {
    }

    private record ExpandedQuery(String base, List<List<String>> groups) {
    }
}
