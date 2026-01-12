package ru.itmo.zavar.brainstop.transcoding.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@FR({"FR-3.1"})
@Layer(TestLayer.BE_UNIT)
class TranscodingInternalControllerParameterizedTest {

    @Mock
    private VideoTranscodingService transcoder;

    @Mock
    private TranscodingJobRegistry jobRegistry;

    private TranscodingInternalController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new TranscodingInternalController(transcoder, jobRegistry);
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 100L, 999L, 1000000L, Long.MAX_VALUE})
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PARAM-01: start с различными userId")
    void start_withVariousUserIds(Long userId) throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "tmp", userId, "tmp", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCodeValue()).isEqualTo(202);
        verify(jobRegistry).put(eq(command.jobId()), any());
    }

    @ParameterizedTest
    @MethodSource("provideHeightsCombinations")
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PARAM-02: start с различными комбинациями высот")
    void start_withVariousHeightsCombinations(List<Integer> heights) throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "tmp", 1L, "tmp", heights
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCodeValue()).isEqualTo(202);
    }

    private static Stream<Arguments> provideHeightsCombinations() {
        return Stream.of(
                Arguments.of(List.of(360)),
                Arguments.of(List.of(720)),
                Arguments.of(List.of(1080)),
                Arguments.of(List.of(360, 720)),
                Arguments.of(List.of(360, 720, 1080)),
                Arguments.of(List.of(144, 240, 360, 480, 720, 1080)),
                Arguments.of(List.of())
        );
    }

    @ParameterizedTest
    @CsvSource({
            "video.mp4, key1",
            "test-video.mkv, key2",
            "movie.avi, key3",
            "clip.mov, key4"
    })
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PARAM-03: start с различными именами файлов и ключами")
    void start_withVariousFileNamesAndKeys(String videoName, String storageKey) throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), storageKey, videoName, "tmp", 1L, "tmp", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCodeValue()).isEqualTo(202);
    }

    @ParameterizedTest
    @MethodSource("provideTranscodingStages")
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PARAM-04: status возвращает различные стадии")
    void status_returnsDifferentStages(TranscodingJobStage stage, int percent) {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                stage, percent, stage.name(), "video.mp4", 1L
        );
        when(jobRegistry.get(jobId)).thenReturn(Optional.of(status));

        ResponseEntity<TranscodingJobStatus> response = controller.status(jobId);

        assertThat(response.getBody().getStage()).isEqualTo(stage);
        assertThat(response.getBody().getPercent()).isEqualTo(percent);
    }

    private static Stream<Arguments> provideTranscodingStages() {
        return Stream.of(
                Arguments.of(TranscodingJobStage.UPLOADING, 0),
                Arguments.of(TranscodingJobStage.TRANSCODING, 50),
                Arguments.of(TranscodingJobStage.UPLOADING_TO_MINIO, 90),
                Arguments.of(TranscodingJobStage.DONE, 100),
                Arguments.of(TranscodingJobStage.ERROR, 100)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   ", "\t", "\n"})
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-PARAM-05: start с пустыми storageKey")
    void start_withEmptyStorageKeys(String emptyKey) throws Exception {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), emptyKey, "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        ResponseEntity<Void> response = controller.start(command);

        assertThat(response.getStatusCodeValue()).isEqualTo(400);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 10, 50, 99, 100})
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-PARAM-06: status с различными процентами выполнения")
    void status_withVariousPercentages(int percent) {
        UUID jobId = UUID.randomUUID();
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, percent, "Processing", "video.mp4", 1L
        );
        when(jobRegistry.get(jobId)).thenReturn(Optional.of(status));

        ResponseEntity<TranscodingJobStatus> response = controller.status(jobId);

        assertThat(response.getBody().getPercent()).isEqualTo(percent);
    }
}
