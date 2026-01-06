package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.WatchProgressSummaryDto;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.FollowQueryApi;
import ru.itmo.zavar.brainstop.common.api.profile.UserProfilePublicApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.common.infra.VectorConverter;
import ru.itmo.zavar.brainstop.interactions.client.InteractionsInternalClient;
import ru.itmo.zavar.brainstop.video.mapper.FeedMapperContext;
import ru.itmo.zavar.brainstop.video.mapper.FileMetadataToFeedMapper;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private static final double WATCH_THRESHOLD = 0.5;
    private static final double WATCH_SCORE_WEIGHT = 0.2;
    private static final int DEBUG_SCORE_LIMIT = 20;

    private final FileMetadataRepository fileMetadataRepository;
    private final UserProfilePublicApi userProfilePublicApi;
    private final LikeQueryApi likeQueryApi;
    private final FollowQueryApi followQueryApi;
    private final InteractionsInternalClient interactionsInternalClient;
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
        Map<Long, Double> watchRatios = resolveWatchRatios(currentUserId, candidateVideoIds);

        List<WeightedVector> preferenceVectors = recentEmbedded.getContent().stream()
                .map(f -> {
                    double weight = 0d;
                    if (likedRecent.contains(f.getId())) {
                        weight += 1d;
                    }
                    if (followingIds.contains(f.getUploaderId())) {
                        weight += 1d;
                    }
                    Double watchRatio = watchRatios.get(f.getId());
                    if (watchRatio != null && watchRatio >= WATCH_THRESHOLD) {
                        weight += watchRatio;
                    }
                    if (weight <= 0d || f.getCombinedEmbedding() == null) {
                        return null;
                    }
                    return new WeightedVector(f.getCombinedEmbedding(), weight);
                })
                .filter(Objects::nonNull)
                .toList();

        float[] preference = weightedAverage(preferenceVectors);
        if (preference == null) {
            // fallback to chronological if no signal
            Page<FileMetadataEntity> page = fileMetadataRepository.findAll(PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "uploadedAt")));
            List<FileMetadataEntity> filtered = excludeViewed(currentUserId, page.getContent());
            if (log.isDebugEnabled()) {
                log.debug("Feed scoring: no preference vectors for user {}, using chronological fallback", currentUserId);
            }
            return new PageImpl<>(filtered, page.getPageable(), page.getTotalElements());
        }

        List<WeightedVector> watchVectors = recentEmbedded.getContent().stream()
                .map(f -> {
                    Double watchRatio = watchRatios.get(f.getId());
                    if (watchRatio == null || watchRatio < WATCH_THRESHOLD || f.getCombinedEmbedding() == null) {
                        return null;
                    }
                    return new WeightedVector(f.getCombinedEmbedding(), watchRatio);
                })
                .filter(Objects::nonNull)
                .toList();
        float[] watchPreference = weightedAverage(watchVectors);

        String literal = vectorConverter.convertToDatabaseColumn(preference);
        // fetch a ranked sample, then re-rank with engagement signals and paginate manually
        Page<FileMetadataEntity> sample = fileMetadataRepository.searchByEmbedding(literal,
                PageRequest.of(0, 200));

        List<FileMetadataEntity> ranked = new ArrayList<>(sample.getContent());
        ranked.sort(Comparator.comparingDouble((FileMetadataEntity e) ->
                        score(preference, watchPreference, e))
                .reversed());

        ranked = excludeViewed(currentUserId, ranked);
        logScoreDebug(currentUserId, preference, watchPreference, ranked);
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), ranked.size());
        if (from >= ranked.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, ranked.size());
        }
        return new PageImpl<>(ranked.subList(from, to), pageable, ranked.size());
    }

    private List<FileMetadataEntity> excludeViewed(Long userId, List<FileMetadataEntity> items) {
        if (userId == null || items == null || items.isEmpty()) {
            return items;
        }
        Set<Long> ids = items.stream()
                .map(FileMetadataEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return items;
        }
        List<Long> viewedList = interactionsInternalClient.getViewedVideoIds(new VideoIdsRequest(userId, ids));
        if (viewedList == null || viewedList.isEmpty()) {
            return items;
        }
        Set<Long> viewed = new HashSet<>(viewedList);
        return items.stream()
                .filter(item -> item.getId() == null || !viewed.contains(item.getId()))
                .toList();
    }

    private float[] weightedAverage(List<WeightedVector> vectors) {
        if (vectors == null || vectors.isEmpty()) {
            return null;
        }
        int len = vectors.get(0).vector().length;
        float[] sum = new float[len];
        double weightSum = 0d;
        for (WeightedVector v : vectors) {
            if (v == null || v.vector() == null || v.vector().length != len || v.weight() <= 0d) {
                continue;
            }
            weightSum += v.weight();
            for (int i = 0; i < len; i++) {
                sum[i] += v.vector()[i] * v.weight();
            }
        }
        if (weightSum <= 0d) {
            return null;
        }
        float[] avg = new float[len];
        for (int i = 0; i < len; i++) {
            avg[i] = (float) (sum[i] / weightSum);
        }
        return avg;
    }

    private double score(float[] preference, float[] watchPreference, FileMetadataEntity entity) {
        double sim = cosineSimilarity(preference, entity.getCombinedEmbedding());
        double watchSim = cosineSimilarity(watchPreference, entity.getCombinedEmbedding());
        double likes = Math.log1p(entity.getLikesCount());
        double views = Math.log1p(entity.getViewsCount());
        // tuneable weights for engagement
        return sim + WATCH_SCORE_WEIGHT * watchSim + 0.3 * likes + 0.2 * views;
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

    private void logScoreDebug(Long userId, float[] preference, float[] watchPreference, List<FileMetadataEntity> ranked) {
        if (!log.isDebugEnabled()) {
            return;
        }
        int limit = Math.min(DEBUG_SCORE_LIMIT, ranked.size());
        for (int i = 0; i < limit; i++) {
            FileMetadataEntity entity = ranked.get(i);
            double sim = cosineSimilarity(preference, entity.getCombinedEmbedding());
            double watchSim = cosineSimilarity(watchPreference, entity.getCombinedEmbedding());
            long likesCount = entity.getLikesCount() != null ? entity.getLikesCount() : 0L;
            long viewsCount = entity.getViewsCount() != null ? entity.getViewsCount() : 0L;
            double likes = Math.log1p(likesCount);
            double views = Math.log1p(viewsCount);
            double score = sim + WATCH_SCORE_WEIGHT * watchSim + 0.3 * likes + 0.2 * views;
            log.debug(
                    "Feed score user={} rank={} videoId={} uuid={} sim={} watchSim={} likes={} views={} score={}",
                    userId,
                    i,
                    entity.getId(),
                    entity.getUuid(),
                    String.format(Locale.ROOT, "%.4f", sim),
                    String.format(Locale.ROOT, "%.4f", watchSim),
                    String.format(Locale.ROOT, "%.4f", likes),
                    String.format(Locale.ROOT, "%.4f", views),
                    String.format(Locale.ROOT, "%.4f", score)
            );
        }
    }

    private Map<Long, Double> resolveWatchRatios(Long userId, Set<Long> videoIds) {
        if (userId == null || videoIds == null || videoIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WatchProgressSummaryDto> progress = interactionsInternalClient.getWatchProgress(
                new VideoIdsRequest(userId, videoIds));
        if (progress == null || progress.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Double> ratios = new HashMap<>();
        for (WatchProgressSummaryDto p : progress) {
            if (p == null || p.videoId() == null || p.durationSeconds() == null || p.durationSeconds() <= 0) {
                continue;
            }
            long total = p.totalWatchedSeconds() != null ? p.totalWatchedSeconds() : 0L;
            double ratio = total / (double) p.durationSeconds();
            if (ratio <= 0d) {
                continue;
            }
            ratios.put(p.videoId(), Math.min(1d, ratio));
        }
        return ratios;
    }

    private record WeightedVector(float[] vector, double weight) {
    }
}
