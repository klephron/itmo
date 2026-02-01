package ru.itmo.zavar.brainstop.transcoding.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
@FR({"FR-3.1", "FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class TranscodingInternalControllerTest {

    @Mock
    private VideoTranscodingService transcoder;

    @Mock
    private TranscodingJobRegistry jobRegistry;

    @InjectMocks
    private TranscodingInternalController controller;

    private UUID testJobId;
    private TranscodingCommand testCommand;

    @BeforeEach
    void setUp() {
        testJobId = UUID.randomUUID();
        testCommand = new TranscodingCommand(
            testJobId,
            "test-key",
            "test-video.mp4",
            "tmp",
            123L,
            "tmp",
            List.of(720, 1080)
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-01: start успешно запускает транскодинг")
    void start_successfullyStartsTranscoding() throws Exception {
        ResponseEntity<Void> response = controller.start(testCommand);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(jobRegistry).put(eq(testJobId), any(TranscodingJobStatus.class));
        verify(transcoder).handleCommand(testCommand);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-CTL-02: start возвращает bad request для null команды")
    void start_returnsBadRequestForNullCommand() throws Exception {
        ResponseEntity<Void> response = controller.start(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(transcoder, never()).handleCommand(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-CTL-03: start возвращает bad request если storageKey пустой")
    void start_returnsBadRequestForEmptyStorageKey() throws Exception {
        TranscodingCommand invalidCommand = new TranscodingCommand(
                testJobId, "", "video.mp4", "tmp", 123L, "tmp", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(invalidCommand);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(transcoder, never()).handleCommand(any());
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-CTL-04: start возвращает bad request если jobId null")
    void start_returnsBadRequestForNullJobId() throws Exception {
        TranscodingCommand invalidCommand = new TranscodingCommand(
                null, "key", "video.mp4", "tmp", 123L, "tmp", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(invalidCommand);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(transcoder, never()).handleCommand(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-06: status возвращает статус джоба")
    void status_returnsJobStatus() {
        TranscodingJobStatus expectedStatus = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "Processing", "video.mp4", 123L
        );
        when(jobRegistry.get(testJobId)).thenReturn(Optional.of(expectedStatus));

        ResponseEntity<TranscodingJobStatus> response = controller.status(testJobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expectedStatus);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-CTL-07: status возвращает 404 для несуществующего джоба")
    void status_returns404ForNonExistentJob() {
        when(jobRegistry.get(testJobId)).thenReturn(Optional.empty());

        ResponseEntity<TranscodingJobStatus> response = controller.status(testJobId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-08: start с множественными высотами")
    void start_withMultipleHeights() throws Exception {
        TranscodingCommand multiHeightCommand = new TranscodingCommand(
                testJobId, "key", "video.mp4", "tmp", 123L, "tmp", List.of(360, 480, 720, 1080)
        );

        ResponseEntity<Void> response = controller.start(multiHeightCommand);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(transcoder).handleCommand(multiHeightCommand);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-09: start с пустым списком высот")
    void start_withEmptyHeightsList() throws Exception {
        TranscodingCommand emptyHeightsCommand = new TranscodingCommand(
                testJobId, "key", "video.mp4", "tmp", 123L, "tmp", List.of()
        );

        ResponseEntity<Void> response = controller.start(emptyHeightsCommand);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(transcoder).handleCommand(emptyHeightsCommand);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-10: start обрабатывает исключение от transcoder")
    void start_handlesTranscoderException() throws Exception {
        doThrow(new RuntimeException("Transcoding failed")).when(transcoder).handleCommand(any());

        try {
            controller.start(testCommand);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Transcoding failed");
        }

        verify(jobRegistry).put(eq(testJobId), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-11: status вызывается с правильным jobId")
    void status_calledWithCorrectJobId() {
        UUID customJobId = UUID.randomUUID();
        when(jobRegistry.get(customJobId)).thenReturn(Optional.empty());

        controller.status(customJobId);

        verify(jobRegistry).get(customJobId);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-12: start с очень длинным именем видео")
    void start_withVeryLongVideoName() throws Exception {
        String longName = "a".repeat(300) + ".mp4";
        TranscodingCommand longNameCommand = new TranscodingCommand(
                testJobId, "key", longName, "tmp", 123L, "tmp", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(longNameCommand);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-13: start с разными userId")
    void start_withDifferentUserIds() throws Exception {
        TranscodingCommand command1 = new TranscodingCommand(UUID.randomUUID(), "key1", "v1.mp4", "tmp", 1L, "tmp", List.of(720));
        TranscodingCommand command2 = new TranscodingCommand(UUID.randomUUID(), "key2", "v2.mp4", "tmp", 999L, "tmp", List.of(720));

        controller.start(command1);
        controller.start(command2);

        verify(transcoder, times(2)).handleCommand(any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-CTL-15: start с null storageKey возвращает bad request")
    void start_withNullStorageKey() throws Exception {
        TranscodingCommand nullKeyCommand = new TranscodingCommand(
                testJobId, null, "video.mp4", "tmp", 123L, "tmp", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(nullKeyCommand);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
