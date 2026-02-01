package ru.itmo.zavar.brainstop.video.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.video.streaming.model.StreamRange;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-1.3"})
@Layer(TestLayer.BE_UNIT)
class StreamRangeTest {

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-01: parseHttpRangeString парсит полный range")
    void parseHttpRangeString_parsesFullRange() {
        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-999", 1000);

        assertThat(range.getRangeStart()).isEqualTo(0);
        assertThat(range.getRangeEnd()).isEqualTo(999L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-02: parseHttpRangeString парсит open-ended range")
    void parseHttpRangeString_parsesOpenEndedRange() {
        StreamRange range = StreamRange.parseHttpRangeString("bytes=500-", 1000);

        assertThat(range.getRangeStart()).isEqualTo(500);
        assertThat(range.getRangeEnd()).isNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-03: parseHttpRangeString возвращает дефолт для null")
    void parseHttpRangeString_returnsDefaultForNull() {
        StreamRange range = StreamRange.parseHttpRangeString(null, 1000);

        assertThat(range.getRangeStart()).isEqualTo(0);
        assertThat(range.getRangeEnd()).isNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-04: getRangeEnd вычисляет end для open range")
    void getRangeEnd_calculatesForOpenRange() {
        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-", 1000);

        long end = range.getRangeEnd(10000);

        assertThat(end).isEqualTo(999);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-05: getRangeEnd ограничивает по размеру файла")
    void getRangeEnd_limitsToFileSize() {
        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-5000", 1000);

        long end = range.getRangeEnd(1000);

        assertThat(end).isEqualTo(999);
    }

    @ParameterizedTest
    @CsvSource({
            "bytes=0-999, 10000, 999",
            "bytes=500-1499, 10000, 1499",
            "bytes=9000-, 10000, 9999",
            "bytes=0-, 1000, 999"
    })
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-06: getRangeEnd для различных ranges")
    void getRangeEnd_variousRanges(String rangeHeader, long fileSize, long expectedEnd) {
        StreamRange range = StreamRange.parseHttpRangeString(rangeHeader, 1000);

        long end = range.getRangeEnd(fileSize);

        assertThat(end).isEqualTo(expectedEnd);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-07: parseHttpRangeString с пробелами")
    void parseHttpRangeString_withWhitespace() {
        StreamRange range = StreamRange.parseHttpRangeString("bytes= 100 - 200 ", 1000);

        assertThat(range.getRangeStart()).isEqualTo(100);
        assertThat(range.getRangeEnd()).isEqualTo(200L);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-08: getRangeEnd не превышает fileSize-1")
    void getRangeEnd_doesNotExceedFileSizeMinusOne() {
        StreamRange range = StreamRange.parseHttpRangeString("bytes=0-10000", 1000);

        long end = range.getRangeEnd(5000);

        assertThat(end).isLessThan(5000);
        assertThat(end).isEqualTo(4999);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-09: getDefaultChunkSize возвращает размер по умолчанию")
    void getDefaultChunkSize_returnsDefault() {
        StreamRange range = StreamRange.parseHttpRangeString(null, 2048);

        assertThat(range.getDefaultChunkSize()).isEqualTo(2048);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-RANGE-10: parseHttpRangeString для невалидного header")
    void parseHttpRangeString_forInvalidHeader() {
        StreamRange range = StreamRange.parseHttpRangeString("invalid-range", 1000);

        assertThat(range.getRangeStart()).isEqualTo(0);
        assertThat(range.getRangeEnd()).isNull();
    }
}
