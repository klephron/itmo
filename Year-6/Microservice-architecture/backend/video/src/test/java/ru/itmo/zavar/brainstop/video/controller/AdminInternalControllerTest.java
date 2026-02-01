package ru.itmo.zavar.brainstop.video.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.admin.controller.AdminInternalController;
import ru.itmo.zavar.brainstop.common.api.video.VideoAdminApi;
import ru.itmo.zavar.brainstop.common.api.video.VideoModerationApi;
import ru.itmo.zavar.brainstop.common.api.video.dto.UserVideoDto;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.1", "FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class AdminInternalControllerTest {

    @Mock
    private VideoAdminApi videoAdminApi;

    @Mock
    private VideoModerationApi videoModerationApi;

    @InjectMocks
    private AdminInternalController controller;

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-ADMIN-01: regenerateThumbnails вызывает API")
    void regenerateThumbnails_callsApi() {
        when(videoAdminApi.regenerateThumbnails(true)).thenReturn(5);

        ResponseEntity<Map<String, Object>> response = controller.regenerateThumbnails(true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("updated", 5);
        assertThat(response.getBody()).containsEntry("force", true);
        verify(videoAdminApi).regenerateThumbnails(true);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-ADMIN-02: regenerateThumbnails с force=false")
    void regenerateThumbnails_withForceFalse() {
        when(videoAdminApi.regenerateThumbnails(false)).thenReturn(0);

        ResponseEntity<Map<String, Object>> response = controller.regenerateThumbnails(false);

        assertThat(response.getBody()).containsEntry("force", false);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-ADMIN-04: getUserVideos с пустым списком")
    void getUserVideos_withEmptyList() {
        when(videoModerationApi.findByUploaderId(999L)).thenReturn(List.of());

        ResponseEntity<List<UserVideoDto>> response = controller.getUserVideos(999L);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-ADMIN-05: deleteVideo удаляет видео")
    void deleteVideo_deletesVideo() {
        UUID uuid = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteVideo(uuid);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(videoAdminApi).deleteVideo(uuid);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-ADMIN-06: regenerateThumbnails возвращает 0 если нет обновлений")
    void regenerateThumbnails_returnsZeroIfNoUpdates() {
        when(videoAdminApi.regenerateThumbnails(true)).thenReturn(0);

        ResponseEntity<Map<String, Object>> response = controller.regenerateThumbnails(true);

        assertThat(response.getBody()).containsEntry("updated", 0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-ADMIN-08: regenerateThumbnails с большим количеством обновлений")
    void regenerateThumbnails_withLargeUpdateCount() {
        when(videoAdminApi.regenerateThumbnails(true)).thenReturn(100);

        ResponseEntity<Map<String, Object>> response = controller.regenerateThumbnails(true);

        assertThat(response.getBody()).containsEntry("updated", 100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-ADMIN-09: rebuildEmbeddings вызывает VideoAdminApi")
    void rebuildEmbeddings_callsApi() {
        when(videoAdminApi.rebuildEmbeddings()).thenReturn(7);

        ResponseEntity<Map<String, Object>> response = controller.rebuildEmbeddings();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("updated", 7);
        verify(videoAdminApi).rebuildEmbeddings();
    }
}
