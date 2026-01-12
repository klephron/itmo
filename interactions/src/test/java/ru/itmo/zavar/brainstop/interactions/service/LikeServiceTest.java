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
import ru.itmo.zavar.brainstop.like.model.VideoLikeEntity;
import ru.itmo.zavar.brainstop.like.repo.VideoLikeRepository;
import ru.itmo.zavar.brainstop.like.service.LikeService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1", "FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class LikeServiceTest {

    @Mock
    private VideoLikeRepository videoLikeRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private VideoMetadataPublicApi videoMetadataPublicApi;

    @InjectMocks
    private LikeService likeService;

    private UUID videoUuid;
    private VideoMetadataSummary videoMeta;

    @BeforeEach
    void setUp() {
        videoUuid = UUID.randomUUID();
        videoMeta = new VideoMetadataSummary(
                1L,              // id
                videoUuid,       // uuid
                5L,              // likesCount
                100L,            // viewsCount
                10L,             // uploaderId
                "Test Uploader", // uploaderName
                "video/mp4",     // contentType
                1000000L,        // contentSize
                "thumb.jpg"      // thumbnailKey
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-01: Добавление лайка увеличивает счетчик")
    void likeVideo_success() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(videoLikeRepository.existsByVideoIdAndUserId(anyLong(), anyLong())).thenReturn(false);
        when(videoMetadataPublicApi.incrementLikesCount(anyLong(), anyLong())).thenReturn(6L);

        long result = likeService.likeVideo(videoUuid, 100L);

        assertThat(result).isEqualTo(6L);
        verify(videoLikeRepository).save(any(VideoLikeEntity.class));
        verify(videoMetadataPublicApi).incrementLikesCount(1L, 1L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-02: Повторный лайк не изменяет счетчик")
    void likeVideo_alreadyExists() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(videoLikeRepository.existsByVideoIdAndUserId(anyLong(), anyLong())).thenReturn(true);

        long result = likeService.likeVideo(videoUuid, 100L);

        assertThat(result).isEqualTo(5L);
        verify(videoLikeRepository, never()).save(any(VideoLikeEntity.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-03: Удаление лайка уменьшает счетчик")
    void unlikeVideo_success() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(videoLikeRepository.deleteByVideoIdAndUserId(anyLong(), anyLong())).thenReturn(1);
        when(videoMetadataPublicApi.incrementLikesCount(anyLong(), anyLong())).thenReturn(4L);

        long result = likeService.unlikeVideo(videoUuid, 100L);

        assertThat(result).isEqualTo(4L);
        verify(videoLikeRepository).deleteByVideoIdAndUserId(anyLong(), anyLong());
        verify(videoMetadataPublicApi).incrementLikesCount(1L, -1L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-04: Удаление несуществующего лайка возвращает текущий счетчик")
    void unlikeVideo_notExists() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        when(videoLikeRepository.deleteByVideoIdAndUserId(anyLong(), anyLong())).thenReturn(0);

        long result = likeService.unlikeVideo(videoUuid, 100L);

        assertThat(result).isEqualTo(5L);
        verify(videoMetadataPublicApi, never()).incrementLikesCount(anyLong(), anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-05: Проверка статуса лайка для залайканного видео")
    void isLikedByUser_returnsTrue() {
        when(videoMetadataPublicApi.findByUuid(any(UUID.class)))
                .thenReturn(Optional.of(videoMeta));
        when(videoLikeRepository.existsByVideoIdAndUserId(anyLong(), anyLong()))
                .thenReturn(true);

        boolean result = likeService.isLikedByUser(videoUuid, 100L);

        assertThat(result).isTrue();
        verify(videoLikeRepository).existsByVideoIdAndUserId(1L, 100L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-06: Проверка статуса лайка для не лайканного видео")
    void isLikedByUser_returnsFalse() {
        when(videoMetadataPublicApi.findByUuid(any(UUID.class)))
                .thenReturn(Optional.of(videoMeta));
        when(videoLikeRepository.existsByVideoIdAndUserId(anyLong(), anyLong()))
                .thenReturn(false);

        boolean result = likeService.isLikedByUser(videoUuid, 100L);

        assertThat(result).isFalse();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-07: Проверка статуса для несуществующего видео")
    void isLikedByUser_videoNotFound() {
        when(videoMetadataPublicApi.findByUuid(any(UUID.class)))
                .thenReturn(Optional.empty());

        boolean result = likeService.isLikedByUser(videoUuid, 100L);

        assertThat(result).isFalse();
        verify(videoLikeRepository, never()).existsByVideoIdAndUserId(anyLong(), anyLong());
    }
}
