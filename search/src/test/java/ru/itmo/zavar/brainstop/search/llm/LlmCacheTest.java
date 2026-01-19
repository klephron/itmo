package ru.itmo.zavar.brainstop.search.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@FR({"FR-5.1"})
@Layer(TestLayer.BE_UNIT)
class LlmCacheTest {

    private LlmCache<String> cache;

    @BeforeEach
    void setUp() {
        cache = new LlmCache<>(Duration.ofMinutes(5));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-01: Put и get возвращают значение")
    void putAndGet_returnsValue() {
        cache.put("key", "value");

        Optional<String> result = cache.get("key");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("value");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-02: Get несуществующего ключа возвращает empty")
    void getNonExistent_returnsEmpty() {
        Optional<String> result = cache.get("nonexistent");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CACHE-03: Null ключ возвращает empty")
    void getNullKey_returnsEmpty() {
        Optional<String> result = cache.get(null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CACHE-04: Пустой ключ возвращает empty")
    void getBlankKey_returnsEmpty() {
        Optional<String> result = cache.get("");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.NEGATIVE)
    @DisplayName("BE-UNIT-CACHE-05: Put с null ключом игнорируется")
    void putNullKey_ignored() {
        cache.put(null, "value");

        Optional<String> result = cache.get(null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-06: Перезапись значения")
    void put_overwritesValue() {
        cache.put("key", "value1");
        cache.put("key", "value2");

        Optional<String> result = cache.get("key");

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("value2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-07: Истечение TTL")
    void get_expiredEntry_returnsEmpty() throws InterruptedException {
        LlmCache<String> shortCache = new LlmCache<>(Duration.ofMillis(10));
        shortCache.put("key", "value");

        Thread.sleep(50);

        Optional<String> result = shortCache.get("key");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-08: Нулевой TTL отключает кеш")
    void zeroTtl_disablesCache() {
        LlmCache<String> disabledCache = new LlmCache<>(Duration.ZERO);
        disabledCache.put("key", "value");

        Optional<String> result = disabledCache.get("key");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-09: Отрицательный TTL отключает кеш")
    void negativeTtl_disablesCache() {
        LlmCache<String> disabledCache = new LlmCache<>(Duration.ofSeconds(-1));
        disabledCache.put("key", "value");

        Optional<String> result = disabledCache.get("key");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-10: Null TTL отключает кеш")
    void nullTtl_disablesCache() {
        LlmCache<String> disabledCache = new LlmCache<>(null);
        disabledCache.put("key", "value");

        Optional<String> result = disabledCache.get("key");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-CACHE-11: Множественные ключи")
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
    @DisplayName("BE-UNIT-CACHE-12: Кеширование сложных объектов")
    void cacheComplexObjects() {
        record TestObject(String name, int value) {}
        LlmCache<TestObject> objectCache = new LlmCache<>(Duration.ofMinutes(5));

        TestObject obj = new TestObject("test", 42);
        objectCache.put("key", obj);

        Optional<TestObject> result = objectCache.get("key");

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("test");
        assertThat(result.get().value()).isEqualTo(42);
    }
}
