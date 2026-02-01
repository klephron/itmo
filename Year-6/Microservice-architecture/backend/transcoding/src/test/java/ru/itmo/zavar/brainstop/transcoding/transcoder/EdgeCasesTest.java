package ru.itmo.zavar.brainstop.transcoding.transcoder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-3.1", "FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class EdgeCasesTest {

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-01: TranscodingCommand с максимальными значениями")
    void transcodingCommand_withMaxValues() {
        UUID jobId = UUID.randomUUID();
        String longName = "a".repeat(1000) + ".mp4";
        List<Integer> manyHeights = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            manyHeights.add(i * 10);
        }

        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", longName, "tmp", Long.MAX_VALUE, "tmp", manyHeights
        );

        assertThat(command.jobId()).isEqualTo(jobId);
        assertThat(command.videoName()).hasSize(1004);
        assertThat(command.targetHeights()).hasSize(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-03: TranscodedRenditionMetadata с нулевым размером")
    void transcodedRenditionMetadata_withZeroSize() {
        TranscodedRenditionMetadata metadata = new TranscodedRenditionMetadata(
                720, "key", 0L, "video/mp4"
        );

        assertThat(metadata.size()).isEqualTo(0L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-04: TranscodingResult с пустым списком рендиций")
    void transcodingResult_withEmptyRenditions() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(), List.of(), null, 720
        );

        assertThat(result.renditions()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-05: TranscodingJobStatus с пустым сообщением")
    void transcodingJobStatus_withEmptyMessage() {
        TranscodingJobStatus status = new TranscodingJobStatus(
                TranscodingJobStage.TRANSCODING, 50, "", "video.mp4", 1L
        );

        assertThat(status.getMessage()).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-06: TranscodingCommand с очень коротким именем")
    void transcodingCommand_withVeryShortName() {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "k", "v", "tmp", 1L, "tmp", List.of(720)
        );

        assertThat(command.videoName()).hasSize(1);
        assertThat(command.storageKey()).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-07: TranscodedRenditionMetadata с очень большим размером")
    void transcodedRenditionMetadata_withVeryLargeSize() {
        TranscodedRenditionMetadata metadata = new TranscodedRenditionMetadata(
                2160, "key", Long.MAX_VALUE, "video/mp4"
        );

        assertThat(metadata.size()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-08: TranscodingProgressEvent с 0% прогресса")
    void transcodingProgressEvent_withZeroProgress() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 0, "Starting", TranscodingJobStage.UPLOADING, Instant.now()
        );

        assertThat(event.percent()).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-09: TranscodingProgressEvent с 100% прогресса")
    void transcodingProgressEvent_with100Progress() {
        TranscodingProgressEvent event = new TranscodingProgressEvent(
                UUID.randomUUID(), 100, "Done", TranscodingJobStage.DONE, Instant.now()
        );

        assertThat(event.percent()).isEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-10: TranscodingResult с множественными рендициями одинаковой высоты")
    void transcodingResult_withMultipleSameHeightRenditions() {
        List<TranscodedRenditionMetadata> renditions = List.of(
                new TranscodedRenditionMetadata(720, "key1", 1000L, "video/mp4"),
                new TranscodedRenditionMetadata(720, "key2", 1100L, "video/mp4"),
                new TranscodedRenditionMetadata(720, "key3", 1200L, "video/mp4")
        );

        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(), renditions, null, 720
        );

        assertThat(result.renditions()).hasSize(3);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-11: все TranscodingJobStage значения")
    void allTranscodingJobStageValues() {
        TranscodingJobStage[] stages = TranscodingJobStage.values();

        assertThat(stages).containsExactlyInAnyOrder(
                TranscodingJobStage.UPLOADING,
                TranscodingJobStage.TRANSCODING,
                TranscodingJobStage.UPLOADING_TO_MINIO,
                TranscodingJobStage.DONE,
                TranscodingJobStage.ERROR
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-12: TranscodingCommand с отрицательным userId (граничный случай)")
    void transcodingCommand_withNegativeUserId() {
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "tmp", -1L, "tmp", List.of(720)
        );

        assertThat(command.userId()).isEqualTo(-1L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-13: TranscodingCommand с огромным списком высот")
    void transcodingCommand_withHugeHeightsList() {
        List<Integer> hugeList = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            hugeList.add(i);
        }

        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), "key", "video.mp4", "tmp", 1L, "tmp", hugeList
        );

        assertThat(command.targetHeights()).hasSize(10000);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-EDGE-15: TranscodingResult с null thumbnailKey")
    void transcodingResult_withNullThumbnailKey() {
        TranscodingResult result = new TranscodingResult(
                UUID.randomUUID(),
                List.of(new TranscodedRenditionMetadata(720, "key", 1000L, "video/mp4")),
                null,
                720
        );

        assertThat(result.thumbnailKey()).isNull();
    }
}
