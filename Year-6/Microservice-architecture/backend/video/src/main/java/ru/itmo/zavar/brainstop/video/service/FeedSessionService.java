package ru.itmo.zavar.brainstop.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.streaming.dto.response.FeedSessionResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedSessionService {

    private static final int MAX_HISTORY = 100;
    private static final int MAX_PAGE_ATTEMPTS = 5;
    private static final int MAX_REFRESH_ATTEMPTS = 2;
    private static final int PREFETCH_PAGES = 3;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private static final String META_DEVICE = "deviceId";
    private static final String META_USER = "userId";
    private static final String META_PAGE_SIZE = "pageSize";
    private static final String META_CURSOR = "cursor";
    private static final String META_NEXT_PAGE = "nextPage";
    private static final String META_TOTAL = "total";
    private static final String META_EXHAUSTED = "exhausted";

    private static final TypeReference<List<FeedVideoDto>> FEED_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FeedService feedService;
    private final FileMetadataRepository fileMetadataRepository;
    private final LikeQueryApi likeQueryApi;

    public FeedSessionResponse startSession(String deviceId, Long userId, int pageSize) {
        int safePageSize = clamp(pageSize, 1, MAX_HISTORY);
        String feedId = UUID.randomUUID().toString();
        String listKey = listKey(feedId);
        String metaKey = metaKey(feedId);
        String seenKey = seenKey(feedId);

        SessionMeta meta = new SessionMeta(deviceId, userId, safePageSize, 0, 0, 0L, false);
        PageLoad load = fetchItems(userId, seenKey, safePageSize, 0, safePageSize, MAX_PAGE_ATTEMPTS);
        List<FeedVideoDto> items = load.items();
        List<FeedVideoDto> buffered = load.buffered();
        meta = meta.withNextPage(load.nextPage()).withExhausted(load.exhausted());

        if (!items.isEmpty()) {
            appendPages(listKey, items, buffered, safePageSize);
            meta = meta.withTotal(items.size() + buffered.size()).withCursor(0);
        } else {
            meta = meta.withCursor(0);
        }

        meta = trimHistory(feedId, meta);
        saveMeta(metaKey, meta);
        touch(feedId);

        long pageCount = listSize(listKey);
        items = refreshLikedByMe(userId, items);

        return buildResponse(feedId, meta, items, pageCount);
    }

    public FeedSessionResponse next(String feedId, String deviceId, Long userId, int count) {
        SessionMeta meta = requireMeta(feedId);
        validateAccess(meta, deviceId, userId);

        int safeCount = clamp(count, 1, MAX_HISTORY);
        int pageSize = meta.pageSize() > 0 ? meta.pageSize() : safeCount;
        if (meta.pageSize() <= 0) {
            meta = meta.withPageSize(pageSize);
        }

        String listKey = listKey(feedId);
        long pageCount = listSize(listKey);

        if (pageCount > 0 && meta.cursor() + 1 < pageCount) {
            int nextCursor = meta.cursor() + 1;
            List<FeedVideoDto> items = readPage(listKey, nextCursor);
            items = refreshLikedByMe(meta.userId(), items);
            meta = meta.withCursor(nextCursor);
            saveMeta(metaKey(feedId), meta);
            touch(feedId);
            return buildResponse(feedId, meta, items, pageCount);
        }

        String seenKey = seenKey(feedId);
        PageLoad load = fetchItems(meta.userId(), seenKey, pageSize, meta.nextPage(), pageSize, MAX_PAGE_ATTEMPTS);
        List<FeedVideoDto> items = load.items();
        List<FeedVideoDto> buffered = load.buffered();
        int nextPage = load.nextPage();

        if (items.isEmpty()) {
            PageLoad refresh = fetchItems(meta.userId(), seenKey, pageSize, 0, pageSize, MAX_REFRESH_ATTEMPTS);
            if (!refresh.items().isEmpty()) {
                items = refresh.items();
                buffered = refresh.buffered();
                nextPage = refresh.nextPage();
                meta = meta.withExhausted(refresh.exhausted());
            } else {
                meta = meta.withExhausted(true);
            }
        } else {
            meta = meta.withExhausted(load.exhausted());
        }

        meta = meta.withNextPage(nextPage);

        if (!items.isEmpty()) {
            long pageCountBefore = pageCount;
            appendPages(listKey, items, buffered, pageSize);
            pageCount = listSize(listKey);
            meta = meta.withCursor((int) pageCountBefore).withTotal(meta.total() + items.size() + buffered.size());
        }

        meta = trimHistory(feedId, meta);
        saveMeta(metaKey(feedId), meta);
        touch(feedId);

        items = refreshLikedByMe(meta.userId(), items);
        pageCount = listSize(listKey);

        return buildResponse(feedId, meta, items, pageCount);
    }

    public FeedSessionResponse prev(String feedId, String deviceId, Long userId) {
        SessionMeta meta = requireMeta(feedId);
        validateAccess(meta, deviceId, userId);

        String listKey = listKey(feedId);
        long pageCount = listSize(listKey);
        if (pageCount <= 0 || meta.cursor() <= 0) {
            touch(feedId);
            return buildResponse(feedId, meta, Collections.emptyList(), pageCount);
        }

        int prevCursor = meta.cursor() - 1;
        List<FeedVideoDto> items = readPage(listKey, prevCursor);
        items = refreshLikedByMe(meta.userId(), items);
        meta = meta.withCursor(prevCursor);

        saveMeta(metaKey(feedId), meta);
        touch(feedId);

        return buildResponse(feedId, meta, items, pageCount);
    }

    private PageLoad fetchItems(Long userId,
                                String seenKey,
                                int pageSize,
                                int startPage,
                                int count,
                                int maxAttempts) {
        int page = startPage;
        int attempts = 0;
        boolean exhausted = false;
        List<FeedVideoDto> items = new ArrayList<>();
        List<FeedVideoDto> buffered = new ArrayList<>();
        int batchSize = Math.min(pageSize * PREFETCH_PAGES, MAX_HISTORY);
        int prefetchCapacity = Math.max(0, batchSize - count);
        while ((items.size() < count || buffered.size() < prefetchCapacity) && attempts < maxAttempts) {
            Page<FeedVideoDto> sample = feedService.getFeed(userId, PageRequest.of(page, batchSize));
            page++;
            List<FeedVideoDto> content = sample.getContent();
            if (content == null || content.isEmpty()) {
                exhausted = true;
                break;
            }
            for (FeedVideoDto dto : content) {
                if (addSeen(seenKey, dto)) {
                    if (items.size() < count) {
                        items.add(dto);
                    } else if (buffered.size() < prefetchCapacity) {
                        buffered.add(dto);
                    }
                    if (items.size() >= count && buffered.size() >= prefetchCapacity) {
                        break;
                    }
                }
            }
            attempts++;
        }
        return new PageLoad(items, buffered, page, exhausted);
    }

    private SessionMeta trimHistory(String feedId, SessionMeta meta) {
        if (meta.total() <= MAX_HISTORY) {
            return meta;
        }
        String listKey = listKey(feedId);
        List<String> pages = listOps().range(listKey, 0, -1);
        if (pages == null || pages.isEmpty()) {
            return meta.withTotal(0L).withCursor(0);
        }
        long total = meta.total();
        int cursor = meta.cursor();
        int trimCount = 0;
        for (String payload : pages) {
            if (total <= MAX_HISTORY) {
                break;
            }
            total -= pageSize(payload);
            trimCount++;
            cursor--;
        }
        if (trimCount <= 0) {
            return meta;
        }
        listOps().trim(listKey, trimCount, -1);
        if (cursor < 0) {
            cursor = 0;
        }
        return meta.withCursor(cursor).withTotal(Math.max(total, 0));
    }

    private int pageSize(String payload) {
        if (payload == null || payload.isBlank()) {
            return 0;
        }
        try {
            FeedVideoDto[] items = objectMapper.readValue(payload, FeedVideoDto[].class);
            return items.length;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse feed session page", e);
        }
    }

    private void appendPages(String listKey,
                             List<FeedVideoDto> items,
                             List<FeedVideoDto> buffered,
                             int pageSize) {
        if ((items == null || items.isEmpty()) && (buffered == null || buffered.isEmpty())) {
            return;
        }
        List<String> payloads = new ArrayList<>();
        if (items != null && !items.isEmpty()) {
            payloads.add(toJson(items));
        }
        if (buffered != null && !buffered.isEmpty()) {
            for (int i = 0; i < buffered.size(); i += pageSize) {
                int to = Math.min(i + pageSize, buffered.size());
                List<FeedVideoDto> page = buffered.subList(i, to);
                if (!page.isEmpty()) {
                    payloads.add(toJson(page));
                }
            }
        }
        if (!payloads.isEmpty()) {
            listOps().rightPushAll(listKey, payloads);
        }
    }

    private List<FeedVideoDto> readPage(String listKey, int index) {
        List<String> payloads = listOps().range(listKey, index, index);
        if (payloads == null || payloads.isEmpty()) {
            return Collections.emptyList();
        }
        String payload = payloads.get(0);
        if (payload == null || payload.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(payload, FEED_LIST_TYPE);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse feed session page", e);
        }
    }

    private FeedSessionResponse buildResponse(String feedId,
                                              SessionMeta meta,
                                              List<FeedVideoDto> items,
                                              long pageCount) {
        List<FeedVideoDto> safeItems = items == null ? Collections.emptyList() : items;
        boolean hasPrev = pageCount > 0 && meta.cursor() > 0;
        boolean hasNext = pageCount > 0 && (meta.cursor() + 1 < pageCount || !meta.exhausted());
        return new FeedSessionResponse(feedId, safeItems, hasPrev, hasNext, meta.total());
    }

    private List<FeedVideoDto> refreshLikedByMe(Long userId, List<FeedVideoDto> items) {
        if (userId == null || items == null || items.isEmpty()) {
            return items;
        }
        List<UUID> uuids = new ArrayList<>();
        for (FeedVideoDto item : items) {
            if (item != null && item.uuid() != null) {
                uuids.add(item.uuid());
            }
        }
        if (uuids.isEmpty()) {
            return items;
        }
        List<FileMetadataEntity> entities = fileMetadataRepository.findByUuidIn(uuids);
        if (entities == null || entities.isEmpty()) {
            return items;
        }
        Map<UUID, Long> idByUuid = new HashMap<>();
        for (FileMetadataEntity entity : entities) {
            if (entity != null && entity.getUuid() != null && entity.getId() != null) {
                idByUuid.put(entity.getUuid(), entity.getId());
            }
        }
        if (idByUuid.isEmpty()) {
            return items;
        }
        List<Long> likedVideoIds = likeQueryApi.findLikedVideoIds(userId, idByUuid.values());
        Set<Long> likedIds = likedVideoIds == null ? Collections.emptySet() : new HashSet<>(likedVideoIds);
        List<FeedVideoDto> updated = new ArrayList<>(items.size());
        for (FeedVideoDto item : items) {
            if (item == null || item.uuid() == null) {
                updated.add(item);
                continue;
            }
            Long id = idByUuid.get(item.uuid());
            if (id == null) {
                updated.add(item);
                continue;
            }
            boolean liked = likedIds.contains(id);
            if (liked == item.likedByMe()) {
                updated.add(item);
                continue;
            }
            updated.add(new FeedVideoDto(
                    item.uuid(),
                    item.name(),
                    item.uploaderId(),
                    item.uploaderName(),
                    item.uploaderAvatarUrl(),
                    item.contentSize(),
                    item.contentType(),
                    item.uploadedAt(),
                    item.likesCount(),
                    item.viewsCount(),
                    item.commentsCount(),
                    liked,
                    item.subscribedToAuthor(),
                    item.defaultHeight(),
                    item.availableQualities(),
                    item.description()
            ));
        }
        return updated;
    }

    private SessionMeta requireMeta(String feedId) {
        Map<String, String> metaMap = hashOps().entries(metaKey(feedId));
        if (metaMap == null || metaMap.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Feed session not found");
        }
        String deviceId = metaMap.get(META_DEVICE);
        String userRaw = metaMap.get(META_USER);
        Long userId = (userRaw == null || userRaw.isBlank()) ? null : Long.valueOf(userRaw);
        int pageSize = parseInt(metaMap.get(META_PAGE_SIZE), 10);
        int cursor = parseInt(metaMap.get(META_CURSOR), 0);
        int nextPage = parseInt(metaMap.get(META_NEXT_PAGE), 0);
        long total = parseLong(metaMap.get(META_TOTAL), 0L);
        boolean exhausted = parseBoolean(metaMap.get(META_EXHAUSTED), false);
        return new SessionMeta(deviceId, userId, pageSize, cursor, nextPage, total, exhausted);
    }

    private void validateAccess(SessionMeta meta, String deviceId, Long userId) {
        if (!Objects.equals(meta.deviceId(), deviceId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Feed session is bound to another device");
        }
        if (!Objects.equals(meta.userId(), userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Feed session is bound to another user");
        }
    }

    private void saveMeta(String metaKey, SessionMeta meta) {
        hashOps().put(metaKey, META_DEVICE, meta.deviceId());
        hashOps().put(metaKey, META_USER, meta.userId() == null ? "" : String.valueOf(meta.userId()));
        hashOps().put(metaKey, META_PAGE_SIZE, String.valueOf(meta.pageSize()));
        hashOps().put(metaKey, META_CURSOR, String.valueOf(meta.cursor()));
        hashOps().put(metaKey, META_NEXT_PAGE, String.valueOf(meta.nextPage()));
        hashOps().put(metaKey, META_TOTAL, String.valueOf(meta.total()));
        hashOps().put(metaKey, META_EXHAUSTED, String.valueOf(meta.exhausted()));
    }

    private void touch(String feedId) {
        redisTemplate.expire(metaKey(feedId), SESSION_TTL);
        redisTemplate.expire(listKey(feedId), SESSION_TTL);
        redisTemplate.expire(seenKey(feedId), SESSION_TTL);
    }

    private boolean addSeen(String seenKey, FeedVideoDto dto) {
        if (dto == null || dto.uuid() == null) {
            return false;
        }
        Long added = setOps().add(seenKey, dto.uuid().toString());
        return added != null && added > 0;
    }

    private String toJson(List<FeedVideoDto> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize feed session page", e);
        }
    }

    private int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long parseLong(String raw, long fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Boolean.parseBoolean(raw);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private long listSize(String listKey) {
        Long size = listOps().size(listKey);
        return size == null ? 0 : size;
    }

    private HashOperations<String, String, String> hashOps() {
        return redisTemplate.opsForHash();
    }

    private ListOperations<String, String> listOps() {
        return redisTemplate.opsForList();
    }

    private SetOperations<String, String> setOps() {
        return redisTemplate.opsForSet();
    }

    private String metaKey(String feedId) {
        return "feed:session:" + feedId + ":meta";
    }

    private String listKey(String feedId) {
        return "feed:session:" + feedId + ":pages";
    }

    private String seenKey(String feedId) {
        return "feed:session:" + feedId + ":seen";
    }

    private record SessionMeta(String deviceId,
                               Long userId,
                               int pageSize,
                               int cursor,
                               int nextPage,
                               long total,
                               boolean exhausted) {
        SessionMeta withCursor(int newCursor) {
            return new SessionMeta(deviceId, userId, pageSize, newCursor, nextPage, total, exhausted);
        }

        SessionMeta withNextPage(int newNextPage) {
            return new SessionMeta(deviceId, userId, pageSize, cursor, newNextPage, total, exhausted);
        }

        SessionMeta withTotal(long newTotal) {
            return new SessionMeta(deviceId, userId, pageSize, cursor, nextPage, newTotal, exhausted);
        }

        SessionMeta withExhausted(boolean newExhausted) {
            return new SessionMeta(deviceId, userId, pageSize, cursor, nextPage, total, newExhausted);
        }

        SessionMeta withPageSize(int newPageSize) {
            return new SessionMeta(deviceId, userId, newPageSize, cursor, nextPage, total, exhausted);
        }
    }

    private record PageLoad(List<FeedVideoDto> items,
                            List<FeedVideoDto> buffered,
                            int nextPage,
                            boolean exhausted) {
    }
}
