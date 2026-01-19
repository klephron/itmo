package ru.itmo.zavar.brainstop.video.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.interactions.dto.VideoIdsRequest;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.interactions.client.InteractionsInternalClient;
import ru.itmo.zavar.brainstop.like.service.LikeQueryApiImpl;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-2.2"})
@Layer(TestLayer.BE_UNIT)
class LikeQueryApiImplTest {

    @Mock
    private InteractionsInternalClient interactionsInternalClient;

    @InjectMocks
    private LikeQueryApiImpl likeQueryApi;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-API-01: findLikedVideoIds возвращает liked videos")
    void findLikedVideoIds_returnsLiked() {
        when(interactionsInternalClient.getLikedVideoIds(any(VideoIdsRequest.class)))
                .thenReturn(List.of(1L, 2L, 3L));

        List<Long> result = likeQueryApi.findLikedVideoIds(1L, Set.of(1L, 2L, 3L, 4L));

        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-API-02: findLikedVideoIds с null userId")
    void findLikedVideoIds_nullUserId() {
        List<Long> result = likeQueryApi.findLikedVideoIds(null, Set.of(1L, 2L));

        assertThat(result).isEmpty();
        verify(interactionsInternalClient, never()).getLikedVideoIds(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-API-03: findLikedVideoIds с пустыми videoIds")
    void findLikedVideoIds_emptyVideoIds() {
        List<Long> result = likeQueryApi.findLikedVideoIds(1L, Set.of());

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-LIKE-API-04: findLikedVideoIds с null videoIds")
    void findLikedVideoIds_nullVideoIds() {
        List<Long> result = likeQueryApi.findLikedVideoIds(1L, null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-API-05: findLikedVideoIds обрабатывает null response")
    void findLikedVideoIds_nullResponse() {
        when(interactionsInternalClient.getLikedVideoIds(any())).thenReturn(null);

        List<Long> result = likeQueryApi.findLikedVideoIds(1L, Set.of(1L, 2L));

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LIKE-API-06: findLikedVideoIds с пустым response")
    void findLikedVideoIds_emptyResponse() {
        when(interactionsInternalClient.getLikedVideoIds(any())).thenReturn(List.of());

        List<Long> result = likeQueryApi.findLikedVideoIds(1L, Set.of(1L, 2L));

        assertThat(result).isEmpty();
    }
}
