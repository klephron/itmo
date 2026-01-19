package ru.itmo.zavar.brainstop.video.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class LlmCacheVideoTest {

    private LlmCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = new LlmCache<>(Duration.ofMinutes(5));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-01: put и get работают")
    void putAndGet_work() {
        cache.put("key", "value");

        Optional<String> result = cache.get("key");

        assertThat(result).hasValue("value");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-02: get несуществующего ключа")
    void get_nonExistent() {
        Optional<String> result = cache.get("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CACHE-VID-03: get с null ключом")
    void get_nullKey() {
        Optional<String> result = cache.get(null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CACHE-VID-04: get с пустым ключом")
    void get_blankKey() {
        Optional<String> result = cache.get("   ");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CACHE-VID-05: put с null ключом игнорируется")
    void put_nullKey() {
        cache.put(null, "value");

        assertThat(cache.get(null)).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-06: put перезаписывает значение")
    void put_overwrites() {
        cache.put("key", "value1");
        cache.put("key", "value2");

        Optional<String> result = cache.get("key");

        assertThat(result).hasValue("value2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-07: истечение TTL")
    void get_expired() throws InterruptedException {
        LlmCache<String> shortCache = new LlmCache<>(Duration.ofMillis(10));
        shortCache.put("key", "value");

        Thread.sleep(50);

        Optional<String> result = shortCache.get("key");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-08: нулевой TTL отключает кеш")
    void zeroTtl_disablesCache() {
        LlmCache<String> disabled = new LlmCache<>(Duration.ZERO);
        disabled.put("key", "value");

        Optional<String> result = disabled.get("key");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-09: отрицательный TTL отключает кеш")
    void negativeTtl_disablesCache() {
        LlmCache<String> disabled = new LlmCache<>(Duration.ofSeconds(-1));
        disabled.put("key", "value");

        Optional<String> result = disabled.get("key");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-10: null TTL отключает кеш")
    void nullTtl_disablesCache() {
        LlmCache<String> disabled = new LlmCache<>(null);
        disabled.put("key", "value");

        Optional<String> result = disabled.get("key");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-11: множественные ключи")
    void multipleKeys() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        assertThat(cache.get("key1")).hasValue("value1");
        assertThat(cache.get("key2")).hasValue("value2");
        assertThat(cache.get("key3")).hasValue("value3");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-VID-12: кеширование списков")
    void cacheLists() {
        LlmCache<List<String>> listCache = new LlmCache<>(Duration.ofMinutes(5));
        List<String> list = List.of("a", "b", "c");

        listCache.put("key", list);

        Optional<List<String>> result = listCache.get("key");

        assertThat(result).hasValue(list);
    }
}
