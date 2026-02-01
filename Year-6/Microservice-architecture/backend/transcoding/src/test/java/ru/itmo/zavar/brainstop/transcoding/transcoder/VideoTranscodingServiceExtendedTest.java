package ru.itmo.zavar.brainstop.transcoding.transcoder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@FR({"FR-3.1", "FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class VideoTranscodingServiceExtendedTest {

    @Mock
    private TranscodingJobRegistry jobRegistry;

    @Mock
    private TranscodingEventsProducer eventsProducer;

    @Mock
    private VideoObjectStorage videoObjectStorage;

    private VideoTranscodingService service;

    @BeforeEach
    void setUp() {
        service = new VideoTranscodingService(jobRegistry, eventsProducer, videoObjectStorage);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -100.0, -0.5})
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-01: normalizeDuration для нулевых и отрицательных значений")
    void normalizeDuration_forZeroAndNegativeValues(double input) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, input);
        assertThat(result).isEqualTo(60.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.0, 30.5, 100.0, 3600.0, 7200.5})
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-02: normalizeDuration для положительных значений")
    void normalizeDuration_forPositiveValues(double input) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, input);
        assertThat(result).isEqualTo(input);
    }

    @ParameterizedTest
    @CsvSource({
            "00:00:01.000, 1.0",
            "00:01:00.000, 60.0",
            "01:00:00.000, 3600.0",
            "00:00:30.500, 30.5",
            "02:30:15.250, 9015.25"
    })
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-03: parseFfmpegTimeToSeconds для различных времен")
    void parseFfmpegTimeToSeconds_forVariousTimes(String timeStr, double expectedSeconds) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, timeStr);
        assertThat(result).isCloseTo(expectedSeconds, within(0.01));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "", "abc:def:ghi", "1:2", ":::"})
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-TRANS-EXT-04: parseFfmpegTimeToSeconds для некорректных форматов")
    void parseFfmpegTimeToSeconds_forInvalidFormats(String invalidTime) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, invalidTime);
        assertThat(result).isEqualTo(0.0);
    }

    @ParameterizedTest
    @MethodSource("provideHeightsAndExpectedResults")
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-05: normalizeTargets для различных комбинаций")
    void normalizeTargets_forVariousCombinations(List<Integer> input, int sourceHeight, List<Integer> expected) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, input, sourceHeight);

        assertThat(result).containsExactlyElementsOf(expected);
    }

    private static Stream<Arguments> provideHeightsAndExpectedResults() {
        return Stream.of(
                Arguments.of(List.of(), 1080, List.of(144, 240, 360, 480, 720, 1080)),
                Arguments.of(List.of(540), 1080, List.of(144, 240, 360, 480, 540, 720, 1080)),
                Arguments.of(List.of(360, 720), 1080, List.of(144, 240, 360, 480, 720, 1080)),
                Arguments.of(List.of(2160), 1080, List.of(144, 240, 360, 480, 720, 1080, 2160))
        );
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-06: handleCommand вызывает jobRegistry.put")
    void handleCommand_callsJobRegistryPut() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "test-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed(anyString()))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {}

        verify(jobRegistry, atLeastOnce()).put(eq(jobId), any(TranscodingJobStatus.class));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-09: normalizeTargets фильтрует нулевые высоты")
    void normalizeTargets_filtersZeroHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(0, 720, 0), 1080);

        assertThat(result).allMatch(h -> h > 0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-10: normalizeTargets сохраняет порядок сортировки")
    void normalizeTargets_maintainsSortOrder() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(1080, 360, 720, 240), 1080);

        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i)).isLessThan(result.get(i + 1));
        }
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-11: handleCommand создает временный файл")
    void handleCommand_createsTempFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        TranscodingCommand command = new TranscodingCommand(
                jobId, "test-key", "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        byte[] testData = new byte[1000];
        when(videoObjectStorage.downloadProcessed("test-key"))
                .thenReturn(new ByteArrayInputStream(testData));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {}

        verify(videoObjectStorage).downloadProcessed("test-key");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-12: parseFfmpegTimeToSeconds обрабатывает микросекунды")
    void parseFfmpegTimeToSeconds_handlesMicroseconds() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:05.123456");
        assertThat(result).isGreaterThan(5.0);
        assertThat(result).isLessThan(6.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-13: normalizeDuration с граничными значениями")
    void normalizeDuration_withBoundaryValues() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        assertThat((double) method.invoke(service, 0.001)).isEqualTo(0.001);
        assertThat((double) method.invoke(service, Double.MAX_VALUE)).isEqualTo(Double.MAX_VALUE);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-14: normalizeTargets включает все стандартные высоты")
    void normalizeTargets_includesAllStandardHeights() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 2160);

        assertThat(result).contains(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-15: normalizeTargets с высотами выше 1080p")
    void normalizeTargets_withHeightsAbove1080p() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(1440, 2160, 4320), 2160);

        assertThat(result).contains(1440, 2160, 4320);
    }

    @ParameterizedTest
    @CsvSource({
            "144, true",
            "240, true",
            "360, true",
            "480, true",
            "720, true",
            "1080, true"
    })
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-16: normalizeTargets всегда включает стандартные разрешения")
    void normalizeTargets_alwaysIncludesStandardResolutions(int height, boolean shouldBeIncluded) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(), 2160);

        assertThat(result.contains(height)).isEqualTo(shouldBeIncluded);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-17: handleCommand скачивает из правильного ключа")
    void handleCommand_downloadsFromCorrectKey() throws Exception {
        String storageKey = "specific-storage-key";
        TranscodingCommand command = new TranscodingCommand(
                UUID.randomUUID(), storageKey, "video.mp4", "tmp", 1L, "tmp", List.of(720)
        );

        when(videoObjectStorage.downloadProcessed(storageKey))
                .thenReturn(new ByteArrayInputStream(new byte[100]));

        try {
            service.handleCommand(command);
        } catch (Exception ignored) {}

        verify(videoObjectStorage).downloadProcessed(storageKey);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-18: parseFfmpegTimeToSeconds с нулевыми компонентами")
    void parseFfmpegTimeToSeconds_withZeroComponents() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        assertThat((double) method.invoke(service, "00:00:00.000")).isEqualTo(0.0);
        assertThat((double) method.invoke(service, "01:00:00.000")).isEqualTo(3600.0);
        assertThat((double) method.invoke(service, "00:01:00.000")).isEqualTo(60.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-19: normalizeTargets удаляет дубликаты из входного списка")
    void normalizeTargets_removesDuplicatesFromInput() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(720, 720, 720, 1080, 1080), 1080);

        long count720 = result.stream().filter(h -> h == 720).count();
        long count1080 = result.stream().filter(h -> h == 1080).count();

        assertThat(count720).isEqualTo(1);
        assertThat(count1080).isEqualTo(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-TRANS-EXT-20: normalizeTargets работает с очень большим списком")
    void normalizeTargets_worksWithVeryLargeList() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        List<Integer> largeList = Stream.iterate(100, i -> i + 10)
                .limit(200)
                .toList();

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, largeList, 2160);

        assertThat(result).hasSizeGreaterThan(6);
        assertThat(result).isSorted();
    }
}
