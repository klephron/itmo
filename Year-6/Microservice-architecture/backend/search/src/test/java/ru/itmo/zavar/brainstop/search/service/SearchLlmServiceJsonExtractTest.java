package ru.itmo.zavar.brainstop.search.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.zavar.brainstop.common.test.FR;
import ru.itmo.zavar.brainstop.common.test.Layer;
import ru.itmo.zavar.brainstop.common.test.TestLayer;
import ru.itmo.zavar.brainstop.common.test.TestType;
import ru.itmo.zavar.brainstop.search.llm.OpenRouterClient;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmProperties;
import ru.itmo.zavar.brainstop.search.llm.SearchLlmService;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServiceJsonExtractTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private SearchLlmProperties properties;

    private SearchLlmService service;
    private Method extractStringsFromJsonMethod;
    private Method extractStringsFromContainerMethod;
    private Method readJsonValueMethod;
    private Method splitFallbackMethod;
    private Method extractUuidsMethod;
    private Method extractUuidMethod;

    @BeforeEach
    void setUp() throws Exception {
        when(properties.getCacheTtl()).thenReturn(Duration.ofMinutes(5));
        service = new SearchLlmService(openRouterClient, properties, new ObjectMapper());
        service.init();

        extractStringsFromJsonMethod = SearchLlmService.class.getDeclaredMethod("extractStringsFromJson", Object.class);
        extractStringsFromJsonMethod.setAccessible(true);

        extractStringsFromContainerMethod = SearchLlmService.class.getDeclaredMethod("extractStringsFromContainer", Map.class);
        extractStringsFromContainerMethod.setAccessible(true);

        readJsonValueMethod = SearchLlmService.class.getDeclaredMethod("readJsonValue", String.class);
        readJsonValueMethod.setAccessible(true);

        splitFallbackMethod = SearchLlmService.class.getDeclaredMethod("splitFallback", String.class);
        splitFallbackMethod.setAccessible(true);

        extractUuidsMethod = SearchLlmService.class.getDeclaredMethod("extractUuids", List.class, String.class);
        extractUuidsMethod.setAccessible(true);

        extractUuidMethod = SearchLlmService.class.getDeclaredMethod("extractUuid", String.class);
        extractUuidMethod.setAccessible(true);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-01: extractStringsFromJson из строки")
    void extractStringsFromJson_string() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromJsonMethod.invoke(service, "test");

        assertThat(result).containsExactly("test");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-02: extractStringsFromJson из списка")
    void extractStringsFromJson_list() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromJsonMethod.invoke(service, List.of("a", "b", "c"));

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-03: extractStringsFromJson из map")
    void extractStringsFromJson_map() throws Exception {
        Map<String, Object> map = Map.of("queries", List.of("a", "b"));

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromJsonMethod.invoke(service, map);

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-04: extractStringsFromJson null")
    void extractStringsFromJson_null() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromJsonMethod.invoke(service, (Object) null);

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-05: extractStringsFromContainer queries")
    void extractStringsFromContainer_queries() throws Exception {
        Map<String, Object> map = Map.of("queries", List.of("q1", "q2"));

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromContainerMethod.invoke(service, map);

        assertThat(result).containsExactly("q1", "q2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-06: extractStringsFromContainer query")
    void extractStringsFromContainer_query() throws Exception {
        Map<String, Object> map = Map.of("query", "test");

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromContainerMethod.invoke(service, map);

        assertThat(result).containsExactly("test");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-07: readJsonValue валидный JSON")
    void readJsonValue_valid() throws Exception {
        Object result = readJsonValueMethod.invoke(service, "{\"key\": \"value\"}");

        assertThat(result).isNotNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-08: readJsonValue невалидный JSON")
    void readJsonValue_invalid() throws Exception {
        Object result = readJsonValueMethod.invoke(service, "not json");

        assertThat(result).isNull();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-09: splitFallback по запятым")
    void splitFallback_commas() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) splitFallbackMethod.invoke(service, "a, b, c");

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-10: splitFallback по точкам с запятой")
    void splitFallback_semicolons() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) splitFallbackMethod.invoke(service, "a; b; c");

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-11: splitFallback по переводам строк")
    void splitFallback_newlines() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) splitFallbackMethod.invoke(service, "a\nb\nc");

        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-12: extractUuids из списка")
    void extractUuids_fromList() throws Exception {
        String uuid1 = "123e4567-e89b-12d3-a456-426614174000";
        String uuid2 = "123e4567-e89b-12d3-a456-426614174001";

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractUuidsMethod.invoke(service, List.of(uuid1, uuid2), "");

        assertThat(result).hasSize(2);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-13: extractUuids из сырого текста")
    void extractUuids_fromRaw() throws Exception {
        String raw = "Some text with uuid: 123e4567-e89b-12d3-a456-426614174000 here";

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractUuidsMethod.invoke(service, List.of(), raw);

        assertThat(result).hasSize(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-14: extractUuid находит UUID")
    void extractUuid_finds() throws Exception {
        String result = (String) extractUuidMethod.invoke(service, "uuid=123e4567-e89b-12d3-a456-426614174000");

        assertThat(result).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-15: extractUuid пустая для невалидного")
    void extractUuid_invalid() throws Exception {
        String result = (String) extractUuidMethod.invoke(service, "not a uuid");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-16: extractStringsFromContainer synonyms")
    void extractStringsFromContainer_synonyms() throws Exception {
        Map<String, Object> map = Map.of("synonyms", List.of("s1", "s2"));

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromContainerMethod.invoke(service, map);

        assertThat(result).containsExactly("s1", "s2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-17: extractStringsFromContainer variants")
    void extractStringsFromContainer_variants() throws Exception {
        Map<String, Object> map = Map.of("variants", List.of("v1", "v2"));

        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromContainerMethod.invoke(service, map);

        assertThat(result).containsExactly("v1", "v2");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-18: splitFallback удаляет кавычки")
    void splitFallback_stripQuotes() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) splitFallbackMethod.invoke(service, "\"a\", \"b\"");

        assertThat(result).containsExactly("a", "b");
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-19: readJsonValue массив")
    void readJsonValue_array() throws Exception {
        Object result = readJsonValueMethod.invoke(service, "[\"a\", \"b\"]");

        assertThat(result).isInstanceOf(List.class);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-JSON-EXT-20: extractStringsFromJson вложенный список")
    void extractStringsFromJson_nestedList() throws Exception {
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) extractStringsFromJsonMethod.invoke(
                service, List.of(List.of("a", "b"), "c"));

        assertThat(result).contains("a", "b", "c");
    }
}
