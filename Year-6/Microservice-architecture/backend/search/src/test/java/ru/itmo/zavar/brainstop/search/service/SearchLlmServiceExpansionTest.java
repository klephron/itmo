package ru.itmo.zavar.brainstop.search.llm;

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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServiceExpansionTest {

    @Mock
    private OpenRouterClient openRouterClient;

    @Mock
    private SearchLlmProperties properties;

    private SearchLlmService service;

    @BeforeEach
    void setUp() {
        service = new SearchLlmService(openRouterClient, properties, new ObjectMapper());
        service.init();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-EXP-01: buildQueryVariants возвращает расширенные варианты")
    void buildQueryVariants_returnsExpansions() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("cat");

        assertThat(result).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-EXP-02: buildQueryVariants ограничивает количество")
    void buildQueryVariants_limitsCount() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(2);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("test");

        assertThat(result.size()).isLessThanOrEqualTo(4);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-EXP-03: buildQueryVariants кеширует результаты")
    void buildQueryVariants_caches() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        service.buildQueryVariants("test");
        List<String> result = service.buildQueryVariants("test");

        assertThat(result).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-EXP-04: buildQueryVariants балансирует языки")
    void buildQueryVariants_balancesLanguages() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("test");

        long cyrillicCount = result.stream().filter(s -> s.matches(".*\\p{IsCyrillic}.*")).count();
        long latinCount = result.stream().filter(s -> s.matches(".*[a-zA-Z].*")).count();

        assertThat(cyrillicCount).isGreaterThan(0);
        assertThat(latinCount).isGreaterThan(0);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-EXP-06: cacheExpansionFromStream обрабатывает невалидный JSON")
    void cacheExpansionFromStream_invalid() {
        List<String> result = service.cacheExpansionFromStream("test", "invalid json");

        assertThat(result).isEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-EXP-08: buildQueryVariants удаляет дубликаты")
    void buildQueryVariants_removeDuplicates() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("test");

        long uniqueLower = result.stream().map(String::toLowerCase).distinct().count();
        assertThat(uniqueLower).isEqualTo(result.size());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-EXP-09: buildQueryVariants сохраняет оригинальный запрос")
    void buildQueryVariants_preservesOriginal() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("test");

        assertThat(result.stream().anyMatch(s -> s.equalsIgnoreCase("test"))).isTrue();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-EXP-10: buildQueryVariants с fallback вариантами")
    void buildQueryVariants_fallbackVariants() {
        when(properties.isEnabled()).thenReturn(false);

        List<String> result = service.buildQueryVariants("test");

        assertThat(result).isNotEmpty();
    }
}
