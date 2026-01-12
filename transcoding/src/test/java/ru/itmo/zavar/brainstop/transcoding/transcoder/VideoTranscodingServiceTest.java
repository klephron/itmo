package ru.itmo.zavar.brainstop.transcoding.transcoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.transcoding.*;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.messaging.TranscodingEventsProducer;
import ru.itmo.zavar.brainstop.transcoding.service.VideoTranscodingService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-3.1", "FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class VideoTranscodingServiceTest {

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
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-01: normalizeDuration возвращает положительное значение")
    void normalizeDuration_returnsPositiveValue() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, 120.5);
        assertThat(result).isEqualTo(120.5);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-SVC-02: normalizeDuration возвращает 60 для отрицательного значения")
    void normalizeDuration_returns60ForNegative() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, -10.0);
        assertThat(result).isEqualTo(60.0);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-SVC-03: normalizeDuration возвращает 60 для нуля")
    void normalizeDuration_returns60ForZero() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, 0.0);
        assertThat(result).isEqualTo(60.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-04: normalizeTargets добавляет стандартные высоты")
    void normalizeTargets_addsStandardHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(720), 1080);

        assertThat(result).contains(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-05: normalizeTargets возвращает отсортированный список")
    void normalizeTargets_returnsSortedList() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(1080, 360, 720), 1080);

        assertThat(result).isSorted();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-06: normalizeTargets удаляет дубликаты")
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
    @DisplayName("BE-UNIT-TRANS-SVC-07: parseFfmpegTimeToSeconds парсит HH:MM:SS")
    void parseFfmpegTimeToSeconds_parsesTime() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "01:30:45.500");
        assertThat(result).isCloseTo(5445.5, within(0.1));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-08: parseFfmpegTimeToSeconds парсит нулевое время")
    void parseFfmpegTimeToSeconds_parsesZeroTime() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:00.000");
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-SVC-09: parseFfmpegTimeToSeconds возвращает 0 для невалидного формата")
    void parseFfmpegTimeToSeconds_returns0ForInvalidFormat() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "invalid");
        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-11: normalizeTargets с пустым списком возвращает стандартные высоты")
    void normalizeTargets_withEmptyListReturnsDefaults() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 1080);

        assertThat(result).containsExactly(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-12: normalizeTargets с null списком возвращает стандартные высоты")
    void normalizeTargets_withNullListReturnsDefaults() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, null, 1080);

        assertThat(result).containsExactly(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-13: parseFfmpegTimeToSeconds корректно парсит часы")
    void parseFfmpegTimeToSeconds_correctlyParsesHours() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "02:00:00.000");
        assertThat(result).isEqualTo(7200.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-14: parseFfmpegTimeToSeconds корректно парсит минуты")
    void parseFfmpegTimeToSeconds_correctlyParsesMinutes() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:45:00.000");
        assertThat(result).isEqualTo(2700.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-15: parseFfmpegTimeToSeconds корректно парсит секунды")
    void parseFfmpegTimeToSeconds_correctlyParsesSeconds() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:30.500");
        assertThat(result).isEqualTo(30.5);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-16: normalizeTargets фильтрует отрицательные высоты")
    void normalizeTargets_filtersNegativeHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(-100, 720, 0), 1080);

        assertThat(result).allMatch(h -> h > 0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-17: normalizeDuration с большим значением")
    void normalizeDuration_withLargeValue() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, 10000.0);
        assertThat(result).isEqualTo(10000.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-18: handleCommand скачивает видео из storage")
    void handleCommand_downloadsVideoFromStorage() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "storage-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        InputStream mockStream = new ByteArrayInputStream(new byte[1000]);
        when(videoObjectStorage.downloadProcessed("storage-key")).thenReturn(mockStream);

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {
        }

        verify(videoObjectStorage).downloadProcessed("storage-key");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-19: parseFfmpegTimeToSeconds с микросекундами")
    void parseFfmpegTimeToSeconds_withMicroseconds() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:10.123456");
        assertThat(result).isCloseTo(10.123456, within(0.001));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-SVC-20: normalizeTargets сохраняет кастомные высоты")
    void normalizeTargets_preservesCustomHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(540, 900), 1080);

        assertThat(result).contains(540, 900);
    }
}
