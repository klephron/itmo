package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.common.infra.VectorConverter;
import ru.itmo.zavar.brainstop.video.mapper.FeedMapperContext;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToFeedMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final FileMetadataRepository fileMetadataRepository;
    private final UserProfilePublicApi userProfilePublicApi;
    private final LikeQueryApi likeQueryApi;
    private final FollowQueryApi followQueryApi;
    private final FileMetadataToFeedMapper mapper;
    private final VectorConverter vectorConverter = new VectorConverter();

    public Page<FeedVideoDto> getFeed(Long currentUserId, Pageable pageable) {

        Page<FileMetadataEntity> page = resolvePage(currentUserId, pageable);

        List<FileMetadataEntity> entities = page.getContent();

        Set<Long> uploaderIds = entities.stream()
                .map(FileMetadataEntity::getUploaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> videoIds = entities.stream()
                .map(FileMetadataEntity::getId)
                .collect(Collectors.toSet());

        Map<Long, String> avatarByUserId = uploaderIds.stream()
                .map(id -> userProfilePublicApi.findUserSummary(id)
                        .filter(summary -> summary.avatarUrl() != null && !summary.avatarUrl().isBlank())
                        .map(summary -> Map.entry(id, summary.avatarUrl()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<Long> likedByMe;
        Set<Long> followingIds;

        if (currentUserId != null) {
            likedByMe = new HashSet<>(this.likeQueryApi.findLikedVideoIds(currentUserId, videoIds));
            followingIds = this.followQueryApi.getFollowingWithin(currentUserId, uploaderIds);
        } else {
            likedByMe = Collections.emptySet();
            followingIds = Collections.emptySet();
        }

        FeedMapperContext mapperContext = new FeedMapperContext(
                currentUserId,
                avatarByUserId,
                likedByMe,
                followingIds
        );

        List<FeedVideoDto> feedItems = this.mapper.toDto(entities, mapperContext);

        return new PageImpl<>(feedItems, page.getPageable(), page.getTotalElements());
    }

    private Page<FileMetadataEntity> resolvePage(Long currentUserId, Pageable pageable) {
        // Anonymous users: simple reverse-chronological feed
        if (currentUserId == null) {
            return fileMetadataRepository.findAll(PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "uploadedAt")));
        }

        // Take a larger sample of embedded videos to infer interests from likes + followings
        Page<FileMetadataEntity> recentEmbedded = fileMetadataRepository.findByCombinedEmbeddingIsNotNull(
                PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "uploadedAt")));

        Set<Long> candidateUploaderIds = recentEmbedded.getContent().stream()
                .map(FileMetadataEntity::getUploaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> candidateVideoIds = recentEmbedded.getContent().stream()
                .map(FileMetadataEntity::getId)
                .collect(Collectors.toSet());

        Set<Long> followingIds = followQueryApi.getFollowingWithin(currentUserId, candidateUploaderIds);
        Set<Long> likedRecent = new HashSet<>(likeQueryApi.findLikedVideoIds(currentUserId, candidateVideoIds));

        List<float[]> preferenceVectors = recentEmbedded.getContent().stream()
                .filter(f -> f.getCombinedEmbedding() != null &&
                        (likedRecent.contains(f.getId()) || followingIds.contains(f.getUploaderId())))
                .map(FileMetadataEntity::getCombinedEmbedding)
                .toList();

        float[] preference = average(preferenceVectors);
        if (preference == null) {
            // fallback to chronological if no signal
            return fileMetadataRepository.findAll(PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "uploadedAt")));
        }

        String literal = vectorConverter.convertToDatabaseColumn(preference);
        // fetch a ranked sample, then re-rank with engagement signals and paginate manually
        Page<FileMetadataEntity> sample = fileMetadataRepository.searchByEmbedding(literal,
                PageRequest.of(0, 200));

        List<FileMetadataEntity> ranked = new ArrayList<>(sample.getContent());
        ranked.sort(Comparator.comparingDouble((FileMetadataEntity e) ->
                        score(preference, e))
                .reversed());

        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), ranked.size());
        if (from >= ranked.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, ranked.size());
        }
        return new PageImpl<>(ranked.subList(from, to), pageable, ranked.size());
    }

    private float[] average(List<float[]> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return null;
        }
        int len = vectors.get(0).length;
        float[] sum = new float[len];
        int used = 0;
        for (float[] v : vectors) {
            if (v == null || v.length != len) {
                continue;
            }
            used++;
            for (int i = 0; i < len; i++) {
                sum[i] += v[i];
            }
        }
        if (used == 0) {
            return null;
        }
        float[] avg = new float[len];
        for (int i = 0; i < len; i++) {
            avg[i] = sum[i] / used;
        }
        return avg;
    }

    private double score(float[] preference, FileMetadataEntity entity) {
        double sim = cosineSimilarity(preference, entity.getCombinedEmbedding());
        double likes = Math.log1p(entity.getLikesCount());
        double views = Math.log1p(entity.getViewsCount());
        // tuneable weights for engagement
        return sim + 0.3 * likes + 0.2 * views;
    }

    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) {
            return 0d;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0d;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
