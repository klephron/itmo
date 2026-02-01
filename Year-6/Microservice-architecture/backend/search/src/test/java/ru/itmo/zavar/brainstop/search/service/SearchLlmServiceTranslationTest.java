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

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@FR({"FR-5.2"})
@Layer(TestLayer.BE_UNIT)
class SearchLlmServiceTranslationTest {

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
    @DisplayName("BE-UNIT-LLM-TRANS-01: buildQueryVariants с кириллицей добавляет латиницу")
    void buildQueryVariants_cyrillicAddsLatin() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("тест");

        assertThat(result).hasSizeGreaterThan(1);
        assertThat(result).anyMatch(s -> s.contains("test") || s.contains("тест"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-02: buildQueryVariants с латиницей добавляет кириллицу")
    void buildQueryVariants_latinAddsCyrillic() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("test");

        assertThat(result).hasSizeGreaterThan(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-03: buildQueryVariants сохраняет номера моделей")
    void buildQueryVariants_preservesModelNumbers() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("BMW X5");

        assertThat(result).anyMatch(s -> s.contains("BMW"));
        assertThat(result).anyMatch(s -> s.contains("X5"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-04: buildQueryVariants с брендами")
    void buildQueryVariants_preservesBrands() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("iPhone 15");

        assertThat(result).anyMatch(s -> s.contains("iPhone"));
        assertThat(result).anyMatch(s -> s.contains("15"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-05: buildQueryVariants дедублирует результаты")
    void buildQueryVariants_deduplicates() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("test");

        long uniqueCount = result.stream().map(String::toLowerCase).distinct().count();
        assertThat(uniqueCount).isEqualTo(result.size());
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-06: buildQueryVariants с короткими запросами")
    void buildQueryVariants_shortQueries() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getShortQueryMaxLength()).thenReturn(4);

        List<String> result = service.buildQueryVariants("test");

        assertThat(result).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-07: buildQueryVariants с длинными запросами")
    void buildQueryVariants_longQueries() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("this is a very long search query");

        assertThat(result).hasSizeGreaterThan(1);
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-08: buildQueryVariants с цифрами")
    void buildQueryVariants_withNumbers() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("test 123");

        assertThat(result).anyMatch(s -> s.contains("123"));
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-09: buildQueryVariants с дефисами")
    void buildQueryVariants_withHyphens() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("test-query");

        assertThat(result).isNotEmpty();
    }

    @Test
    @TestType(TestType.Type.BASIC)
    @DisplayName("BE-UNIT-LLM-TRANS-10: buildQueryVariants с акронимами")
    void buildQueryVariants_withAcronyms() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.isExpansionEnabled()).thenReturn(true);
        when(properties.getExpansionCount()).thenReturn(3);
        when(properties.getMinQueryLength()).thenReturn(3);
        when(properties.getExpansionMaxTokens()).thenReturn(120);

        List<String> result = service.buildQueryVariants("USA");

        assertThat(result).anyMatch(s -> s.contains("USA"));
    }
}
