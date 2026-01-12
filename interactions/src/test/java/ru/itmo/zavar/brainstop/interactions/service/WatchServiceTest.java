package ru.itmo.zavar.brainstop.interactions.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.like.dto.request.WatchProgressRequest;
import ru.itmo.zavar.brainstop.like.repo.VideoWatchRepository;
import ru.itmo.zavar.brainstop.like.service.WatchService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.4"})
@Layer(TestLayer.BE_UNIT)
class WatchServiceTest {

    @Mock
    private VideoWatchRepository videoWatchRepository;

    @Mock
    private VideoMetadataPublicApi videoMetadataPublicApi;

    @InjectMocks
    private WatchService watchService;

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
    @DisplayName("BE-UNIT-WATCH-01: Запись прогресса с positionSeconds")
    void recordProgress_withPosition() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        doNothing().when(videoWatchRepository).upsertProgress(anyLong(), anyLong(), anyLong(), anyLong());

        WatchProgressRequest request = new WatchProgressRequest(30L, 120L, null);
        watchService.recordProgress(videoUuid, 100L, request);

        verify(videoWatchRepository).upsertProgress(1L, 100L, 30L, 120L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-WATCH-02: Запись прогресса с percent вычисляет позицию")
    void recordProgress_withPercent() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        doNothing().when(videoWatchRepository).upsertProgress(anyLong(), anyLong(), anyLong(), anyLong());

        WatchProgressRequest request = new WatchProgressRequest(null, 100L, 50);
        watchService.recordProgress(videoUuid, 100L, request);

        verify(videoWatchRepository).upsertProgress(eq(1L), eq(100L), eq(50L), eq(100L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-WATCH-03: Позиция не превышает длительность")
    void recordProgress_positionClampedToDuration() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        doNothing().when(videoWatchRepository).upsertProgress(anyLong(), anyLong(), anyLong(), anyLong());

        WatchProgressRequest request = new WatchProgressRequest(150L, 100L, null);
        watchService.recordProgress(videoUuid, 100L, request);

        verify(videoWatchRepository).upsertProgress(1L, 100L, 100L, 100L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-WATCH-04: Отрицательная позиция нормализуется до 0")
    void recordProgress_negativePositionClamped() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        doNothing().when(videoWatchRepository).upsertProgress(anyLong(), anyLong(), anyLong(), anyLong());

        WatchProgressRequest request = new WatchProgressRequest(-10L, 100L, null);
        watchService.recordProgress(videoUuid, 100L, request);

        verify(videoWatchRepository).upsertProgress(1L, 100L, 0L, 100L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-WATCH-05: Без userId запись игнорируется")
    void recordProgress_noUserId_ignored() {
        WatchProgressRequest request = new WatchProgressRequest(30L, 100L, null);

        watchService.recordProgress(videoUuid, null, request);

        verify(videoWatchRepository, never()).upsertProgress(anyLong(), anyLong(), anyLong(), anyLong());
        verify(videoMetadataPublicApi, never()).getRequired(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-WATCH-06: Null request игнорируется")
    void recordProgress_nullRequest_ignored() {
        watchService.recordProgress(videoUuid, 100L, null);

        verify(videoWatchRepository, never()).upsertProgress(anyLong(), anyLong(), anyLong(), anyLong());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-WATCH-07: Процент 100 записывает полную длительность")
    void recordProgress_percent100() {
        when(videoMetadataPublicApi.getRequired(any(UUID.class))).thenReturn(videoMeta);
        doNothing().when(videoWatchRepository).upsertProgress(anyLong(), anyLong(), anyLong(), anyLong());

        WatchProgressRequest request = new WatchProgressRequest(null, 100L, 100);
        watchService.recordProgress(videoUuid, 100L, request);

        verify(videoWatchRepository).upsertProgress(1L, 100L, 100L, 100L);
    }
}
