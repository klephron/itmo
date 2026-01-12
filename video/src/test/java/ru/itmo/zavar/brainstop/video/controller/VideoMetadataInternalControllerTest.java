package ru.itmo.zavar.brainstop.video.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataPublicApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoMetadataSummary;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-1.4"})
@Layer(TestLayer.BE_UNIT)
class VideoMetadataInternalControllerTest {

    @Mock
    private VideoMetadataPublicApi videoMetadataPublicApi;

    @InjectMocks
    private VideoMetadataInternalController controller;

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-META-02: getSummary возвращает 404 для несуществующего видео")
    void getSummary_returns404ForNonExistent() {
        UUID uuid = UUID.randomUUID();
        when(videoMetadataPublicApi.findByUuid(uuid)).thenReturn(Optional.empty());

        ResponseEntity<VideoMetadataSummary> response = controller.getSummary(uuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-META-03: incrementLikes увеличивает счетчик")
    void incrementLikes_incrementsCounter() {
        when(videoMetadataPublicApi.incrementLikesCount(1L, 1L)).thenReturn(11L);

        ResponseEntity<Long> response = controller.incrementLikes(1L, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(11L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-META-04: incrementLikes возвращает 404 для несуществующего видео")
    void incrementLikes_returns404ForNonExistent() {
        when(videoMetadataPublicApi.incrementLikesCount(999L, 1L)).thenReturn(-1L);

        ResponseEntity<Long> response = controller.incrementLikes(999L, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-META-05: incrementViews увеличивает просмотры")
    void incrementViews_incrementsCounter() {
        when(videoMetadataPublicApi.incrementViewsCount(1L, 5L)).thenReturn(105L);

        ResponseEntity<Long> response = controller.incrementViews(1L, 5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(105L);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-META-06: incrementViews возвращает 404 для несуществующего видео")
    void incrementViews_returns404ForNonExistent() {
        when(videoMetadataPublicApi.incrementViewsCount(999L, 1L)).thenReturn(-1L);

        ResponseEntity<Long> response = controller.incrementViews(999L, 1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-META-07: updateLikes устанавливает новое значение")
    void updateLikes_setsNewValue() {
        ResponseEntity<Void> response = controller.updateLikes(1L, 50L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(videoMetadataPublicApi).updateLikesCount(1L, 50L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-META-08: updateViews устанавливает новое значение")
    void updateViews_setsNewValue() {
        ResponseEntity<Void> response = controller.updateViews(1L, 500L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(videoMetadataPublicApi).updateViewsCount(1L, 500L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-META-09: incrementLikes с отрицательной delta")
    void incrementLikes_withNegativeDelta() {
        when(videoMetadataPublicApi.incrementLikesCount(1L, -1L)).thenReturn(9L);

        ResponseEntity<Long> response = controller.incrementLikes(1L, -1L);

        assertThat(response.getBody()).isEqualTo(9L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-META-10: incrementViews с большой delta")
    void incrementViews_withLargeDelta() {
        when(videoMetadataPublicApi.incrementViewsCount(1L, 1000L)).thenReturn(2000L);

        ResponseEntity<Long> response = controller.incrementViews(1L, 1000L);

        assertThat(response.getBody()).isEqualTo(2000L);
    }
}
