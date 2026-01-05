package ru.itmo.zavar.brainstop.video.service;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.video.streaming.dto.response.FeedSessionResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeedSessionService {

    private static final int MAX_HISTORY = 100;
    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private static final String META_DEVICE = "deviceId";
    private static final String META_USER = "userId";
    private static final String META_PAGE_SIZE = "pageSize";
    private static final String META_NEXT_PAGE = "nextPage";
    private static final String META_POS = "pos";
    private static final String META_BASE_INDEX = "baseIndex";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FeedService feedService;

    public FeedSessionResponse startSession(String deviceId, Long userId, int pageSize) {
        int safePageSize = clamp(pageSize, 1, MAX_HISTORY);
        String feedId = UUID.randomUUID().toString();
        String listKey = listKey(feedId);
        String metaKey = metaKey(feedId);
        String seenKey = seenKey(feedId);

        Page<FeedVideoDto> page = feedService.getFeed(userId, PageRequest.of(0, safePageSize));
        List<FeedVideoDto> items = page.getContent();

        if (items != null && !items.isEmpty()) {
            List<String> payloads = new ArrayList<>();
            for (FeedVideoDto dto : items) {
                String json = toJson(dto);
                payloads.add(json);
                addSeen(seenKey, dto);
            }
            listOps().rightPushAll(listKey, payloads);
        }

        long baseIndex = 0;
        long pos = items == null || items.isEmpty() ? -1 : baseIndex;
        SessionMeta meta = new SessionMeta(deviceId, userId, safePageSize, 1, pos, baseIndex);
        saveMeta(metaKey, meta);
        touch(feedId);

        long fromIndex = items == null || items.isEmpty() ? -1 : baseIndex;
        long toIndex = items == null || items.isEmpty() ? -1 : baseIndex + items.size() - 1;

        return buildResponse(feedId, meta, items, fromIndex, toIndex, listSize(listKey));
    }

    public FeedSessionResponse next(String feedId, String deviceId, Long userId, int count, Long pos) {
        SessionMeta meta = requireMeta(feedId);
        validateAccess(meta, deviceId, userId);
        if (pos != null) {
            meta = alignPos(meta, pos);
            saveMeta(metaKey(feedId), meta);
        }

        int safeCount = clamp(count, 1, MAX_HISTORY);
        meta = ensureItems(feedId, meta, meta.pos() + safeCount);

        String listKey = listKey(feedId);
        long size = listSize(listKey);
        if (size <= 0) {
            touch(feedId);
            return buildResponse(feedId, meta, Collections.emptyList(), -1, -1, size);
        }

        long maxIndex = meta.baseIndex() + size - 1;
        long start = meta.pos() + 1;
        if (start > maxIndex) {
            touch(feedId);
            return buildResponse(feedId, meta, Collections.emptyList(), -1, -1, size);
        }

        long end = Math.min(start + safeCount - 1, maxIndex);
        List<FeedVideoDto> items = readRange(listKey, meta.baseIndex(), start, end);

        meta = meta.withPos(end);
        saveMeta(metaKey(feedId), meta);
        touch(feedId);

        return buildResponse(feedId, meta, items, start, end, size);
    }

    public FeedSessionResponse prev(String feedId, String deviceId, Long userId, int count, Long pos) {
        SessionMeta meta = requireMeta(feedId);
        validateAccess(meta, deviceId, userId);
        if (pos != null) {
            meta = alignPos(meta, pos);
            saveMeta(metaKey(feedId), meta);
        }

        int safeCount = clamp(count, 1, MAX_HISTORY);
        String listKey = listKey(feedId);
        long size = listSize(listKey);
        if (size <= 0) {
            touch(feedId);
            return buildResponse(feedId, meta, Collections.emptyList(), -1, -1, size);
        }

        long baseIndex = meta.baseIndex();
        if (meta.pos() <= baseIndex) {
            touch(feedId);
            return buildResponse(feedId, meta, Collections.emptyList(), -1, -1, size);
        }

        long end = meta.pos() - 1;
        long start = Math.max(baseIndex, meta.pos() - safeCount);
        List<FeedVideoDto> items = readRange(listKey, baseIndex, start, end);

        meta = meta.withPos(start);
        saveMeta(metaKey(feedId), meta);
        touch(feedId);

        return buildResponse(feedId, meta, items, start, end, size);
    }

    private SessionMeta ensureItems(String feedId, SessionMeta meta, long desiredIndex) {
        String listKey = listKey(feedId);
        String seenKey = seenKey(feedId);
        long size = listSize(listKey);
        long maxIndex = size <= 0 ? meta.baseIndex() - 1 : meta.baseIndex() + size - 1;

        int attempts = 0;
        while (maxIndex < desiredIndex && attempts < 5) {
            Page<FeedVideoDto> page = feedService.getFeed(meta.userId(), PageRequest.of(meta.nextPage(), meta.pageSize()));
            meta = meta.withNextPage(meta.nextPage() + 1);
            List<FeedVideoDto> items = page.getContent();
            if (items != null && !items.isEmpty()) {
                List<String> payloads = new ArrayList<>();
                for (FeedVideoDto dto : items) {
                    if (addSeen(seenKey, dto)) {
                        payloads.add(toJson(dto));
                    }
                }
                if (!payloads.isEmpty()) {
                    listOps().rightPushAll(listKey, payloads);
                    size = listSize(listKey);
                    maxIndex = meta.baseIndex() + size - 1;
                }
            }
            if (items == null || items.isEmpty()) {
                break;
            }
            attempts++;
        }

        meta = trimHistory(feedId, meta);
        saveMeta(metaKey(feedId), meta);
        touch(feedId);

        return meta;
    }

    private SessionMeta trimHistory(String feedId, SessionMeta meta) {
        String listKey = listKey(feedId);
        long size = listSize(listKey);
        if (size <= MAX_HISTORY) {
            return meta;
        }
        long trim = size - MAX_HISTORY;
        listOps().trim(listKey, trim, -1);
        long newBase = meta.baseIndex() + trim;
        long newPos = meta.pos() - trim;
        if (newPos < newBase - 1) {
            newPos = newBase - 1;
        }
        return new SessionMeta(meta.deviceId(), meta.userId(), meta.pageSize(), meta.nextPage(), newPos, newBase);
    }

    private SessionMeta alignPos(SessionMeta meta, long pos) {
        long base = meta.baseIndex();
        long aligned = Math.max(base - 1, pos);
        return meta.withPos(aligned);
    }

    private FeedSessionResponse buildResponse(String feedId, SessionMeta meta, List<FeedVideoDto> items, long fromIndex, long toIndex, long listSize) {
        long maxIndex = listSize <= 0 ? meta.baseIndex() - 1 : meta.baseIndex() + listSize - 1;
        boolean hasPrev = meta.pos() > meta.baseIndex();
        boolean hasNext = meta.pos() < maxIndex;
        return new FeedSessionResponse(feedId, items, fromIndex, toIndex, meta.pos(), meta.baseIndex(), hasPrev, hasNext);
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
        int nextPage = parseInt(metaMap.get(META_NEXT_PAGE), 1);
        long pos = parseLong(metaMap.get(META_POS), -1);
        long baseIndex = parseLong(metaMap.get(META_BASE_INDEX), 0);
        return new SessionMeta(deviceId, userId, pageSize, nextPage, pos, baseIndex);
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
        hashOps().put(metaKey, META_NEXT_PAGE, String.valueOf(meta.nextPage()));
        hashOps().put(metaKey, META_POS, String.valueOf(meta.pos()));
        hashOps().put(metaKey, META_BASE_INDEX, String.valueOf(meta.baseIndex()));
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

    private List<FeedVideoDto> readRange(String listKey, long baseIndex, long startIndex, long endIndex) {
        long from = startIndex - baseIndex;
        long to = endIndex - baseIndex;
        if (from < 0 || to < 0 || to < from) {
            return Collections.emptyList();
        }
        List<String> raw = listOps().range(listKey, from, to);
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<FeedVideoDto> result = new ArrayList<>();
        for (String payload : raw) {
            if (payload == null || payload.isBlank()) {
                continue;
            }
            try {
                result.add(objectMapper.readValue(payload, FeedVideoDto.class));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to parse feed session item", e);
            }
        }
        return result;
    }

    private String toJson(FeedVideoDto dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize feed session item", e);
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
        return "feed:session:" + feedId + ":list";
    }

    private String seenKey(String feedId) {
        return "feed:session:" + feedId + ":seen";
    }

    private record SessionMeta(String deviceId, Long userId, int pageSize, int nextPage, long pos, long baseIndex) {
        SessionMeta withPos(long newPos) {
            return new SessionMeta(deviceId, userId, pageSize, nextPage, newPos, baseIndex);
        }

        SessionMeta withNextPage(int newNextPage) {
            return new SessionMeta(deviceId, userId, pageSize, newNextPage, pos, baseIndex);
        }
    }
}
