package ru.itmo.zavar.brainstop.interactions.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.interactions.video.client.VideoMetadataInternalClient;
import ru.itmo.zavar.brainstop.interactions.video.service.VideoMetadataPublicApiImpl;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-4.1"})
@Layer(TestLayer.BE_UNIT)
class VideoMetadataPublicApiImplTest {

    @Mock
    private VideoMetadataInternalClient videoMetadataInternalClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private VideoMetadataPublicApiImpl videoMetadataPublicApi;

    private UUID videoUuid;
    private VideoMetadataSummary videoMeta;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        videoMeta = new VideoMetadataSummary(
                1L, videoUuid, 5L, 100L, 10L, "Uploader", "video/mp4", 1000000L, "thumb.jpg"
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-01: Получение метаданных из клиента")
    void findByUuid_fromClient() throws Exception {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(videoMetadataInternalClient.getByUuid(any(UUID.class))).thenReturn(videoMeta);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Optional<VideoMetadataSummary> result = videoMetadataPublicApi.findByUuid(videoUuid);

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(1L);
        verify(videoMetadataInternalClient).getByUuid(videoUuid);
        verify(valueOperations).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-02: Получение из кеша Redis")
    void findByUuid_fromCache() throws Exception {
        String cached = "{\"id\":1,\"uuid\":\"" + videoUuid + "\"}";
        when(valueOperations.get(anyString())).thenReturn(cached);
        when(objectMapper.readValue(anyString(), eq(VideoMetadataSummary.class))).thenReturn(videoMeta);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Optional<VideoMetadataSummary> result = videoMetadataPublicApi.findByUuid(videoUuid);

        assertThat(result).isPresent();
        verify(videoMetadataInternalClient, never()).getByUuid(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-META-03: Видео не найдено")
    void findByUuid_notFound() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(videoMetadataInternalClient.getByUuid(any(UUID.class)))
                .thenThrow(FeignException.NotFound.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Optional<VideoMetadataSummary> result = videoMetadataPublicApi.findByUuid(videoUuid);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-04: getRequired выбрасывает исключение если не найдено")
    void getRequired_notFound_throws() {
        when(valueOperations.get(anyString())).thenReturn(null);
        when(videoMetadataInternalClient.getByUuid(any(UUID.class))).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThatThrownBy(() -> videoMetadataPublicApi.getRequired(videoUuid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Video not found");
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-META-06: Инкремент лайков для несуществующего видео")
    void incrementLikesCount_notFound() {
        when(videoMetadataInternalClient.incrementLikes(anyLong(), anyLong()))
                .thenThrow(FeignException.NotFound.class);

        long result = videoMetadataPublicApi.incrementLikesCount(999L, 1L);

        assertThat(result).isEqualTo(-1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-07: Обновление счетчика просмотров")
    void updateViewsCount_success() {
        doNothing().when(videoMetadataInternalClient).updateViews(anyLong(), anyLong());

        videoMetadataPublicApi.updateViewsCount(1L, 200L);

        verify(videoMetadataInternalClient).updateViews(1L, 200L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-META-08: Обновление лайков игнорирует NotFound")
    void updateLikesCount_notFound_silent() {
        doThrow(FeignException.NotFound.class).when(videoMetadataInternalClient).updateLikes(anyLong(), anyLong());

        videoMetadataPublicApi.updateLikesCount(999L, 10L);

        verify(videoMetadataInternalClient).updateLikes(999L, 10L);
    }
}
