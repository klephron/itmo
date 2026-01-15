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
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/internal/search")
@RequiredArgsConstructor
@Tag(name = "Internal-Search", description = "Р'Р?С?С'С?РчР?Р?РёР№ API Р?Р>С? РїР?РёС?РєР° (РёС?РїР?Р>С?Р·С?РчС'С?С? search-service)")
public class SearchInternalController {

    private static final int HYBRID_MULTIPLIER = 3;
    private static final int HYBRID_MAX_CANDIDATES = 200;
    private static final double VECTOR_WEIGHT = 0.7;
    private static final double TEXT_WEIGHT = 0.3;
    private static final double EXACT_MATCH_BOOST = 0.4;
    private static final double PREFIX_MATCH_BOOST = 0.2;
    private static final double NAME_MATCH_BOOST = 0.1;
    private static final double DESCRIPTION_MATCH_BOOST = 0.05;
    private static final double UPLOADER_MATCH_BOOST = 0.05;

    private final FileMetadataRepository fileMetadataRepository;
    private final FileMetadataMapper fileMetadataMapper;
    @Value("${search.embedding.max-distance:0.6}")
    private double maxEmbeddingDistance;

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
        if (queryVecLiteral == null || queryVecLiteral.isBlank()) {
            return new PageResponse<>(List.of(), page, size, 0);
        }
        if (size <= 0) {
            return new PageResponse<>(List.of(), page, size, 0);
        }

        String normalized = normalizeQuery(query);
        if (normalized.isBlank()) {
            Pageable pageable = PageRequest.of(page, size);
            Page<FileMetadataEntity> result = this.fileMetadataRepository.searchByEmbedding(queryVecLiteral, maxEmbeddingDistance, pageable);
            return new PageResponse<>(
                    result.getContent().stream().map(fileMetadataMapper::toDto).toList(),
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements()
            );
        }

        HybridResult hybrid = searchHybrid(queryVecLiteral, normalized, page, size);
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
        String normalized = normalizeQuery(query);
        if (normalized.isBlank()) {
            if (queryVecLiteral == null || queryVecLiteral.isBlank()) {
                return List.of();
            }
            Page<FileMetadataEntity> page = this.fileMetadataRepository.searchByEmbedding(queryVecLiteral, maxEmbeddingDistance, PageRequest.of(0, limit));
            return page.getContent().stream().map(fileMetadataMapper::toDto).toList();
        }
        String tsQuery = buildPrefixTsQuery(normalized);
        if (tsQuery.isBlank()) {
            return List.of();
        }
        Page<FileMetadataEntity> textPage = this.fileMetadataRepository.searchByTextPrefix(tsQuery, PageRequest.of(0, limit));
        return textPage.getContent().stream().map(fileMetadataMapper::toDto).toList();
    }

    private HybridResult searchHybrid(String queryVecLiteral, String query, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int candidateLimit = Math.min((safePage + 1) * safeSize * HYBRID_MULTIPLIER, HYBRID_MAX_CANDIDATES);
        Page<FileMetadataEntity> vectorPage = this.fileMetadataRepository.searchByEmbedding(
                queryVecLiteral,
                maxEmbeddingDistance,
                PageRequest.of(0, candidateLimit));
        Page<FileMetadataEntity> textPage = this.fileMetadataRepository.searchByText(
                query,
                PageRequest.of(0, candidateLimit));

        List<FileMetadataEntity> ranked = mergeAndRank(vectorPage.getContent(), textPage.getContent(), query);
        int from = safePage * safeSize;
        int to = Math.min(from + safeSize, ranked.size());
        List<FileMetadataEntity> slice = from >= ranked.size()
                ? Collections.emptyList()
                : ranked.subList(from, to);
        long total = Math.max(vectorPage.getTotalElements(), textPage.getTotalElements());
        return new HybridResult(slice, total);
    }

    private List<FileMetadataEntity> mergeAndRank(List<FileMetadataEntity> vectorHits,
                                                  List<FileMetadataEntity> textHits,
                                                  String query) {
        List<FileMetadataEntity> vectorList = vectorHits == null ? List.of() : vectorHits;
        List<FileMetadataEntity> textList = textHits == null ? List.of() : textHits;
        Map<String, FileMetadataEntity> merged = new LinkedHashMap<>();
        for (FileMetadataEntity entity : vectorList) {
            putIfPresent(merged, entity);
        }
        for (FileMetadataEntity entity : textList) {
            putIfPresent(merged, entity);
        }
        Map<String, Integer> vectorRanks = buildRankMap(vectorList);
        Map<String, Integer> textRanks = buildRankMap(textList);
        int vectorSize = vectorList.size();
        int textSize = textList.size();
        String normalized = normalizeQuery(query);

        List<ScoredEntity> scored = new ArrayList<>();
        for (Map.Entry<String, FileMetadataEntity> entry : merged.entrySet()) {
            String key = entry.getKey();
            FileMetadataEntity entity = entry.getValue();
            double vectorScore = rankScore(vectorRanks.get(key), vectorSize);
            double textScore = rankScore(textRanks.get(key), textSize);
            double boost = boostScore(entity, normalized);
            double score = VECTOR_WEIGHT * vectorScore + TEXT_WEIGHT * textScore + boost;
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

    private double boostScore(FileMetadataEntity entity, String query) {
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
        return boost;
    }

    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private String buildPrefixTsQuery(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String cleaned = query.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", " ").trim();
        if (cleaned.isBlank()) {
            return "";
        }
        String[] parts = cleaned.split("\\s+");
        int maxParts = Math.min(parts.length, 5);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxParts; i++) {
            String token = parts[i];
            if (token.isBlank()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(" & ");
            }
            sb.append(token).append(":*");
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
}
