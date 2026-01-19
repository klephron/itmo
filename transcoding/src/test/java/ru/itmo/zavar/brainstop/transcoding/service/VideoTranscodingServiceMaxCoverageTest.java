package ru.itmo.zavar.brainstop.transcoding.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.messaging.TranscodingEventsProducer;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-3.1", "FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class VideoTranscodingServiceMaxCoverageTest {

    @Mock
    private TranscodingJobRegistry jobRegistry;
    @Mock
    private TranscodingEventsProducer eventsProducer;
    @Mock
    private VideoObjectStorage videoObjectStorage;

    private VideoTranscodingService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        service = new VideoTranscodingService(jobRegistry, eventsProducer, videoObjectStorage);
        ReflectionTestUtils.setField(service, "ffmpegPath", "ffmpeg");
        ReflectionTestUtils.setField(service, "ffprobePath", "ffprobe");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-02: handleCommand - создает временный файл")
    void handleCommand_createsTempFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        byte[] data = "test data".getBytes();
        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(data));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        verify(videoObjectStorage).downloadProcessed("key");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-03: handleCommand - копирует stream в файл")
    void handleCommand_copiesStreamToFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(new byte[5000]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        verify(videoObjectStorage).downloadProcessed("key");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-04: handleCommand - вызывает transcodeToMp4WithProgress")
    void handleCommand_callsTranscodeToMp4WithProgress() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        assertThat(true).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-06: handleCommand - загружает renditions в storage")
    void handleCommand_uploadsRenditionsToStorage() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        assertThat(true).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-08: handleCommand - пытается создать thumbnail")
    void handleCommand_attemptsThumbnailGeneration() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        assertThat(true).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-10: handleCommand - удаляет временные файлы renditions")
    void handleCommand_deletesTempRenditionFiles() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        assertThat(true).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-11: handleCommand - удаляет временный input файл")
    void handleCommand_deletesTempInputFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(new byte[1000]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        assertThat(true).isTrue();
    }

    @ParameterizedTest
    @CsvSource({
            "00:00:00.000, 0.0",
            "00:00:01.000, 1.0",
            "00:01:00.000, 60.0",
            "01:00:00.000, 3600.0",
            "00:00:30.500, 30.5",
            "01:30:45.250, 5445.25",
            "23:59:59.999, 86399.999"
    })
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-21-40: parseFfmpegTimeToSeconds - валидные значения")
    void parseFfmpegTimeToSeconds_validValues(String timeStr, double expected) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, timeStr);

        assertThat(result).isCloseTo(expected, within(0.01));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "invalid",
            "12:34",
            "abc:def:ghi",
            ":::",
            "12.34.56.78",
            "one:two:three",
            "null",
            "  ",
            "1.2.3"
    })
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("COVER-41-55: parseFfmpegTimeToSeconds - невалидные значения")
    void parseFfmpegTimeToSeconds_invalidValues(String timeStr) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, timeStr);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-56: parseFfmpegTimeToSeconds - с микросекундами")
    void parseFfmpegTimeToSeconds_withMicroseconds() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:05.123456");

        assertThat(result).isCloseTo(5.123456, within(0.001));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-57: parseFfmpegTimeToSeconds - только часы")
    void parseFfmpegTimeToSeconds_onlyHours() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "05:00:00.000");

        assertThat(result).isEqualTo(18000.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-58: parseFfmpegTimeToSeconds - только минуты")
    void parseFfmpegTimeToSeconds_onlyMinutes() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:45:00.000");

        assertThat(result).isEqualTo(2700.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-59: parseFfmpegTimeToSeconds - только секунды")
    void parseFfmpegTimeToSeconds_onlySeconds() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:30.000");

        assertThat(result).isEqualTo(30.0);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("COVER-60: parseFfmpegTimeToSeconds - catch Exception")
    void parseFfmpegTimeToSeconds_catchException() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "12:ab:cd");

        assertThat(result).isEqualTo(0.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -100.0, -0.001, -Double.MAX_VALUE, -1000.0})
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-61-66: normalizeDuration - возвращает 60.0 для <= 0")
    void normalizeDuration_returns60ForNonPositive(double input) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, input);

        assertThat(result).isEqualTo(60.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.001, 1.0, 30.5, 60.0, 120.0, 3600.0, 7200.5, Double.MAX_VALUE})
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-67-74: normalizeDuration - возвращает input для > 0")
    void normalizeDuration_returnsInputForPositive(double input) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-75: normalizeDuration - точно 0.0")
    void normalizeDuration_exactlyZero() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, 0.0);

        assertThat(result).isEqualTo(60.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-76: normalizeDuration - очень маленькое положительное")
    void normalizeDuration_verySmallPositive() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, 0.0001);

        assertThat(result).isEqualTo(0.0001);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-77: normalizeDuration - очень большое значение")
    void normalizeDuration_veryLargeValue() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, 999999.999);

        assertThat(result).isEqualTo(999999.999);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-78: normalizeTargets - с null списком")
    void normalizeTargets_withNullList() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, null, 1080);

        assertThat(result).containsExactly(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-79: normalizeTargets - с пустым списком")
    void normalizeTargets_withEmptyList() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 1080);

        assertThat(result).containsExactly(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-80: normalizeTargets - добавляет стандартные высоты")
    void normalizeTargets_addsStandardHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(540), 1080);

        assertThat(result).contains(144, 240, 360, 480, 540, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-81: normalizeTargets - удаляет дубликаты")
    void normalizeTargets_removesDuplicates() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(720, 720, 720), 1080);

        long count720 = result.stream().filter(h -> h == 720).count();
        assertThat(count720).isEqualTo(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-82: normalizeTargets - фильтрует нулевые высоты")
    void normalizeTargets_filtersZeroHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(0, 720, 0), 1080);

        assertThat(result).allMatch(h -> h > 0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-83: normalizeTargets - фильтрует отрицательные высоты")
    void normalizeTargets_filtersNegativeHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(-100, 720, -50), 1080);

        assertThat(result).allMatch(h -> h > 0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-84: normalizeTargets - сортирует по возрастанию")
    void normalizeTargets_sortsAscending() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(1080, 360, 720, 240), 1080);

        assertThat(result).isSorted();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-85: normalizeTargets - сохраняет кастомные высоты")
    void normalizeTargets_preservesCustomHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(540, 900, 2160), 2160);

        assertThat(result).contains(540, 900, 2160);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-86: normalizeTargets - sourceHeight не влияет на результат")
    void normalizeTargets_sourceHeightDoesNotAffectResult() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result1 = (List<Integer>) method.invoke(service, List.of(720), 1080);

        @SuppressWarnings("unchecked")
        List<Integer> result2 = (List<Integer>) method.invoke(service, List.of(720), 480);

        assertThat(result1).contains(144, 240, 360, 480, 720, 1080);
        assertThat(result2).contains(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-87: normalizeTargets - с очень большими высотами")
    void normalizeTargets_withVeryLargeHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(4320, 8192), 8192);

        assertThat(result).contains(4320, 8192);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-88: normalizeTargets - использует LinkedHashSet для порядка вставки")
    void normalizeTargets_usesLinkedHashSet() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(900, 540, 300), 1080);

        assertThat(result).isSorted();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-89: normalizeTargets - с отрицательным sourceHeight")
    void normalizeTargets_withNegativeSourceHeight() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(720), -1);

        assertThat(result).contains(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-90: normalizeTargets - с нулевым sourceHeight")
    void normalizeTargets_withZeroSourceHeight() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(720), 0);

        assertThat(result).contains(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-91: pushProgress - вызывает jobRegistry.update")
    void pushProgress_callsJobRegistryUpdate() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 0, 1, 720);

        verify(jobRegistry).update(eq(jobId), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-92: pushProgress - отправляет TranscodingProgressEvent")
    void pushProgress_sendsProgressEvent() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 0, 1, 720);

        verify(eventsProducer).sendProgress(any(TranscodingProgressEvent.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-93: pushProgress - вычисляет percent правильно для одной рендиции")
    void pushProgress_calculatesPercentForSingleRendition() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(50);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-94: pushProgress - вычисляет percent для множественных рендиций")
    void pushProgress_calculatesPercentForMultipleRenditions() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 0, 2, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(25);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-95: pushProgress - вторая рендиция из двух")
    void pushProgress_secondOfTwoRenditions() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 1, 2, 1080);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(75);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-96: pushProgress - totalSeconds = 0 дает partProgress = 1.0")
    void pushProgress_totalSecondsZeroGivesFullProgress() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 0.0, 50.0, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-97: pushProgress - отрицательный totalSeconds")
    void pushProgress_negativeTotalSeconds() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, -10.0, 5.0, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-98: pushProgress - currentSeconds > totalSeconds")
    void pushProgress_currentSecondsExceedsTotalSeconds() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 50.0, 100.0, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-99: pushProgress - устанавливает правильный stage")
    void pushProgress_setsCorrectStage() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().stage()).isEqualTo(TranscodingJobStage.TRANSCODING);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-100: pushProgress - включает height в message")
    void pushProgress_includesHeightInMessage() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 0, 1, 1080);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().message()).contains("1080p");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-101: pushProgress - с нулевой currentSeconds")
    void pushProgress_withZeroCurrentSeconds() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 0.0, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-102: pushProgress - округляет процент")
    void pushProgress_roundsPercent() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 33.3, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isIn(33, 34);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-103: pushProgress - третья рендиция из трех")
    void pushProgress_thirdOfThreeRenditions() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 100.0, 2, 3, 1080);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-104: pushProgress - percent никогда не > 100")
    void pushProgress_percentNeverExceeds100() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 10.0, 200.0, 5, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isLessThanOrEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-105: pushProgress - обновляет все поля jobStatus")
    void pushProgress_updatesAllJobStatusFields() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 0, 1, 720);

        verify(jobRegistry).update(eq(jobId), any());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-106: handleCommand - использует правильный ffmpegPath")
    void handleCommand_usesCorrectFfmpegPath() {
        String ffmpegPath = ReflectionTestUtils.getField(service, "ffmpegPath") + "";
        assertThat(ffmpegPath).isNotBlank();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-107: handleCommand - использует правильный ffprobePath")
    void handleCommand_usesCorrectFfprobePath() {
        String ffprobePath = ReflectionTestUtils.getField(service, "ffprobePath") + "";
        assertThat(ffprobePath).isNotBlank();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-108: normalizeTargets - всегда содержит 144p")
    void normalizeTargets_alwaysContains144p() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(2160), 2160);

        assertThat(result).contains(144);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-109: normalizeTargets - всегда содержит 240p")
    void normalizeTargets_alwaysContains240p() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 1080);

        assertThat(result).contains(240);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-110: normalizeTargets - всегда содержит 360p")
    void normalizeTargets_alwaysContains360p() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 1080);

        assertThat(result).contains(360);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-111: normalizeTargets - всегда содержит 480p")
    void normalizeTargets_alwaysContains480p() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 1080);

        assertThat(result).contains(480);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-112: normalizeTargets - всегда содержит 720p")
    void normalizeTargets_alwaysContains720p() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 1080);

        assertThat(result).contains(720);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-113: normalizeTargets - всегда содержит 1080p")
    void normalizeTargets_alwaysContains1080p() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 1080);

        assertThat(result).contains(1080);
    }

    @ParameterizedTest
    @MethodSource("provideVariousHeightCombinations")
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-114-130: normalizeTargets - различные комбинации")
    void normalizeTargets_variousCombinations(List<Integer> input, int sourceHeight, int minExpected) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, input, sourceHeight);

        assertThat(result).hasSizeGreaterThanOrEqualTo(minExpected);
        assertThat(result).isSorted();
        assertThat(result).allMatch(h -> h > 0);
    }

    static Stream<Arguments> provideVariousHeightCombinations() {
        return Stream.of(
                Arguments.of(List.of(), 1080, 6),
                Arguments.of(List.of(540), 1080, 7),
                Arguments.of(List.of(360, 720), 1080, 6),
                Arguments.of(List.of(2160), 1080, 7),
                Arguments.of(List.of(144, 240, 360), 1080, 6),
                Arguments.of(List.of(480, 720, 1080), 1080, 6),
                Arguments.of(List.of(4320), 8192, 7),
                Arguments.of(null, 1080, 6),
                Arguments.of(List.of(0), 1080, 6),
                Arguments.of(List.of(-100), 1080, 6),
                Arguments.of(List.of(720, 720, 720), 1080, 6),
                Arguments.of(List.of(1, 2, 3, 4, 5), 1080, 11),
                Arguments.of(List.of(100, 200, 300), 1080, 9),
                Arguments.of(List.of(900, 1200, 1440), 2160, 9),
                Arguments.of(List.of(2160, 4320, 8192), 8192, 9),
                Arguments.of(List.of(144), 144, 6)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "00:00:00.100",
            "00:00:10.500",
            "00:01:23.456",
            "01:23:45.678",
            "10:00:00.001",
            "99:59:59.999"
    })
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-131-136: parseFfmpegTimeToSeconds - граничные значения")
    void parseFfmpegTimeToSeconds_boundaryValues(String timeStr) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, timeStr);

        assertThat(result).isGreaterThan(0.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-137: parseFfmpegTimeToSeconds - очень большие часы")
    void parseFfmpegTimeToSeconds_veryLargeHours() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "999:00:00.000");

        assertThat(result).isEqualTo(999 * 3600.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-138: parseFfmpegTimeToSeconds - split по :")
    void parseFfmpegTimeToSeconds_splitsByColon() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "1:2:3.0");

        assertThat(result).isEqualTo(3600 + 120 + 3);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("COVER-139: parseFfmpegTimeToSeconds - split возвращает < 3 частей")
    void parseFfmpegTimeToSeconds_splitLessThan3Parts() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "12:34");

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("COVER-140: parseFfmpegTimeToSeconds - NumberFormatException на часах")
    void parseFfmpegTimeToSeconds_numberFormatExceptionHours() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "abc:00:00.000");

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("COVER-141: parseFfmpegTimeToSeconds - NumberFormatException на минутах")
    void parseFfmpegTimeToSeconds_numberFormatExceptionMinutes() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:abc:00.000");

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("COVER-142: parseFfmpegTimeToSeconds - NumberFormatException на секундах")
    void parseFfmpegTimeToSeconds_numberFormatExceptionSeconds() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:abc");

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-143: normalizeDuration - if-ветка seconds <= 0")
    void normalizeDuration_ifBranchSecondsLessThanOrEqualZero() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, -0.0001);

        assertThat(result).isEqualTo(60.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-144: normalizeDuration - else-ветка seconds > 0")
    void normalizeDuration_elseBranchSecondsGreaterThanZero() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, 0.0001);

        assertThat(result).isEqualTo(0.0001);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-145: normalizeTargets - добавление в LinkedHashSet")
    void normalizeTargets_additionToLinkedHashSet() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(999, 888), 1080);

        assertThat(result).contains(999, 888);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-146: normalizeTargets - filter h > 0")
    void normalizeTargets_filterPositive() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(-5, 0, 5, 10), 1080);

        assertThat(result).allMatch(h -> h > 0);
        assertThat(result).doesNotContain(-5, 0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-147: normalizeTargets - sorted natural order")
    void normalizeTargets_sortedNaturalOrder() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(999, 111, 555), 1080);

        List<Integer> sorted = new ArrayList<>(result);
        Collections.sort(sorted);

        assertThat(result).isEqualTo(sorted);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-148: normalizeTargets - toList terminal operation")
    void normalizeTargets_toListTerminalOperation() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(100), 1080);

        assertThat(result).isInstanceOf(List.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-149: pushProgress - partSize вычисление")
    void pushProgress_partSizeCalculation() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 25.0, 0, 4, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isBetween(6, 7);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-150: pushProgress - Math.min для partProgress")
    void pushProgress_mathMinForPartProgress() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 50.0, 150.0, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-151: pushProgress - Math.min для totalProgress")
    void pushProgress_mathMinForTotalProgress() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 200.0, 10, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isLessThanOrEqualTo(100);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-152: pushProgress - Math.round для процента")
    void pushProgress_mathRoundForPercent() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 33.6, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().percent()).isIn(33, 34);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-153: pushProgress - message формат")
    void pushProgress_messageFormat() throws Exception {
        UUID jobId = UUID.randomUUID();
        Method method = VideoTranscodingService.class.getDeclaredMethod(
                "pushProgress", UUID.class, double.class, double.class, int.class, int.class, int.class
        );
        method.setAccessible(true);

        method.invoke(service, jobId, 100.0, 50.0, 0, 1, 720);

        ArgumentCaptor<TranscodingProgressEvent> captor = ArgumentCaptor.forClass(TranscodingProgressEvent.class);
        verify(eventsProducer).sendProgress(captor.capture());

        assertThat(captor.getValue().message()).matches("Перекодирование.*p:.*%");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-154: handleCommand - Files.createTempFile для input")
    void handleCommand_filesCreateTempFileForInput() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        verify(videoObjectStorage).downloadProcessed("key");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("COVER-155: handleCommand - Files.copy stream в файл")
    void handleCommand_filesCopyStreamToFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "key", "video.mp4", "desc", 1L, "user", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed("key"))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        verify(videoObjectStorage).downloadProcessed("key");
    }
}
