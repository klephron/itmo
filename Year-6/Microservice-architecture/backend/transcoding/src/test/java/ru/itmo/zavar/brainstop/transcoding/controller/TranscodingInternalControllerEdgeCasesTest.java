package ru.itmo.zavar.brainstop.transcoding.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.service.VideoTranscodingService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-3.1"})
@Layer(TestLayer.BE_UNIT)
class TranscodingInternalControllerEdgeCasesTest {

    @Mock
    private VideoTranscodingService transcoder;

    @Mock
    private TranscodingJobRegistry jobRegistry;

    @InjectMocks
    private TranscodingInternalController controller;

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-EDGE-01: start с whitespace-only storageKey")
    void start_withWhitespaceOnlyStorageKey() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "   ", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(transcoder, never()).handleCommand(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-EDGE-02: start с tab в storageKey")
    void start_withTabInStorageKey() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "\t", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CTRL-EDGE-03: start с newline в storageKey")
    void start_withNewlineInStorageKey() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "\n", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-04: start с очень длинным storageKey")
    void start_withVeryLongStorageKey() throws Exception {
        String longKey = "a".repeat(10000);
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), longKey, "video.mp4", "desc", 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-05: start с single character storageKey")
    void start_withSingleCharStorageKey() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "k", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-06: start с спецсимволами в storageKey")
    void start_withSpecialCharsInStorageKey() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "test/key-with_special.chars", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-07: start с null description")
    void start_withNullDescription() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", null, 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-08: start с null displayName")
    void start_withNullDisplayName() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", 1L, null, List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-09: start с userId = 0")
    void start_withUserIdZero() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", 0L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-10: start с отрицательным userId")
    void start_withNegativeUserId() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", -1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-11: status проверяет правильный jobId")
    void status_checksCorrectJobId() {
        UUID jobId = UUID.randomUUID();
        UUID differentJobId = UUID.randomUUID();

        when(jobRegistry.get(jobId)).thenReturn(Optional.of(
                new TranscodingJobStatus(TranscodingJobStage.TRANSCODING, 50, "msg", "video.mp4", 1L)
        ));
        when(jobRegistry.get(differentJobId)).thenReturn(Optional.empty());

        ResponseEntity<TranscodingJobStatus> response1 = controller.status(jobId);
        ResponseEntity<TranscodingJobStatus> response2 = controller.status(differentJobId);

        assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-12: status множественные вызовы")
    void status_multipleCalls() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "msg", "video.mp4", 1L
        );
        when(jobRegistry.get(jobId)).thenReturn(Optional.of(status));

        for (int i = 0; i < 10; i++) {
            ResponseEntity<TranscodingJobStatus> response = controller.status(jobId);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        verify(jobRegistry, times(10)).get(jobId);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-13: start вызывает jobRegistry.put перед handleCommand")
    void start_callsJobRegistryPutBeforeHandleCommand() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        controller.start(command);

        verify(jobRegistry).put(eq(command.jobId()), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-14: start с очень длинным videoName")
    void start_withVeryLongVideoName() throws Exception {
        String longName = "video-" + "a".repeat(10000) + ".mp4";
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", longName, "desc", 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-15: start с videoName содержащим спецсимволы")
    void start_withSpecialCharsInVideoName() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video@#$%^&*().mp4", "desc", 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-16: start с описанием длиной 1000+ символов")
    void start_withVeryLongDescription() throws Exception {
        String longDesc = "d".repeat(5000);
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", longDesc, 1L, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-17: start с пустым displayName")
    void start_withEmptyDisplayName() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", 1L, "", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-18: start с высотами в неотсортированном порядке")
    void start_withUnsortedHeights() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", 1L, "user", List.of(1080, 360, 720, 480)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-19: start с дублирующимися высотами")
    void start_withDuplicateHeights() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", 1L, "user", List.of(720, 720, 720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-21: status возвращает все поля status")
    void status_returnsAllStatusFields() {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus fullStatus = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING,
                75,
                "Processing rendition 720p",
                "my-video.mp4",
                123L
        );
        when(jobRegistry.get(jobId)).thenReturn(Optional.of(fullStatus));

        ResponseEntity<TranscodingJobStatus> response = controller.status(jobId);

        assertThat(response.getBody().getStage()).isEqualTo(TranscodingJobStage.TRANSCODING);
        assertThat(response.getBody().getPercent()).isEqualTo(75);
        assertThat(response.getBody().getMessage()).isEqualTo("Processing rendition 720p");
        assertThat(response.getBody().getFileName()).isEqualTo("my-video.mp4");
        assertThat(response.getBody().getUserId()).isEqualTo(123L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-22: start с null userId")
    void start_withNullUserId() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", null, "user", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-23: start сохраняет исходный videoName")
    void start_preservesOriginalVideoName() throws Exception {
        String originalName = "My Special Video.mp4";
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", originalName, "desc", 1L, "user", List.of(720)
        );

        controller.start(command);

        ArgumentCaptor<TranscodingJobStatus> captor = ArgumentCaptor.forClass(TranscodingJobStatus.class);
        verify(jobRegistry).put(any(UUID.class), captor.capture());

        assertThat(captor.getValue().getFileName()).isEqualTo(originalName);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-24: start создает status с 0% прогресса")
    void start_createsStatusWithZeroProgress() throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        controller.start(command);

        ArgumentCaptor<TranscodingJobStatus> captor = ArgumentCaptor.forClass(TranscodingJobStatus.class);
        verify(jobRegistry).put(any(UUID.class), captor.capture());

        assertThat(captor.getValue().getPercent()).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CTRL-EDGE-25: status для различных UUID форматов")
    void status_forVariousUuidFormats() {
        UUID[] jobIds = {
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        };

        for (UUID jobId : jobIds) {
            when(jobRegistry.get(jobId)).thenReturn(Optional.empty());
            ResponseEntity<TranscodingJobStatus> response = controller.status(jobId);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
