package ru.itmo.zavar.brainstop.transcoding.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import ru.itmo.zavar.brainstop.common.api.infra.VideoObjectStorage;
import ru.itmo.zavar.brainstop.common.api.transcoding.TranscodingJobRegistry;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.transcoding.messaging.TranscodingEventsProducer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@FR({"FR-3.2"})
@Layer(TestLayer.BE_UNIT)
class VideoTranscodingServiceHelperMethodsTest {

    @Mock
    private TranscodingJobRegistry jobRegistry;
    @Mock
    private TranscodingEventsProducer eventsProducer;
    @Mock
    private VideoObjectStorage videoObjectStorage;

    private VideoTranscodingService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new VideoTranscodingService(jobRegistry, eventsProducer, videoObjectStorage);
    }

    @ParameterizedTest
    @CsvSource({
            "00:00:01.000, 1.0",
            "00:01:00.000, 60.0",
            "01:00:00.000, 3600.0",
            "00:00:30.500, 30.5",
            "02:15:45.750, 8145.75",
            "10:00:00.000, 36000.0"
    })
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-01: parseFfmpegTimeToSeconds точные значения")
    void parseFfmpegTimeToSeconds_exactValues(String time, double expected) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, time);

        assertThat(result).isCloseTo(expected, within(0.01));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "invalid",
            "12:34",
            "abc:def:ghi",
            ":::"
    })
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-HELPER-02: parseFfmpegTimeToSeconds невалидные форматы")
    void parseFfmpegTimeToSeconds_invalidFormats(String invalidTime) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, invalidTime);

        assertThat(result).isEqualTo(0.0);
    }

    @ParameterizedTest
    @MethodSource("provideNormalizeTargetsData")
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-03: normalizeTargets различные комбинации")
    void normalizeTargets_variousCombinations(List<Integer> input, int sourceHeight, int expectedMin) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, input, sourceHeight);

        assertThat(result).hasSizeGreaterThanOrEqualTo(expectedMin);
        assertThat(result).isSorted();
    }

    static Stream<Object[]> provideNormalizeTargetsData() {
        return Stream.of(
                new Object[]{List.of(), 1080, 6},
                new Object[]{List.of(540), 1080, 7},
                new Object[]{List.of(360, 720), 1080, 6},
                new Object[]{List.of(2160), 1080, 7},
                new Object[]{null, 1080, 6}
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -1.0, -100.0, -0.001, -Double.MAX_VALUE})
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-04: normalizeDuration для нулевых и отрицательных")
    void normalizeDuration_zeroAndNegative(double input) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, input);

        assertThat(result).isEqualTo(60.0);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.001, 1.0, 30.5, 60.0, 3600.0, 7200.5, Double.MAX_VALUE})
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-05: normalizeDuration для положительных значений")
    void normalizeDuration_positiveValues(double input) throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, input);

        assertThat(result).isEqualTo(input);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-06: normalizeTargets удаляет дубликаты")
    void normalizeTargets_removesDuplicates() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service,
                List.of(720, 720, 720, 1080, 1080), 1080);

        long count720 = result.stream().filter(h -> h == 720).count();
        long count1080 = result.stream().filter(h -> h == 1080).count();

        assertThat(count720).isEqualTo(1);
        assertThat(count1080).isEqualTo(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-07: normalizeTargets всегда включает стандартные")
    void normalizeTargets_alwaysIncludesStandard() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(2160), 2160);

        assertThat(result).contains(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-08: normalizeTargets фильтрует нулевые и отрицательные")
    void normalizeTargets_filtersZeroAndNegative() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service,
                List.of(-100, 0, 720, -50), 1080);

        assertThat(result).allMatch(h -> h > 0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-09: parseFfmpegTimeToSeconds с большими значениями")
    void parseFfmpegTimeToSeconds_largeValues() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "23:59:59.999");

        assertThat(result).isCloseTo(86399.999, within(0.01));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-10: parseFfmpegTimeToSeconds с минимальными значениями")
    void parseFfmpegTimeToSeconds_minimalValues() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:00.001");

        assertThat(result).isCloseTo(0.001, within(0.0001));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-11: normalizeTargets сохраняет порядок")
    void normalizeTargets_preservesOrder() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service,
                List.of(1080, 360, 720, 240), 1080);

        for (int i = 0; i < result.size() - 1; i++) {
            assertThat(result.get(i)).isLessThan(result.get(i + 1));
        }
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-12: parseFfmpegTimeToSeconds с дробными секундами")
    void parseFfmpegTimeToSeconds_withFractionalSeconds() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("parseFfmpegTimeToSeconds", String.class);
        method.setAccessible(true);

        double result = (double) method.invoke(service, "00:00:05.123456");

        assertThat(result).isGreaterThan(5.0);
        assertThat(result).isLessThan(6.0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-13: normalizeTargets с негативным sourceHeight")
    void normalizeTargets_withNegativeSourceHeight() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(720), -1);

        assertThat(result).isNotEmpty();
        assertThat(result).contains(144, 240, 360, 480, 720, 1080);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-14: normalizeTargets с нулевым sourceHeight")
    void normalizeTargets_withZeroSourceHeight() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeTargets", List.class, int.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Integer> result = (List<Integer>) method.invoke(service, List.of(720), 0);

        assertThat(result).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HELPER-15: normalizeDuration граничные случаи")
    void normalizeDuration_boundaryCases() throws Exception {
        Method method = VideoTranscodingService.class.getDeclaredMethod("normalizeDuration", double.class);
        method.setAccessible(true);

        assertThat((double) method.invoke(service, 0.0)).isEqualTo(60.0);
        assertThat((double) method.invoke(service, 0.001)).isEqualTo(0.001);
        assertThat((double) method.invoke(service, -0.001)).isEqualTo(60.0);
    }
}
