package ru.itmo.zavar.brainstop.interactions.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.like.repo.VideoLikeRepository;
import ru.itmo.zavar.brainstop.like.service.LikeQueryApiImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1"})
@Layer(TestLayer.BE_UNIT)
class LikeQueryApiImplTest {

    @Mock
    private VideoLikeRepository videoLikeRepository;

    @InjectMocks
    private LikeQueryApiImpl likeQueryApi;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-QUERY-01: Получение лайкнутых видео")
    void findLikedVideoIds_success() {
        when(videoLikeRepository.findLikedVideoIdsByUserIdAndVideoIdIn(anyLong(), any()))
                .thenReturn(List.of(1L, 3L, 5L));

        List<Long> result = likeQueryApi.findLikedVideoIds(100L, List.of(1L, 2L, 3L, 4L, 5L));

        assertThat(result).containsExactly(1L, 3L, 5L);
        verify(videoLikeRepository).findLikedVideoIdsByUserIdAndVideoIdIn(100L, List.of(1L, 2L, 3L, 4L, 5L));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-QUERY-02: Null userId возвращает пустой список")
    void findLikedVideoIds_nullUserId() {
        List<Long> result = likeQueryApi.findLikedVideoIds(null, List.of(1L, 2L));

        assertThat(result).isEmpty();
        verify(videoLikeRepository, never()).findLikedVideoIdsByUserIdAndVideoIdIn(anyLong(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-QUERY-03: Пустой список videoIds")
    void findLikedVideoIds_emptyVideoIds() {
        List<Long> result = likeQueryApi.findLikedVideoIds(100L, List.of());

        assertThat(result).isEmpty();
        verify(videoLikeRepository, never()).findLikedVideoIdsByUserIdAndVideoIdIn(anyLong(), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-QUERY-04: Null videoIds возвращает пустой список")
    void findLikedVideoIds_nullVideoIds() {
        List<Long> result = likeQueryApi.findLikedVideoIds(100L, null);

        assertThat(result).isEmpty();
        verify(videoLikeRepository, never()).findLikedVideoIdsByUserIdAndVideoIdIn(anyLong(), any());
    }
}
