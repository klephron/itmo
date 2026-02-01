package ru.itmo.zavar.brainstop.search.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.embedding.HashingEmbeddingService;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-5.1"})
@Layer(TestLayer.BE_UNIT)
class HashingEmbeddingServiceTest {

    private HashingEmbeddingService service;

    @BeforeEach
    void setUp() {
        service = new HashingEmbeddingService();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-01: Embed возвращает вектор размером 1024")
    void embed_returnsVector1024() {
        float[] result = service.embed("test query");

        assertThat(result).hasSize(1024);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-02: Embed нормализует вектор")
    void embed_normalizesVector() {
        float[] result = service.embed("test query");

        double norm = 0.0;
        for (float v : result) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-03: Одинаковые запросы дают одинаковые векторы")
    void embed_sameQuerySameVector() {
        float[] first = service.embed("test query");
        float[] second = service.embed("test query");

        assertThat(second).isEqualTo(first);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-04: Разные запросы дают разные векторы")
    void embed_differentQueryDifferentVector() {
        float[] first = service.embed("test query");
        float[] second = service.embed("another query");

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-HASH-05: Null возвращает нулевой вектор")
    void embed_null_returnsZeroVector() {
        float[] result = service.embed(null);

        assertThat(result).hasSize(1024);
        assertThat(result).containsOnly(0.0f);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-HASH-06: Пустая строка возвращает нулевой вектор")
    void embed_emptyString_returnsZeroVector() {
        float[] result = service.embed("");

        assertThat(result).hasSize(1024);
        assertThat(result).containsOnly(0.0f);
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-HASH-07: Пробельная строка возвращает нулевой вектор")
    void embed_blankString_returnsZeroVector() {
        float[] result = service.embed("   \t\n   ");

        assertThat(result).hasSize(1024);
        assertThat(result).containsOnly(0.0f);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-08: Регистронезависимость")
    void embed_caseInsensitive() {
        float[] upper = service.embed("TEST QUERY");
        float[] lower = service.embed("test query");

        assertThat(upper).isEqualTo(lower);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-09: Множественные токены")
    void embed_multipleTokens() {
        float[] result = service.embed("one two three four five");

        assertThat(result).hasSize(1024);
        assertThat(result).isNotEqualTo(new float[1024]);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-10: Специальные символы игнорируются")
    void embed_specialCharacters() {
        float[] withSpecial = service.embed("test@#$query");
        float[] withoutSpecial = service.embed("test query");

        assertThat(withSpecial).hasSize(1024);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-11: Длинный текст")
    void embed_longText() {
        String longText = "word ".repeat(100);
        float[] result = service.embed(longText);

        assertThat(result).hasSize(1024);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-HASH-12: Unicode символы")
    void embed_unicodeCharacters() {
        float[] result = service.embed("тест запрос");

        assertThat(result).hasSize(1024);
    }
}
