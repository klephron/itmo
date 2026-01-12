package ru.itmo.zavar.brainstop.interactions.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.like.model.VideoViewEntity;
import ru.itmo.zavar.brainstop.like.repo.VideoViewRepository;
import ru.itmo.zavar.brainstop.like.service.ViewService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.3"})
@Layer(TestLayer.BE_UNIT)
class ViewServiceTest {

    @Mock
    private VideoViewRepository videoViewRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private VideoMetadataPublicApi videoMetadataPublicApi;

    @InjectMocks
    private ViewService viewService;

    private UUID videoUuid;
    private VideoMetadataSummary videoMeta;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        videoMeta = new VideoMetadataSummary(
                1L,              // id
                videoUuid,       // uuid
                5L,              // likesCount
                10L,             // viewsCount
                10L,             // uploaderId
                "Test Uploader", // uploaderName
                "video/mp4",     // contentType
                1000000L,        // contentSize
                "thumb.jpg"      // thumbnailKey
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIEW-01: Первый просмотр увеличивает счетчик")
    void viewVideo_firstView() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(videoViewRepository.existsByVideoIdAndUserId(anyLong(), anyLong())).thenReturn(false);
        when(videoMetadataPublicApi.incrementViewsCount(anyLong(), anyLong())).thenReturn(11L);

        long result = viewService.viewVideo(videoUuid, 100L);

        assertThat(result).isEqualTo(11L);
        verify(videoViewRepository).save(any(VideoViewEntity.class));
        verify(videoMetadataPublicApi).incrementViewsCount(1L, 1L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-VIEW-02: Повторный просмотр не увеличивает счетчик")
    void viewVideo_alreadyViewed() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(videoViewRepository.existsByVideoIdAndUserId(anyLong(), anyLong())).thenReturn(true);

        long result = viewService.viewVideo(videoUuid, 100L);

        assertThat(result).isEqualTo(10L);
        verify(videoViewRepository, never()).save(any(VideoViewEntity.class));
        verify(videoMetadataPublicApi, never()).incrementViewsCount(anyLong(), anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIEW-03: Получение количества просмотров")
    void getViews_success() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);

        long result = viewService.getViews(videoUuid);

        assertThat(result).isEqualTo(10L);
        verify(videoMetadataPublicApi).getRequired(videoUuid);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-VIEW-04: Просмотр с fallback на updateViewsCount")
    void viewVideo_incrementFails_fallbackToUpdate() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(videoViewRepository.existsByVideoIdAndUserId(anyLong(), anyLong())).thenReturn(false);
        when(videoMetadataPublicApi.incrementViewsCount(anyLong(), anyLong())).thenReturn(-1L);
        doNothing().when(videoMetadataPublicApi).updateViewsCount(anyLong(), anyLong());

        long result = viewService.viewVideo(videoUuid, 100L);

        assertThat(result).isEqualTo(11L);
        verify(videoMetadataPublicApi).updateViewsCount(1L, 11L);
    }
}
