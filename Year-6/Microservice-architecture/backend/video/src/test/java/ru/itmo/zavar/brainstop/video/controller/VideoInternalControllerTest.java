package ru.itmo.zavar.brainstop.video.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.itmo.zavar.brainstop.common.api.PageResponse;
import ru.itmo.zavar.brainstop.common.api.video.VideoProfileApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.PublicUserVideoDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.4"})
@Layer(TestLayer.BE_UNIT)
class VideoInternalControllerTest {

    @Mock
    private VideoProfileApi videoProfileApi;

    @InjectMocks
    private VideoInternalController controller;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-03: getUserVideos с сортировкой по uploadedAt")
    void getUserVideos_withSortByUploadedAt() {
        Page<PublicUserVideoDto> page = new PageImpl<>(List.of());
        when(videoProfileApi.findUserVideos(anyLong(), any(), any())).thenReturn(page);

        controller.getUserVideos(1L, 0, 10, "uploadedAt,desc", null);

        verify(videoProfileApi).findUserVideos(eq(1L), eq(null), any(Pageable.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-04: getUserVideos с сортировкой по likesCount")
    void getUserVideos_withSortByLikesCount() {
        Page<PublicUserVideoDto> page = new PageImpl<>(List.of());
        when(videoProfileApi.findUserVideos(anyLong(), any(), any())).thenReturn(page);

        controller.getUserVideos(1L, 0, 10, "likesCount,asc", null);

        verify(videoProfileApi).findUserVideos(eq(1L), eq(null), any(Pageable.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-05: getUserVideos с некорректной сортировкой использует дефолт")
    void getUserVideos_withInvalidSort_usesDefault() {
        Page<PublicUserVideoDto> page = new PageImpl<>(List.of());
        when(videoProfileApi.findUserVideos(anyLong(), any(), any())).thenReturn(page);

        controller.getUserVideos(1L, 0, 10, "invalidField,desc", null);

        verify(videoProfileApi).findUserVideos(eq(1L), eq(null), any(Pageable.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-07: getUserVideos возвращает пустой список")
    void getUserVideos_returnsEmptyList() {
        Page<PublicUserVideoDto> page = new PageImpl<>(List.of());
        when(videoProfileApi.findUserVideos(anyLong(), any(), any())).thenReturn(page);

        PageResponse<PublicUserVideoDto> response = controller.getUserVideos(1L, 0, 10, null, null);

        assertThat(response.content()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-09: getUserVideos с сортировкой viewsCount")
    void getUserVideos_withSortByViewsCount() {
        Page<PublicUserVideoDto> page = new PageImpl<>(List.of());
        when(videoProfileApi.findUserVideos(anyLong(), any(), any())).thenReturn(page);

        controller.getUserVideos(1L, 0, 10, "viewsCount,desc", null);

        verify(videoProfileApi).findUserVideos(eq(1L), eq(null), any(Pageable.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-INT-10: getUserVideos с пустой строкой сортировки")
    void getUserVideos_withEmptySortString() {
        Page<PublicUserVideoDto> page = new PageImpl<>(List.of());
        when(videoProfileApi.findUserVideos(anyLong(), any(), any())).thenReturn(page);

        controller.getUserVideos(1L, 0, 10, "", null);

        verify(videoProfileApi).findUserVideos(eq(1L), eq(null), any(Pageable.class));
    }
}
