package ru.itmo.zavar.brainstop.interactions.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.comment.repo.CommentRepository;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoCommentsCountDto;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoUuidsRequest;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.WatchProgressSummaryDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.like.repo.VideoLikeRepository;
import ru.itmo.zavar.brainstop.like.repo.VideoViewRepository;
import ru.itmo.zavar.brainstop.like.repo.VideoWatchRepository;
import ru.itmo.zavar.brainstop.like.repo.WatchProgressSummary;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1", "FR-5.3", "FR-6.1"})
@Layer(TestLayer.BE_UNIT)
class InteractionsInternalControllerTest {

    @Mock
    private VideoLikeRepository videoLikeRepository;

    @Mock
    private VideoViewRepository videoViewRepository;

    @Mock
    private VideoWatchRepository videoWatchRepository;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private InteractionsInternalController controller;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-INT-01: Получение лайкнутых видео")
    void likedVideoIds_success() {
        VideoIdsRequest request = new VideoIdsRequest(100L, List.of(1L, 2L, 3L));
        when(videoLikeRepository.findLikedVideoIdsByUserIdAndVideoIdIn(anyLong(), any()))
                .thenReturn(List.of(1L, 3L));

        List<Long> result = controller.likedVideoIds(request);

        assertThat(result).containsExactly(1L, 3L);
        verify(videoLikeRepository).findLikedVideoIdsByUserIdAndVideoIdIn(100L, List.of(1L, 2L, 3L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-INT-02: Пустой запрос возвращает пустой список")
    void likedVideoIds_emptyRequest() {
        VideoIdsRequest request = new VideoIdsRequest(100L, List.of());

        List<Long> result = controller.likedVideoIds(request);

        assertThat(result).isEmpty();
        verify(videoLikeRepository, never()).findLikedVideoIdsByUserIdAndVideoIdIn(anyLong(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-INT-03: Получение просмотренных видео")
    void viewedVideoIds_success() {
        VideoIdsRequest request = new VideoIdsRequest(100L, List.of(1L, 2L, 3L));
        when(videoViewRepository.findViewedVideoIds(anyLong(), any()))
                .thenReturn(Set.of(2L, 3L));

        List<Long> result = controller.viewedVideoIds(request);

        assertThat(result).containsExactlyInAnyOrder(2L, 3L);
        verify(videoViewRepository).findViewedVideoIds(100L, List.of(1L, 2L, 3L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-INT-04: Получение прогресса просмотра")
    void watchProgress_success() {
        VideoIdsRequest request = new VideoIdsRequest(100L, List.of(1L, 2L));
        WatchProgressSummary summary = mock(WatchProgressSummary.class);
        when(summary.getVideoId()).thenReturn(1L);
        when(summary.getTotalWatchedSeconds()).thenReturn(60L);
        when(summary.getDurationSeconds()).thenReturn(120L);

        when(videoWatchRepository.findWatchProgress(anyLong(), any()))
                .thenReturn(List.of(summary));

        List<WatchProgressSummaryDto> result = controller.watchProgress(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).videoId()).isEqualTo(1L);
        assertThat(result.get(0).totalWatchedSeconds()).isEqualTo(60L);
        verify(videoWatchRepository).findWatchProgress(100L, List.of(1L, 2L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-INT-05: Получение количества комментариев")
    void commentsCounts_success() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        VideoUuidsRequest request = new VideoUuidsRequest(List.of(uuid1, uuid2));

        CommentRepository.CommentCountView view = mock(CommentRepository.CommentCountView.class);
        when(view.getVideoUuid()).thenReturn(uuid1);
        when(view.getCount()).thenReturn(5L);

        when(commentRepository.countByVideoUuidIn(any())).thenReturn(List.of(view));

        List<VideoCommentsCountDto> result = controller.commentsCounts(request);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).videoUuid()).isEqualTo(uuid1);
        assertThat(result.get(0).commentsCount()).isEqualTo(5L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-INT-INT-06: Пустой запрос комментариев")
    void commentsCounts_emptyRequest() {
        VideoUuidsRequest request = new VideoUuidsRequest(List.of());

        List<VideoCommentsCountDto> result = controller.commentsCounts(request);

        assertThat(result).isEmpty();
        verify(commentRepository, never()).countByVideoUuidIn(any());
    }
}
