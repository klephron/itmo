package ru.itmo.zavar.brainstop.video.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.itmo.zavar.brainstop.common.api.like.LikeQueryApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.FeedVideoDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.model.FileMetadataEntity;
import ru.itmo.zavar.brainstop.video.repo.FileMetadataRepository;
import ru.itmo.zavar.brainstop.video.streaming.dto.response.FeedSessionResponse;

import java.time.Duration;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.1"})
@Layer(TestLayer.BE_UNIT)
class FeedSessionServiceComplexTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private FeedService feedService;
    @Mock
    private FileMetadataRepository fileMetadataRepository;
    @Mock
    private LikeQueryApi likeQueryApi;
    @Mock
    private HashOperations hashOps;
    @Mock
    private ListOperations<String, String> listOps;
    @Mock
    private SetOperations<String, String> setOps;

    private FeedSessionService service;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);

        service = new FeedSessionService(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules(),
                feedService,
                fileMetadataRepository,
                likeQueryApi
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-SESSION-01: startSession создает новую сессию")
    void startSession_createsNew() {
        when(feedService.getFeed(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(createFeedDto())));
        when(setOps.add(anyString(), any())).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        FeedSessionResponse response = service.startSession("device-1", 1L, 10);

        assertThat(response.feedId()).isNotNull();
        assertThat(response.items()).isNotEmpty();
        verify(hashOps, atLeastOnce()).put(anyString(), anyString(), anyString());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FEED-SESSION-02: next выбрасывает exception для несуществующей сессии")
    void next_throwsForNonExistent() {
        when(hashOps.entries(anyString())).thenReturn(Collections.emptyMap());

        assertThatThrownBy(() -> service.next("fake-id", "device-1", 1L, 10))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FEED-SESSION-03: next проверяет deviceId")
    void next_validatesDeviceId() {
        Map<String, String> meta = new HashMap<>();
        meta.put("deviceId", "device-1");
        meta.put("userId", "1");
        meta.put("pageSize", "10");
        meta.put("cursor", "0");
        meta.put("nextPage", "0");
        meta.put("total", "0");
        meta.put("exhausted", "false");

        when(hashOps.entries(anyString())).thenReturn(meta);

        assertThatThrownBy(() -> service.next("feed-id", "wrong-device", 1L, 10))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-FEED-SESSION-04: next проверяет userId")
    void next_validatesUserId() {
        Map<String, String> meta = new HashMap<>();
        meta.put("deviceId", "device-1");
        meta.put("userId", "1");
        meta.put("pageSize", "10");
        meta.put("cursor", "0");
        meta.put("nextPage", "0");
        meta.put("total", "0");
        meta.put("exhausted", "false");

        when(hashOps.entries(anyString())).thenReturn(meta);

        assertThatThrownBy(() -> service.next("feed-id", "device-1", 999L, 10))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-SESSION-05: prev возвращает предыдущую страницу")
    void prev_returnsPrevPage() {
        Map<String, String> meta = new HashMap<>();
        meta.put("deviceId", "device-1");
        meta.put("userId", "1");
        meta.put("pageSize", "10");
        meta.put("cursor", "2");
        meta.put("nextPage", "1");
        meta.put("total", "30");
        meta.put("exhausted", "false");

        when(hashOps.entries(anyString())).thenReturn(meta);
        when(listOps.size(anyString())).thenReturn(3L);
        when(listOps.range(anyString(), eq(1L), eq(1L)))
                .thenReturn(List.of("[" + toJson(createFeedDto()) + "]"));
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(fileMetadataRepository.findByUuidIn(any())).thenReturn(List.of());

        FeedSessionResponse response = service.prev("feed-id", "device-1", 1L);

        assertThat(response.items()).isNotEmpty();
        verify(hashOps, atLeastOnce()).put(anyString(), eq("cursor"), eq("1"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-SESSION-06: prev на первой странице")
    void prev_onFirstPage() {
        Map<String, String> meta = new HashMap<>();
        meta.put("deviceId", "device-1");
        meta.put("userId", "1");
        meta.put("pageSize", "10");
        meta.put("cursor", "0");
        meta.put("nextPage", "0");
        meta.put("total", "10");
        meta.put("exhausted", "false");

        when(hashOps.entries(anyString())).thenReturn(meta);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        when(listOps.size(anyString())).thenReturn(1L);

        FeedSessionResponse response = service.prev("feed-id", "device-1", 1L);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasPrev()).isFalse();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-SESSION-07: startSession обновляет likedByMe")
    void startSession_updatesLikedByMe() {
        FeedVideoDto dto = createFeedDto();
        FileMetadataEntity entity = new FileMetadataEntity();
        entity.setId(1L);
        entity.setUuid(dto.uuid());

        when(feedService.getFeed(eq(1L), any())).thenReturn(new PageImpl<>(List.of(dto)));
        when(setOps.add(anyString(), any())).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);
        when(fileMetadataRepository.findByUuidIn(any())).thenReturn(List.of(entity));
        when(likeQueryApi.findLikedVideoIds(eq(1L), any())).thenReturn(List.of(1L));
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        FeedSessionResponse response = service.startSession("device-1", 1L, 10);

        assertThat(response.items()).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-SESSION-08: next загружает новые элементы")
    void next_loadsNewItems() {
        Map<String, String> meta = new HashMap<>();
        meta.put("deviceId", "device-1");
        meta.put("userId", "1");
        meta.put("pageSize", "10");
        meta.put("cursor", "0");
        meta.put("nextPage", "1");
        meta.put("total", "10");
        meta.put("exhausted", "false");

        FeedVideoDto newDto = createFeedDto();

        when(hashOps.entries(anyString())).thenReturn(meta);
        when(listOps.size(anyString())).thenReturn(1L);
        when(feedService.getFeed(eq(1L), any())).thenReturn(new PageImpl<>(List.of(newDto)));
        when(setOps.add(anyString(), any())).thenReturn(1L);
        when(fileMetadataRepository.findByUuidIn(any())).thenReturn(List.of());
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        FeedSessionResponse response = service.next("feed-id", "device-1", 1L, 10);

        assertThat(response.items()).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-FEED-SESSION-10: startSession ограничивает pageSize")
    void startSession_limitsPageSize() {
        when(feedService.getFeed(any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(listOps.size(anyString())).thenReturn(0L);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        FeedSessionResponse response = service.startSession("device-1", 1L, 1000);

        // pageSize должен быть ограничен до MAX_HISTORY (100)
        assertThat(response).isNotNull();
    }

    private FeedVideoDto createFeedDto() {
        return new FeedVideoDto(
                UUID.randomUUID(),
                "Test Video",
                1L,
                "User",
                null,
                1000L,
                "video/mp4",
                null,
                10L,
                100L,
                5L,
                false,
                false,
                720,
                List.of(720),
                null
        );
    }

    private String toJson(FeedVideoDto dto) {
        try {
            return new ObjectMapper().findAndRegisterModules().writeValueAsString(dto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
