package ru.itmo.zavar.brainstop.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.streaming.dto.response.FeedSessionResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FeedSessionServiceTest {

    private InMemoryRedis redis;
    private FeedService feedService;
    private FileMetadataRepository fileMetadataRepository;
    private LikeQueryApi likeQueryApi;
    private FeedSessionService service;

    @BeforeEach
    void setUp() {
        redis = new InMemoryRedis();
        feedService = mock(FeedService.class);
        fileMetadataRepository = mock(FileMetadataRepository.class);
        likeQueryApi = mock(LikeQueryApi.class);

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new FeedSessionService(redis.template(), objectMapper, feedService, fileMetadataRepository, likeQueryApi);

        when(fileMetadataRepository.findByUuidIn(anyCollection())).thenReturn(Collections.emptyList());
        when(likeQueryApi.findLikedVideoIds(anyLong(), anyCollection())).thenReturn(Collections.emptyList());
    }

    @Test
    void startSessionSinglePageHasNullPrevNext() {
        Long userId = 7L;
        String deviceId = "device-1";
        FeedVideoDto first = video("v1");

        stubFeed(userId, List.of(first), Collections.emptyList());

        FeedSessionResponse response = service.startSession(deviceId, userId, 2);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).uuid()).isEqualTo(first.uuid());
        assertThat(response.prevPos()).isNull();
        assertThat(response.nextPos()).isNull();
    }

    @Test
    void nextPrevUsePageStartPositions() {
        Long userId = 42L;
        String deviceId = "device-2";
        FeedVideoDto first = video("v1");
        FeedVideoDto second = video("v2");
        FeedVideoDto third = video("v3");

        stubFeed(userId, List.of(first, second), List.of(third));

        FeedSessionResponse start = service.startSession(deviceId, userId, 2);

        assertThat(start.prevPos()).isNull();
        assertThat(start.nextPos()).isEqualTo(2L);

        FeedSessionResponse next = service.next(start.feedId(), deviceId, userId, 2, null);

        assertThat(next.items()).hasSize(1);
        assertThat(next.items().get(0).uuid()).isEqualTo(third.uuid());
        assertThat(next.prevPos()).isEqualTo(0L);
        assertThat(next.nextPos()).isNull();

        FeedSessionResponse prev = service.prev(start.feedId(), deviceId, userId, 2, null);

        assertThat(prev.items()).hasSize(2);
        assertThat(prev.items().get(0).uuid()).isEqualTo(first.uuid());
        assertThat(prev.items().get(1).uuid()).isEqualTo(second.uuid());
        assertThat(prev.prevPos()).isNull();
        assertThat(prev.nextPos()).isEqualTo(2L);
    }

    @Test
    void likedByMeIsHydratedForSessionResponses() {
        Long userId = 9L;
        String deviceId = "device-3";
        FeedVideoDto likedVideo = video("liked");

        stubFeed(userId, List.of(likedVideo), Collections.emptyList());

        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId(123L);
        entity.setUuid(likedVideo.uuid());
        when(fileMetadataRepository.findByUuidIn(anyCollection())).thenReturn(List.of(entity));
        when(likeQueryApi.findLikedVideoIds(eq(userId), anyCollection())).thenReturn(List.of(entity.getId()));

        FeedSessionResponse response = service.startSession(deviceId, userId, 1);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).likedByMe()).isTrue();
    }

    private void stubFeed(Long userId, List<FeedVideoDto> page0, List<FeedVideoDto> page1) {
        when(feedService.getFeed(eq(userId), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1);
            int pageNumber = pageable.getPageNumber();
            List<FeedVideoDto> items;
            if (pageNumber == 0) {
                items = new ArrayList<>(page0);
            } else if (pageNumber == 1) {
                items = new ArrayList<>(page1);
            } else {
                items = Collections.emptyList();
            }
            return new PageImpl<>(items, PageRequest.of(pageNumber, pageable.getPageSize()), items.size());
        });
    }

    private FeedVideoDto video(String name) {
        return new FeedVideoDto(
                UUID.randomUUID(),
                name,
                1L,
                "uploader",
                null,
                100L,
                "video/mp4",
                null,
                0L,
                0L,
                0L,
                false,
                false,
                720,
                List.of(720),
                null
        );
    }

    private static final class InMemoryRedis {
        private final Map<String, List<String>> lists = new HashMap<>();
        private final Map<String, Map<String, String>> hashes = new HashMap<>();
        private final Map<String, Set<String>> sets = new HashMap<>();
        private final ListOperations<String, String> listOps;
        private final HashOperations<String, String, String> hashOps;
        private final SetOperations<String, String> setOps;
        private final StringRedisTemplate template;

        @SuppressWarnings("unchecked")
        InMemoryRedis() {
            listOps = mock(ListOperations.class);
            hashOps = mock(HashOperations.class);
            setOps = mock(SetOperations.class);
            template = mock(StringRedisTemplate.class);

            when(template.opsForList()).thenReturn(listOps);
            when(template.<String, String>opsForHash()).thenReturn(hashOps);
            when(template.opsForSet()).thenReturn(setOps);
            when(template.expire(anyString(), any(Duration.class))).thenReturn(true);

            when(listOps.rightPushAll(anyString(), any(Collection.class))).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Collection<String> values = invocation.getArgument(1);
                List<String> list = lists.computeIfAbsent(key, ignored -> new ArrayList<>());
                list.addAll(values);
                return (long) list.size();
            });

            when(listOps.size(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                return (long) lists.getOrDefault(key, Collections.emptyList()).size();
            });

            when(listOps.range(anyString(), anyLong(), anyLong())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                long start = invocation.getArgument(1);
                long end = invocation.getArgument(2);
                List<String> list = lists.getOrDefault(key, Collections.emptyList());
                if (list.isEmpty()) {
                    return Collections.emptyList();
                }
                long maxIndex = list.size() - 1L;
                if (end < 0) {
                    end = maxIndex;
                }
                if (start < 0) {
                    start = 0;
                }
                if (start > end || start > maxIndex) {
                    return Collections.emptyList();
                }
                int from = (int) start;
                int to = (int) Math.min(end, maxIndex);
                return new ArrayList<>(list.subList(from, to + 1));
            });

            doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                long start = invocation.getArgument(1);
                long end = invocation.getArgument(2);
                List<String> list = lists.getOrDefault(key, new ArrayList<>());
                if (list.isEmpty()) {
                    lists.put(key, new ArrayList<>());
                    return null;
                }
                long maxIndex = list.size() - 1L;
                if (end < 0) {
                    end = maxIndex;
                }
                if (start < 0) {
                    start = 0;
                }
                if (start > end || start > maxIndex) {
                    lists.put(key, new ArrayList<>());
                    return null;
                }
                int from = (int) start;
                int to = (int) Math.min(end, maxIndex);
                lists.put(key, new ArrayList<>(list.subList(from, to + 1)));
                return null;
            }).when(listOps).trim(anyString(), anyLong(), anyLong());

            doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                String hashKey = invocation.getArgument(1);
                String value = invocation.getArgument(2);
                hashes.computeIfAbsent(key, ignored -> new HashMap<>()).put(hashKey, value);
                return null;
            }).when(hashOps).put(anyString(), anyString(), anyString());

            when(hashOps.entries(anyString())).thenAnswer(invocation -> {
                String key = invocation.getArgument(0);
                return new HashMap<>(hashes.getOrDefault(key, Collections.emptyMap()));
            });

                        doAnswer(invocation -> {
                String key = invocation.getArgument(0);
                Object rawValues = invocation.getArgument(1);
                Set<String> set = sets.computeIfAbsent(key, ignored -> new HashSet<>());
                long added = 0;
                if (rawValues instanceof String[] values) {
                    for (String value : values) {
                        if (set.add(value)) {
                            added++;
                        }
                    }
                } else if (rawValues instanceof String value) {
                    if (set.add(value)) {
                        added++;
                    }
                }
                return added;
            }).when(setOps).add(anyString(), any());
        }

        StringRedisTemplate template() {
            return template;
        }
    }
}

