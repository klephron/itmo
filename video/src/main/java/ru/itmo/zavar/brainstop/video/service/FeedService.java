package ru.itmo.zavar.brainstop.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoCommentsCountDto;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoUuidsRequest;
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
    private static final int CANDIDATE_SAMPLE_SIZE = 200;

    private final FileMetadataRepository fileMetadataRepository;
    private final UserProfilePublicApi userProfilePublicApi;
    private final LikeQueryApi likeQueryApi;
    private final FollowQueryApi followQueryApi;
    private final InteractionsInternalClient interactionsInternalClient;
    private final FileMetadataToFeedMapper mapper;
    private final VectorConverter vectorConverter = new VectorConverter();
    @Value("${search.embedding.max-distance:0.6}")
    private double maxEmbeddingDistance;
    @Value("${search.embedding.relaxed-max-distance:0.9}")
    private double relaxedMaxEmbeddingDistance;

    public Page<FeedVideoDto> getFeed(Long currentUserId, Pageable pageable) {

        if (log.isDebugEnabled()) {
            log.debug("Feed request userId={}, pageNumber={}, pageSize={}", currentUserId, pageable.getPageNumber(), pageable.getPageSize());
        }

        Page<FileMetadataEntity> page = resolvePage(currentUserId, pageable);

        List<FileMetadataEntity> entities = page.getContent();
        if (log.isDebugEnabled()) {
            log.debug("Feed raw page content (after resolvePage) size={} ids={}", entities.size(),
                    entities.stream().map(FileMetadataEntity::getId).toList());
        }

        Set<Long> uploaderIds = entities.stream()
                .map(FileMetadataEntity::getUploaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> videoIds = entities.stream()
                .map(FileMetadataEntity::getId)
                .collect(Collectors.toSet());
        if (log.isDebugEnabled()) {
            log.debug("Feed entities after resolvePage (ids={}, uuids={})",
                    entities.stream().map(FileMetadataEntity::getId).toList(),
                    entities.stream().map(FileMetadataEntity::getUuid).toList());
        }

        Map<Long, String> avatarByUserId = uploaderIds.stream()
                .map(id -> userProfilePublicApi.findUserSummary(id)
                        .filter(summary -> summary.avatarUrl() != null && !summary.avatarUrl().isBlank())
                        .map(summary -> Map.entry(id, summary.avatarUrl()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<UUID, Long> commentsCountByUuid = resolveCommentsCountByUuid(entities);

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
                followingIds,
                commentsCountByUuid
        );

        List<FeedVideoDto> feedItems = this.mapper.toDto(entities, mapperContext);

        return new PageImpl<>(feedItems, page.getPageable(), page.getTotalElements());
    }

    public Optional<FeedVideoDto> getFeedVideoByUuid(UUID videoUuid, Long currentUserId) {
        if (videoUuid == null) {
            return Optional.empty();
        }

        Optional<FileMetadataEntity> entityOpt = fileMetadataRepository.findByUuid(videoUuid);
        if (entityOpt.isEmpty()) {
            return Optional.empty();
        }

        FileMetadataEntity entity = entityOpt.get();
        Long uploaderId = entity.getUploaderId();
        Long videoId = entity.getId();

        // avatar автора
        Map<Long, String> avatarByUserId = Collections.emptyMap();
        if (uploaderId != null) {
            avatarByUserId = userProfilePublicApi.findUserSummary(uploaderId)
                    .filter(s -> s.avatarUrl() != null && !s.avatarUrl().isBlank())
                    .map(s -> Map.of(uploaderId, s.avatarUrl()))
                    .orElse(Collections.emptyMap());
        }

        // комментарии
        Map<UUID, Long> commentsCountByUuid = resolveCommentsCountByUuid(List.of(entity));

        Set<Long> likedByMe;
        Set<Long> followingIds;
        if (currentUserId != null && videoId != null) {
            likedByMe = new HashSet<>(likeQueryApi.findLikedVideoIds(currentUserId, Set.of(videoId)));
            followingIds = uploaderId != null
                    ? followQueryApi.getFollowingWithin(currentUserId, Set.of(uploaderId))
                    : Collections.emptySet();
        } else {
            likedByMe = Collections.emptySet();
            followingIds = Collections.emptySet();
        }

        FeedMapperContext ctx = new FeedMapperContext(
                currentUserId,
                avatarByUserId,
                likedByMe,
                followingIds,
                commentsCountByUuid
        );
        FeedVideoDto dto = mapper.toDto(entity, ctx);

        return Optional.of(dto);
    }

    private Page<FileMetadataEntity> resolvePage(Long currentUserId, Pageable pageable) {
        // Anonymous users: simple reverse-chronological feed
        if (currentUserId == null) {
            if (log.isDebugEnabled()) {
                log.debug("Feed resolve: anonymous user, chronological order");
            }
            return fileMetadataRepository.findAll(PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "uploadedAt")));
        }

        // Take a larger sample of embedded videos to infer interests from likes + followings
        Page<FileMetadataEntity> recentEmbedded = fileMetadataRepository.findByCombinedEmbeddingIsNotNull(
                PageRequest.of(0, CANDIDATE_SAMPLE_SIZE, Sort.by(Sort.Direction.DESC, "uploadedAt")));

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

        if (log.isDebugEnabled()) {
            log.debug("Feed signals userId={}, recentEmbedded={}, following={}, liked={}, watched={}",
                    currentUserId, recentEmbedded.getTotalElements(), followingIds.size(), likedRecent.size(), watchRatios.size());
        }

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
            return buildChronologicalPage(currentUserId, pageable, "no preference vectors");
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
        if (log.isDebugEnabled()) {
            log.debug("Feed signals: preferenceVectors={}, watchVectors={}, preferenceNull={}, watchPreferenceNull={}",
                    preferenceVectors.size(), watchVectors.size(), preference == null, watchPreference == null);
        }

        String literal = vectorConverter.convertToDatabaseColumn(preference);
        Page<FileMetadataEntity> primarySample = fileMetadataRepository.searchByEmbedding(
                literal,
                maxEmbeddingDistance,
                PageRequest.of(0, CANDIDATE_SAMPLE_SIZE));
        List<FileMetadataEntity> primaryRanked = rankSample(currentUserId, preference, watchPreference, primarySample, "primary");

        Page<FileMetadataEntity> relaxedSample = fileMetadataRepository.searchByEmbedding(
                literal,
                relaxedMaxEmbeddingDistance,
                PageRequest.of(0, CANDIDATE_SAMPLE_SIZE));
        List<FileMetadataEntity> relaxedRanked = rankSample(currentUserId, preference, watchPreference, relaxedSample, "relaxed");

        List<FileMetadataEntity> chronological = buildChronologicalList(currentUserId, pageable, "combined");
        List<FileMetadataEntity> ranked = mergeStages(primaryRanked, relaxedRanked, chronological);

        if (ranked.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), ranked.size());
        if (log.isDebugEnabled()) {
            log.debug("Feed paging: rankedSize={}, from={}, to={}, pageNumber={}, pageSize={}",
                    ranked.size(), from, to, pageable.getPageNumber(), pageable.getPageSize());
        }
        if (from >= ranked.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, ranked.size());
        }
        List<FileMetadataEntity> slice = ranked.subList(from, to);
        if (log.isDebugEnabled()) {
            log.debug("Feed final slice ids={} uuids={}",
                    slice.stream().map(FileMetadataEntity::getId).toList(),
                    slice.stream().map(FileMetadataEntity::getUuid).toList());
        }
        return new PageImpl<>(slice, pageable, ranked.size());
    }

    private List<FileMetadataEntity> rankSample(Long currentUserId,
                                                float[] preference,
                                                float[] watchPreference,
                                                Page<FileMetadataEntity> sample,
                                                String stage) {
        if (log.isDebugEnabled()) {
            log.debug("Feed candidate distances (stage={}, userId={}) size={}", stage, currentUserId, sample.getTotalElements());
            for (FileMetadataEntity e : sample) {
                double sim = cosineSimilarity(preference, e.getCombinedEmbedding());
                double watchSim = cosineSimilarity(watchPreference, e.getCombinedEmbedding());
                double dist = 1.0 - sim;
                double watchDist = 1.0 - watchSim;
                log.debug("Candidate videoId={} uuid={} sim={} dist={} watchSim={} watchDist={}",
                        e.getId(),
                        e.getUuid(),
                        String.format(Locale.ROOT, "%.4f", sim),
                        String.format(Locale.ROOT, "%.4f", dist),
                        String.format(Locale.ROOT, "%.4f", watchSim),
                        String.format(Locale.ROOT, "%.4f", watchDist));
            }
        }

        if (sample.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Feed ranking stage={} is empty for user {}", stage, currentUserId);
            }
            return Collections.emptyList();
        }

        List<FileMetadataEntity> ranked = new ArrayList<>(sample.getContent());
        if (log.isDebugEnabled()) {
            log.debug("Feed sample before rerank (stage={}) ids={} uuids={}",
                    stage,
                    ranked.stream().map(FileMetadataEntity::getId).toList(),
                    ranked.stream().map(FileMetadataEntity::getUuid).toList());
        }
        ranked.sort(Comparator.comparingDouble((FileMetadataEntity e) ->
                        score(preference, watchPreference, e))
                .reversed());

        ranked = excludeViewed(currentUserId, ranked);
        if (ranked.isEmpty() && log.isDebugEnabled()) {
            log.debug("Feed scoring: ranked list empty after excluding viewed for user {}, stage={}, sampleSize={}",
                    currentUserId, stage, sample.getTotalElements());
        }
        logScoreDebug(currentUserId, preference, watchPreference, ranked);
        return ranked;
    }

    private Page<FileMetadataEntity> buildChronologicalPage(Long currentUserId, Pageable pageable, String reason) {
        List<FileMetadataEntity> filtered = buildChronologicalList(currentUserId, pageable, reason);
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), filtered.size());
        List<FileMetadataEntity> slice = from >= filtered.size()
                ? Collections.emptyList()
                : filtered.subList(from, to);
        return new PageImpl<>(slice, pageable, filtered.size());
    }

    private List<FileMetadataEntity> buildChronologicalList(Long currentUserId, Pageable pageable, String reason) {
        int needed = (pageable.getPageNumber() + 1) * pageable.getPageSize();
        int fetchSize = Math.max(needed + pageable.getPageSize(), CANDIDATE_SAMPLE_SIZE);
        Page<FileMetadataEntity> page = fileMetadataRepository.findAll(PageRequest.of(
                0,
                fetchSize,
                Sort.by(Sort.Direction.DESC, "uploadedAt")));
        List<FileMetadataEntity> filtered = excludeViewed(currentUserId, page.getContent());
        if (log.isDebugEnabled()) {
            log.debug("Feed chronological (reason={}): total={}, afterViewed={}",
                    reason, page.getTotalElements(), filtered.size());
        }
        return filtered;
    }

    private List<FileMetadataEntity> mergeStages(List<FileMetadataEntity> primary,
                                                 List<FileMetadataEntity> relaxed,
                                                 List<FileMetadataEntity> chronological) {
        Map<String, FileMetadataEntity> merged = new LinkedHashMap<>();
        appendStage(merged, primary);
        appendStage(merged, relaxed);
        appendStage(merged, chronological);
        if (log.isDebugEnabled()) {
            log.debug("Feed merge: primary={}, relaxed={}, chronological={}, merged={}",
                    primary.size(), relaxed.size(), chronological.size(), merged.size());
        }
        return new ArrayList<>(merged.values());
    }

    private void appendStage(Map<String, FileMetadataEntity> merged, List<FileMetadataEntity> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (FileMetadataEntity entity : items) {
            if (entity == null) {
                continue;
            }
            String key;
            if (entity.getId() != null) {
                key = "id:" + entity.getId();
            } else if (entity.getUuid() != null) {
                key = "uuid:" + entity.getUuid();
            } else {
                continue;
            }
            merged.putIfAbsent(key, entity);
        }
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
            if (log.isDebugEnabled()) {
                log.debug("Feed scoring: no viewed data for user {}, keeping all {} items", userId, items.size());
            }
            return items;
        }
        Set<Long> viewed = new HashSet<>(viewedList);
        List<FileMetadataEntity> result = items.stream()
                .filter(item -> item.getId() == null || !viewed.contains(item.getId()))
                .toList();
        if (log.isDebugEnabled()) {
            log.debug("Feed scoring: excluded viewed items for user {}: {} removed of {}, viewedIds={}",
                    userId, items.size() - result.size(), items.size(), viewed);
        }
        return result;
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
        log.debug("Feed rerank top{} ids={} uuids={}", limit,
                ranked.stream().limit(limit).map(FileMetadataEntity::getId).toList(),
                ranked.stream().limit(limit).map(FileMetadataEntity::getUuid).toList());
        for (int i = 0; i < limit; i++) {
            FileMetadataEntity entity = ranked.get(i);
            double sim = cosineSimilarity(preference, entity.getCombinedEmbedding());
            double watchSim = cosineSimilarity(watchPreference, entity.getCombinedEmbedding());
            double dist = 1.0 - sim;
            double watchDist = 1.0 - watchSim;
            long likesCount = entity.getLikesCount() != null ? entity.getLikesCount() : 0L;
            long viewsCount = entity.getViewsCount() != null ? entity.getViewsCount() : 0L;
            double likes = Math.log1p(likesCount);
            double views = Math.log1p(viewsCount);
            double score = sim + WATCH_SCORE_WEIGHT * watchSim + 0.3 * likes + 0.2 * views;
            log.debug(
                    "Feed score user={} rank={} videoId={} uuid={} sim={} dist={} watchSim={} watchDist={} likes={} views={} score={}",
                    userId,
                    i,
                    entity.getId(),
                    entity.getUuid(),
                    String.format(Locale.ROOT, "%.4f", sim),
                    String.format(Locale.ROOT, "%.4f", dist),
                    String.format(Locale.ROOT, "%.4f", watchSim),
                    String.format(Locale.ROOT, "%.4f", watchDist),
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

    private Map<UUID, Long> resolveCommentsCountByUuid(List<FileMetadataEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<UUID> uuids = entities.stream()
                .map(FileMetadataEntity::getUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (uuids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<VideoCommentsCountDto> counts = interactionsInternalClient.getCommentsCount(new VideoUuidsRequest(uuids));
        if (counts == null || counts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, Long> result = new HashMap<>();
        for (VideoCommentsCountDto row : counts) {
            if (row == null || row.videoUuid() == null) {
                continue;
            }
            result.put(row.videoUuid(), row.commentsCount());
        }
        return result;
    }

    private record WeightedVector(float[] vector, double weight) {
    }
}
